package com.chanwoo.deeperdark.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/** Runs client effects a few ticks later, so particle effects can play out over time. */
@Environment(EnvType.CLIENT)
public final class ClientScheduler {
    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> ADDED = new ArrayList<>();

    private static final class Task {
        private final ClientWorld world;
        private final Consumer<ClientWorld> action;
        private int delay;

        private Task(ClientWorld world, int delay, Consumer<ClientWorld> action) {
            this.world = world;
            this.delay = delay;
            this.action = action;
        }
    }

    /** Runs {@code action} after {@code delay} ticks, unless the player has left that world by then. */
    public static void schedule(ClientWorld world, int delay, Consumer<ClientWorld> action) {
        if (delay <= 0) {
            action.accept(world);
        } else {
            ADDED.add(new Task(world, delay, action));
        }
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(ClientScheduler::tick);
    }

    private static void tick(MinecraftClient client) {
        TASKS.addAll(ADDED);
        ADDED.clear();
        Iterator<Task> iterator = TASKS.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            if (task.world != client.world) {
                iterator.remove();
            } else if (--task.delay <= 0) {
                iterator.remove();
                task.action.accept(task.world);
            }
        }
    }

    private ClientScheduler() {
    }
}
