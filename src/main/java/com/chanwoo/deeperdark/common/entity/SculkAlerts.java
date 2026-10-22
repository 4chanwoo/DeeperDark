package com.chanwoo.deeperdark.common.entity;

import com.chanwoo.deeperdark.common.block.AncientVaseBlock;
import com.chanwoo.deeperdark.core.DeeperDarkGameEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.SculkSensorBlock;
import net.minecraft.block.SculkShriekerBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SculkSensorBlockEntity;
import net.minecraft.block.entity.SculkShriekerBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.VibrationParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.event.BlockPositionSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Carries vase screams to sculk sensors and shriekers. Vanilla listeners only hear vibrations within 8 blocks, a tick
 * late, and shriekers ignore anything without a player behind it, so a scream would often reach them silently or not
 * at all. Instead each scream sends a vibration to every idle sensor and shrieker within
 * {@link AncientVaseBlockEntity#RANGE} blocks, and sets it off when the vibration lands.
 */
public final class SculkAlerts {
    /** A vibration on its way to a sensor or shrieker. */
    private record Alert(ServerWorld world, BlockPos target, BlockPos origin, @Nullable Entity source, float distance, long arrival) {
    }

    private static final List<Alert> IN_FLIGHT = new ArrayList<>();

    public static void init() {
        ServerTickEvents.END_WORLD_TICK.register(SculkAlerts::deliver);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> IN_FLIGHT.clear());
    }

    /** Sends a vibration from {@code origin} to every sculk sensor and shrieker in range that would answer it. */
    public static void alert(ServerWorld world, Vec3d origin, @Nullable Entity source) {
        int range = AncientVaseBlockEntity.RANGE;
        BlockPos center = new BlockPos(origin);
        int minX = ChunkSectionPos.getSectionCoord(center.getX() - range);
        int maxX = ChunkSectionPos.getSectionCoord(center.getX() + range);
        int minZ = ChunkSectionPos.getSectionCoord(center.getZ() - range);
        int maxZ = ChunkSectionPos.getSectionCoord(center.getZ() + range);
        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!isListening(blockEntity) || isInFlight(world, blockEntity.getPos())) {
                        continue;
                    }
                    Vec3d target = Vec3d.ofCenter(blockEntity.getPos());
                    double distance = origin.distanceTo(target);
                    if (distance > range || AncientVaseBlockEntity.isOccluded(world, origin, target)) {
                        continue;
                    }
                    int delay = AncientVaseBlockEntity.flightTicks(distance);
                    AncientVaseBlock.spawnForEveryone(world, new VibrationParticleEffect(new BlockPositionSource(blockEntity.getPos()), delay), origin);
                    IN_FLIGHT.add(new Alert(world, blockEntity.getPos(), center, source, (float) distance, world.getTime() + delay));
                }
            }
        }
    }

    /** Idle sensors and shriekers. Ones already reacting to something would just swallow the vibration. */
    private static boolean isListening(BlockEntity blockEntity) {
        if (blockEntity instanceof SculkSensorBlockEntity) {
            return SculkSensorBlock.isInactive(blockEntity.getCachedState());
        }
        return blockEntity instanceof SculkShriekerBlockEntity && !blockEntity.getCachedState().get(SculkShriekerBlock.SHRIEKING);
    }

    /** One vibration per listener at a time, so several vases screaming together don't stack particles on it. */
    private static boolean isInFlight(ServerWorld world, BlockPos target) {
        for (Alert alert : IN_FLIGHT) {
            if (alert.world() == world && alert.target().equals(target)) {
                return true;
            }
        }
        return false;
    }

    private static void deliver(ServerWorld world) {
        Iterator<Alert> iterator = IN_FLIGHT.iterator();
        List<Alert> landed = new ArrayList<>();
        while (iterator.hasNext()) {
            Alert alert = iterator.next();
            if (alert.world() == world && alert.arrival() <= world.getTime()) {
                iterator.remove();
                landed.add(alert);
            }
        }
        // Set off afterwards: a sensor clicking can make vanilla listeners react, which must not touch the list mid-loop.
        for (Alert alert : landed) {
            BlockEntity blockEntity = world.getBlockEntity(alert.target());
            if (blockEntity instanceof SculkSensorBlockEntity sensor) {
                // Scaled to the sensor's own range, so its redstone strength still falls off over the scream's reach.
                float distance = alert.distance() * sensor.getEventListener().getRange() / AncientVaseBlockEntity.RANGE;
                sensor.accept(world, sensor.getEventListener(), alert.origin(), DeeperDarkGameEvents.VASE_SCREAM,
                        alert.source(), alert.source(), distance);
            } else if (blockEntity instanceof SculkShriekerBlockEntity shrieker) {
                shrieker.shriek(world, responsiblePlayer(world, alert));
            }
        }
    }

    /**
     * Who the shrieker warns: whoever started the alarm, or for one set off by an explosion or a command, the nearest
     * player who could have heard it.
     */
    @Nullable
    private static ServerPlayerEntity responsiblePlayer(ServerWorld world, Alert alert) {
        ServerPlayerEntity player = SculkShriekerBlockEntity.findResponsiblePlayerFromEntity(alert.source());
        if (player != null) {
            return player;
        }
        Vec3d origin = Vec3d.ofCenter(alert.origin());
        PlayerEntity nearest = world.getClosestPlayer(origin.x, origin.y, origin.z, AncientVaseBlockEntity.ALARM_RADIUS,
                entity -> !entity.isSpectator());
        return nearest instanceof ServerPlayerEntity serverPlayer ? serverPlayer : null;
    }

    private SculkAlerts() {
    }
}
