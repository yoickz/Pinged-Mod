package com.savyous.pinged;

import net.minecraft.util.math.Vec3d;

public class Ping {
    private final String author;
    private final String targetPlayer;
    private Vec3d pos;
    private final boolean isPlayer;
    private int ticksRemaining;

    public Ping(String author, String targetPlayer, Vec3d pos, boolean isPlayer) {
        this.author = author;
        this.targetPlayer = targetPlayer;
        this.pos = pos;
        this.isPlayer = isPlayer;
        this.ticksRemaining = PingManager.getPingDurationSeconds() * 20;
    }

    public boolean tick() {
        return --this.ticksRemaining <= 0;
    }

    public String getAuthor() {
        return this.author;
    }

    public String getTargetPlayer() {
        return this.targetPlayer;
    }

    public Vec3d getPos() {
        return this.pos;
    }

    public void setPos(Vec3d pos) {
        this.pos = pos;
    }

    public boolean isPlayer() {
        return this.isPlayer;
    }

    public int getTicksRemaining() {
        return this.ticksRemaining;
    }
}
