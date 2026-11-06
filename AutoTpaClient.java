package pl.autotpa;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoTpaClient implements ClientModInitializer {
    public static boolean enabled = false;
    public static int cooldownSeconds = 60;
    private static long lastGlobalSend = 0;
    private static final Map<String, Long> lastSent = new HashMap<>();
    private static KeyMapping openKey;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.autotpa.open",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_K,
            KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openKey.consumeClick()) {
                if (mc.screen == null) {
                    mc.setScreen(new AutoTpaScreen());
                }
            }
        });

        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, timestamp) -> {
            Minecraft mc = Minecraft.getInstance();
            String name = sender != null ? sender.name() : findSender(mc, message.getString());
            trySend(mc, name);
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            Minecraft mc = Minecraft.getInstance();
            trySend(mc, findSender(mc, message.getString()));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("autotpa")
                .then(ClientCommandManager.literal("on").executes(ctx -> {
                    enabled = true;
                    ctx.getSource().sendFeedback(Component.literal("[AutoTPA] Włączony."));
                    return 1;
                }))
                .then(ClientCommandManager.literal("off").executes(ctx -> {
                    enabled = false;
                    ctx.getSource().sendFeedback(Component.literal("[AutoTPA] Wyłączony."));
                    return 1;
                }))
                .then(ClientCommandManager.literal("cooldown")
                    .then(ClientCommandManager.argument("sekundy", IntegerArgumentType.integer(10, 3600))
                        .executes(ctx -> {
                            cooldownSeconds = IntegerArgumentType.getInteger(ctx, "sekundy");
                            ctx.getSource().sendFeedback(Component.literal("[AutoTPA] Cooldown: " + cooldownSeconds + " s."));
                            return 1;
                        })))
                .then(ClientCommandManager.literal("status").executes(ctx -> {
                    ctx.getSource().sendFeedback(Component.literal("[AutoTPA] " + (enabled ? "Włączony" : "Wyłączony")
                        + ", cooldown: " + cooldownSeconds + " s."));
                    return 1;
                }))));
    }

    private static void trySend(Minecraft mc, String name) {
        if (!enabled || name == null || mc.player == null || mc.player.connection == null) return;
        if (name.equalsIgnoreCase(mc.getUser().getName())) return;

        long now = System.currentTimeMillis();
        if (now - lastGlobalSend < 3000) return;
        Long last = lastSent.get(name.toLowerCase());
        if (last != null && now - last < cooldownSeconds * 1000L) return;

        mc.player.connection.sendCommand("tpa " + name);
        lastGlobalSend = now;
        lastSent.put(name.toLowerCase(), now);
    }

    private static String findSender(Minecraft mc, String text) {
        if (mc.getConnection() == null) return null;
        String best = null;
        int bestIndex = Integer.MAX_VALUE;
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            String n = info.getProfile().name();
            Matcher m = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(n) + "(?![A-Za-z0-9_])").matcher(text);
            if (m.find() && m.start() < bestIndex) {
                bestIndex = m.start();
                best = n;
            }
        }
        return best;
    }
}
