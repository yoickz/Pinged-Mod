package com.savyous.pinged;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Locale;

public class PingRenderer {
    public static void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        Camera camera = client.gameRenderer.getCamera();
        if (!camera.isReady()) return;

        Vec3d camPos = camera.getCameraPos();
        Quaternionf camRot = new Quaternionf(camera.getRotation()).conjugate();
        double fov = client.options.getFov().getValue();
        double fovRad = Math.toRadians(fov);
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        float aspect = (float) width / (float) height;
        float halfTan = (float) Math.tan(fovRad / 2.0);

        for (Ping ping : PingManager.getActivePings()) {
            Vec3d targetPos = ping.getPos();
            if (ping.isPlayer() && ping.getTargetPlayer() != null) {
                for (AbstractClientPlayerEntity p : client.world.getPlayers()) {
                    if (p.getGameProfile().name().equalsIgnoreCase(ping.getTargetPlayer()) ||
                            p.getName().getString().equalsIgnoreCase(ping.getTargetPlayer())) {
                        targetPos = new Vec3d(p.getX(), p.getY() + p.getHeight() * 0.5, p.getZ());
                        ping.setPos(targetPos);
                        break;
                    }
                }
            }

            double dx = targetPos.x - camPos.x;
            double dy = targetPos.y - camPos.y;
            double dz = targetPos.z - camPos.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            Vector3f rel = new Vector3f((float) dx, (float) dy, (float) dz);
            camRot.transform(rel);

            float screenX;
            float screenY;
            float depth = -rel.z;

            if (depth > 0.05f) {
                float xClip = rel.x / (depth * halfTan * aspect);
                float yClip = rel.y / (depth * halfTan);
                screenX = (xClip + 1.0f) * 0.5f * width;
                screenY = (1.0f - yClip) * 0.5f * height;

                boolean offScreen = screenX < 24 || screenX > width - 24 || screenY < 24 || screenY > height - 24;
                if (offScreen) {
                    float dirX = screenX - width * 0.5f;
                    float dirY = screenY - height * 0.5f;
                    float len = (float) Math.hypot(dirX, dirY);
                    if (len > 0.001f) {
                        dirX /= len;
                        dirY /= len;
                    }
                    screenX = width * 0.5f + dirX * (width * 0.45f);
                    screenY = height * 0.5f + dirY * (height * 0.45f);
                }
            } else {
                float dirX = rel.x;
                float dirY = -rel.y;
                float len = (float) Math.hypot(dirX, dirY);
                if (len > 0.001f) {
                    dirX /= len;
                    dirY /= len;
                } else {
                    dirY = 1.0f;
                }
                screenX = width * 0.5f + dirX * (width * 0.45f);
                screenY = height * 0.5f + dirY * (height * 0.45f);
            }

            screenX = Math.max(24, Math.min(width - 24, screenX));
            screenY = Math.max(24, Math.min(height - 24, screenY));

            int x = (int) screenX;
            int y = (int) screenY;

            int headSize = 16;
            int headX = x - headSize / 2;
            int headY = y - 20;

            SkinTextures skin = PingManager.getSkin(ping.getAuthor());

            context.fill(headX - 1, headY - 1, headX + headSize + 1, headY + headSize + 1, 0xCC101010);
            PlayerSkinDrawer.draw(context, skin, headX, headY, headSize);

            int pinColor = ping.isPlayer() ? 0xFFFF3B30 : 0xFF0A84FF;
            context.fill(x - 2, y - 2, x + 2, y + 2, pinColor);
            context.fill(x - 1, y - 1, x + 1, y + 1, 0xFFFFFFFF);

            if (ping.isPlayer() && ping.getTargetPlayer() != null) {
                context.drawCenteredTextWithShadow(client.textRenderer, "§c" + ping.getTargetPlayer(), x, headY - 10, 0xFFFFFF);
            }

            String distText = distance < 1000
                    ? ((int) distance + "m")
                    : String.format(Locale.ROOT, "%.1fkm", distance / 1000.0);
            context.drawCenteredTextWithShadow(client.textRenderer, distText, x, y + 4, 0xFFFFFF);
        }
    }
}
