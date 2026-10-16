package com.savyous.pinged;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentSkipListSet;

public class PingManager {
    private static final List<Ping> ACTIVE_PINGS = new CopyOnWriteArrayList<>();
    private static final Set<String> PING_LIST = new ConcurrentSkipListSet<>(String.CASE_INSENSITIVE_ORDER);
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("pinged.txt");
    private static int pingDurationSeconds = 15;

    public static int getPingDurationSeconds() {
        return pingDurationSeconds;
    }

    public static void setPingDurationSeconds(int seconds) {
        pingDurationSeconds = Math.max(1, seconds);
        saveConfig();
    }

    public static void loadConfig() {
        if (!Files.exists(CONFIG_PATH)) return;
        try {
            List<String> lines = Files.readAllLines(CONFIG_PATH);
            PING_LIST.clear();
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("duration:")) {
                    try {
                        pingDurationSeconds = Math.max(1, Integer.parseInt(trimmed.substring(9).trim()));
                    } catch (NumberFormatException ignored) {
                    }
                } else {
                    PING_LIST.add(trimmed);
                }
            }
        } catch (IOException ignored) {
        }
    }

    public static void saveConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            List<String> lines = new ArrayList<>();
            lines.add("duration:" + pingDurationSeconds);
            lines.addAll(PING_LIST);
            Files.write(CONFIG_PATH, lines);
        } catch (IOException ignored) {
        }
    }

    public static void addFriend(String name) {
        PING_LIST.add(name);
        saveConfig();
    }

    public static void removeFriend(String name) {
        PING_LIST.remove(name);
        saveConfig();
    }

    public static Set<String> getFriends() {
        return Collections.unmodifiableSet(PING_LIST);
    }

    public static List<Ping> getActivePings() {
        return ACTIVE_PINGS;
    }

    public static void clearPings() {
        ACTIVE_PINGS.clear();
    }

    public static void addPing(Ping ping) {
        ACTIVE_PINGS.removeIf(p -> p.getAuthor().equalsIgnoreCase(ping.getAuthor()));
        ACTIVE_PINGS.add(ping);
    }

    public static void tick() {
        ACTIVE_PINGS.removeIf(Ping::tick);
    }

    private static String resolveWhisperCommand(MinecraftClient client) {
        if (client.getNetworkHandler() != null && client.getNetworkHandler().getCommandDispatcher() != null) {
            CommandDispatcher<?> dispatcher = client.getNetworkHandler().getCommandDispatcher();
            if (dispatcher.getRoot().getChild("msg") != null) {
                return "msg";
            }
            if (dispatcher.getRoot().getChild("tell") != null) {
                return "tell";
            }
            if (dispatcher.getRoot().getChild("w") != null) {
                return "w";
            }
        }
        return "msg";
    }

    private static String resolveExactPlayerName(MinecraftClient client, String targetName) {
        if (client.getNetworkHandler() != null) {
            for (PlayerListEntry ple : client.getNetworkHandler().getPlayerList()) {
                if (ple.getProfile().name().equalsIgnoreCase(targetName)) {
                    return ple.getProfile().name();
                }
            }
        }
        return targetName;
    }

    public static void triggerPing(MinecraftClient client) {
        if (client.player == null || client.world == null) return;

        ClientPlayerEntity player = client.player;
        ClientWorld world = client.world;
        String myName = player.getGameProfile().name();
        Vec3d eyePos = player.getCameraPosVec(1.0f);
        Vec3d rotVec = player.getRotationVec(1.0f);
        double maxRange = 512.0;
        Vec3d endPos = eyePos.add(rotVec.multiply(maxRange));

        Vec3d boxMin = new Vec3d(
                Math.min(eyePos.x, endPos.x) - 2.0,
                Math.min(eyePos.y, endPos.y) - 2.0,
                Math.min(eyePos.z, endPos.z) - 2.0
        );
        Vec3d boxMax = new Vec3d(
                Math.max(eyePos.x, endPos.x) + 2.0,
                Math.max(eyePos.y, endPos.y) + 2.0,
                Math.max(eyePos.z, endPos.z) + 2.0
        );
        Box searchBox = new Box(boxMin, boxMax);

        EntityHitResult entityHit = ProjectileUtil.raycast(
                player,
                eyePos,
                endPos,
                searchBox,
                e -> e instanceof PlayerEntity && e != player && !e.isSpectator(),
                maxRange * maxRange
        );

        BlockHitResult blockHit = world.raycast(new RaycastContext(
                eyePos,
                endPos,
                RaycastContext.ShapeType.OUTLINE,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        double entityDistSq = (entityHit != null && entityHit.getEntity() instanceof PlayerEntity)
                ? eyePos.squaredDistanceTo(entityHit.getPos())
                : Double.MAX_VALUE;
        double blockDistSq = (blockHit != null && blockHit.getType() != HitResult.Type.MISS)
                ? eyePos.squaredDistanceTo(blockHit.getPos())
                : Double.MAX_VALUE;

        String whisperCmd = resolveWhisperCommand(client);

        if (entityDistSq < Double.MAX_VALUE && entityDistSq <= blockDistSq) {
            PlayerEntity targetPlayer = (PlayerEntity) entityHit.getEntity();
            String targetName = targetPlayer.getGameProfile().name();
            Vec3d targetPos = new Vec3d(targetPlayer.getX(), targetPlayer.getY() + targetPlayer.getHeight() * 0.5, targetPlayer.getZ());
            int tx = (int) Math.floor(targetPos.x);
            int ty = (int) Math.floor(targetPos.y);
            int tz = (int) Math.floor(targetPos.z);

            player.networkHandler.sendChatMessage("Pinged " + targetName + "!");
            player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 2.0f);

            addPing(new Ping(myName, targetName, targetPos, true));

            for (String friend : PING_LIST) {
                String exactFriend = resolveExactPlayerName(client, friend);
                String cmd = String.format(Locale.ROOT, "%s %s [Pinged] Targeted: %s at X: %d, Y: %d, Z: %d | %s %.2f %.2f %.2f true %s",
                        whisperCmd, exactFriend, targetName, tx, ty, tz, myName, targetPos.x, targetPos.y, targetPos.z, targetName);
                player.networkHandler.sendChatCommand(cmd);
            }
        } else if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
            Vec3d hitPos = blockHit.getPos().add(0, 0.5, 0);
            int px = (int) Math.floor(hitPos.x);
            int py = (int) Math.floor(hitPos.y);
            int pz = (int) Math.floor(hitPos.z);

            player.sendMessage(Text.literal("§a[Pinged] §fPinged location at §eX: " + px + ", Y: " + py + ", Z: " + pz), false);
            player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 2.0f);
            addPing(new Ping(myName, null, hitPos, false));

            for (String friend : PING_LIST) {
                String exactFriend = resolveExactPlayerName(client, friend);
                String cmd = String.format(Locale.ROOT, "%s %s [Pinged] Location: X: %d, Y: %d, Z: %d | %s %.2f %.2f %.2f false none",
                        whisperCmd, exactFriend, px, py, pz, myName, hitPos.x, hitPos.y, hitPos.z);
                player.networkHandler.sendChatCommand(cmd);
            }
        }
    }

    public static boolean handleIncomingMessage(String content) {
        if (content == null || !content.contains("[Pinged]")) return false;
        try {
            String clean = content.replaceAll("(?i)§[0-9a-z]", "").trim();
            int idx = clean.indexOf("[Pinged]");
            if (idx < 0) return false;

            String payload = clean.substring(idx + 8).trim();
            if (payload.contains("|")) {
                payload = payload.substring(payload.lastIndexOf('|') + 1).trim();
            } else if (payload.startsWith(":") || payload.startsWith("-")) {
                payload = payload.substring(1).trim();
            }

            String[] parts = payload.split("\\s+");
            if (parts.length < 5) return false;

            String author = parts[0].replaceAll("[^a-zA-Z0-9_]", "");
            double x = Double.parseDouble(parts[1].replaceAll("[^0-9.-]", ""));
            double y = Double.parseDouble(parts[2].replaceAll("[^0-9.-]", ""));
            double z = Double.parseDouble(parts[3].replaceAll("[^0-9.-]", ""));
            boolean isPlayer = Boolean.parseBoolean(parts[4].replaceAll("[^a-zA-Z]", ""));
            String targetName = parts.length >= 6 ? parts[5].replaceAll("[^a-zA-Z0-9_]", "") : null;
            if ("none".equalsIgnoreCase(targetName) || (targetName != null && targetName.isEmpty())) {
                targetName = null;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.player.getGameProfile().name().equalsIgnoreCase(author)) {
                return true;
            }

            addPing(new Ping(author, targetName, new Vec3d(x, y, z), isPlayer));

            if (client.player != null) {
                client.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0f, 2.0f);
                String notice = isPlayer
                        ? "§e" + author + "§r pinged player §c" + targetName + "§r!"
                        : "§e" + author + "§r pinged a location!";
                client.inGameHud.getChatHud().addMessage(Text.literal(notice));
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static SkinTextures getSkin(String playerName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.getGameProfile().name().equalsIgnoreCase(playerName)) {
            return client.player.getSkin();
        }
        if (client.getNetworkHandler() != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(playerName);
            if (entry != null) {
                return entry.getSkinTextures();
            }
            for (PlayerListEntry ple : client.getNetworkHandler().getPlayerList()) {
                if (ple.getProfile().name().equalsIgnoreCase(playerName)) {
                    return ple.getSkinTextures();
                }
            }
        }
        return DefaultSkinHelper.getSteve();
    }
}
