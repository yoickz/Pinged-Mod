package com.savyous.pinged;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

public class PingedClient implements ClientModInitializer {
    public static final String MOD_ID = "pinged";
    private static KeyBinding pingKey;
    private static KeyBinding clearKey;

    @Override
    public void onInitializeClient() {
        PingManager.loadConfig();

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "keybinds"));

        pingKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pinged.ping",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                category
        ));

        clearKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.pinged.clear",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (pingKey.wasPressed()) {
                PingManager.triggerPing(client);
            }
            while (clearKey.wasPressed()) {
                PingManager.clearPings();
            }
            PingManager.tick();
        });

        HudRenderCallback.EVENT.register(PingRenderer::render);

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            boolean handled = false;
            if (message != null) {
                handled = PingManager.handleIncomingMessage(message.getString());
            }
            if (!handled && signedMessage != null) {
                if (signedMessage.getContent() != null) {
                    handled = PingManager.handleIncomingMessage(signedMessage.getContent().getString());
                }
                if (!handled && signedMessage.getSignedContent() != null) {
                    handled = PingManager.handleIncomingMessage(signedMessage.getSignedContent());
                }
            }
            return !handled;
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (message == null) return true;
            return !PingManager.handleIncomingMessage(message.getString());
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("pinged")
                    .then(ClientCommandManager.literal("add")
                            .then(ClientCommandManager.argument("player", StringArgumentType.word())
                                    .suggests((context, builder) -> {
                                        MinecraftClient client = MinecraftClient.getInstance();
                                        if (client.getNetworkHandler() != null) {
                                            return CommandSource.suggestMatching(
                                                    client.getNetworkHandler().getPlayerList().stream().map(p -> p.getProfile().name()),
                                                    builder
                                            );
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        String player = StringArgumentType.getString(context, "player");
                                        PingManager.addFriend(player);
                                        context.getSource().sendFeedback(Text.literal("§aAdded §e" + player + "§a to your pings list."));
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("remove")
                            .then(ClientCommandManager.argument("player", StringArgumentType.word())
                                    .suggests((context, builder) -> CommandSource.suggestMatching(PingManager.getFriends(), builder))
                                    .executes(context -> {
                                        String player = StringArgumentType.getString(context, "player");
                                        PingManager.removeFriend(player);
                                        context.getSource().sendFeedback(Text.literal("§cRemoved §e" + player + "§c from your pings list."));
                                        return 1;
                                    })
                            )
                    )
                    .then(ClientCommandManager.literal("list")
                            .executes(context -> {
                                Set<String> list = PingManager.getFriends();
                                if (list.isEmpty()) {
                                    context.getSource().sendFeedback(Text.literal("§eYour pings list is empty."));
                                } else {
                                    context.getSource().sendFeedback(Text.literal("§aPings list: §e" + String.join(", ", list)));
                                }
                                return 1;
                            })
                    )
                    .then(ClientCommandManager.literal("clear")
                            .executes(context -> {
                                PingManager.clearPings();
                                context.getSource().sendFeedback(Text.literal("§aCleared all active pings."));
                                return 1;
                            })
                    )
                    .then(ClientCommandManager.literal("config")
                            .executes(context -> {
                                MinecraftClient client = MinecraftClient.getInstance();
                                client.send(() -> client.setScreen(new PingedConfigScreen(null)));
                                return 1;
                            })
                    )
            );
        });
    }
}
