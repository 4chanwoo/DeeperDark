package com.chanwoo.deeperdark.common.entity;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.SculkSpreadable;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps the sculk a capacitor pours out inside a border built on the floor around it. Left alone, sculk veins climb
 * over walls and the sculk they turn the floor into tunnels underneath them, so a capacitor would spread sculk
 * well past any pen. When the floor around a capacitor is closed off by blocks that sculk can't take over, sculk
 * spreading inside that pen can't move out of it, over the border or under it.
 */
public final class SculkContainment {
    /** How far a border can be from the capacitor. Anything bigger counts as open ground. */
    private static final int RADIUS = 16;
    /** How often a discharging capacitor looks at its border again, in case it was changed. */
    private static final int RECHECK_TICKS = 20;
    /** How long a pen keeps holding sculk after the capacitor last poured charge into it. */
    private static final int LINGER_TICKS = 2400;
    /** How far below and above the floor the pen holds sculk: deep enough for the floor it turns to sculk. */
    private static final int DEPTH = 6;
    private static final int HEIGHT = 8;

    /**
     * The floor columns inside a capacitor's border, or {@code null} columns when nothing closes it off.
     */
    private record Pen(@Nullable LongSet columns, int floorY, long surveyedAt, long expiresAt) {
        boolean holds(BlockPos pos) {
            return this.columns != null && pos.getY() >= this.floorY - DEPTH && pos.getY() <= this.floorY + HEIGHT
                    && this.columns.contains(ChunkPos.toLong(pos.getX(), pos.getZ()));
        }

        boolean covers(BlockPos pos) {
            return this.columns != null && this.columns.contains(ChunkPos.toLong(pos.getX(), pos.getZ()));
        }
    }

    private static final Map<ServerWorld, Map<BlockPos, Pen>> PENS = new WeakHashMap<>();

    public static void init() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENS.clear());
    }

    /** Called whenever {@code capacitor} pours out charge, so the sculk stays inside its border for a while. */
    public static void contain(ServerWorld world, BlockPos capacitor) {
        Map<BlockPos, Pen> pens = PENS.computeIfAbsent(world, w -> new HashMap<>());
        long now = world.getTime();
        Pen pen = pens.get(capacitor);
        if (pen == null || now - pen.surveyedAt() >= RECHECK_TICKS) {
            int floorY = floorLevel(world, capacitor);
            pen = new Pen(survey(world, capacitor, floorY), floorY, now, 0L);
        }
        pens.put(capacitor.toImmutable(), new Pen(pen.columns(), pen.floorY(), pen.surveyedAt(), now + LINGER_TICKS));
    }

    /** Whether a sculk cursor may move from {@code from} to {@code to}: never out of a pen. */
    public static boolean mayMove(WorldAccess world, BlockPos from, BlockPos to) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return true;
        }
        Map<BlockPos, Pen> pens = PENS.get(serverWorld);
        if (pens == null || pens.isEmpty()) {
            return true;
        }
        long now = serverWorld.getTime();
        boolean allowed = true;
        for (Iterator<Pen> iterator = pens.values().iterator(); iterator.hasNext(); ) {
            Pen pen = iterator.next();
            if (pen.expiresAt() < now) {
                iterator.remove();
            } else if (pen.holds(from) && !pen.covers(to)) {
                allowed = false;
            }
        }
        return allowed;
    }

    /**
     * The top of the floor around the capacitor: the lowest solid block among its neighbours, so it works whether
     * the catalyst is set into the floor or sitting on it, and one block placed beside it doesn't throw it off.
     */
    private static int floorLevel(ServerWorld world, BlockPos capacitor) {
        int floorY = Integer.MAX_VALUE;
        BlockPos.Mutable pos = new BlockPos.Mutable();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                for (int y = capacitor.getY(); y >= capacitor.getY() - 3; y--) {
                    pos.set(capacitor.getX() + dx, y, capacitor.getZ() + dz);
                    if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
                        floorY = Math.min(floorY, y);
                        break;
                    }
                }
            }
        }
        return floorY == Integer.MAX_VALUE ? capacitor.getY() - 1 : floorY;
    }

    /**
     * Flood fills the floor outwards from the capacitor, stopping at border blocks standing on it. Returns the
     * columns reached, or {@code null} if the fill gets out past {@link #RADIUS}, meaning there's a gap somewhere.
     */
    @Nullable
    private static LongSet survey(ServerWorld world, BlockPos capacitor, int floorY) {
        LongSet inside = new LongOpenHashSet();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        long start = ChunkPos.toLong(capacitor.getX(), capacitor.getZ());
        inside.add(start);
        queue.enqueue(start);
        BlockPos.Mutable pos = new BlockPos.Mutable();
        while (!queue.isEmpty()) {
            long column = queue.dequeueLong();
            int x = ChunkPos.getPackedX(column);
            int z = ChunkPos.getPackedZ(column);
            for (Direction direction : Direction.Type.HORIZONTAL) {
                int nextX = x + direction.getOffsetX();
                int nextZ = z + direction.getOffsetZ();
                long next = ChunkPos.toLong(nextX, nextZ);
                if (inside.contains(next) || isBorder(world, pos.set(nextX, floorY + 1, nextZ))) {
                    continue;
                }
                if (Math.abs(nextX - capacitor.getX()) > RADIUS || Math.abs(nextZ - capacitor.getZ()) > RADIUS) {
                    return null;
                }
                inside.add(next);
                queue.enqueue(next);
            }
        }
        return inside;
    }

    /**
     * Something solid standing on the floor that sculk can't take over, like planks, cobblestone, glass or a fence.
     * Natural stone and sculk itself don't count, so bumps in a cave floor never wall a capacitor in.
     */
    private static boolean isBorder(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return !state.isAir() && !state.isIn(BlockTags.SCULK_REPLACEABLE) && !(state.getBlock() instanceof SculkSpreadable)
                && !state.getCollisionShape(world, pos).isEmpty();
    }

    private SculkContainment() {
    }
}
