package com.chanwoo.deeperdark.common.entity;

import com.chanwoo.deeperdark.common.block.AncientVaseBlock;
import com.chanwoo.deeperdark.core.DeeperDarkBlockEntities;
import com.chanwoo.deeperdark.core.DeeperDarkGameEvents;
import com.chanwoo.deeperdark.core.DeeperDarkParticles;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.VibrationParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockStateRaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.event.BlockPositionSource;
import net.minecraft.world.event.GameEvent;
import net.minecraft.world.event.PositionSource;
import net.minecraft.world.event.listener.GameEventListener;
import org.jetbrains.annotations.Nullable;

/**
 * Lives in the base of every ancient vase. When another vase screams nearby, it sends a vibration from that vase
 * to this one, and screams itself once the vibration arrives, so the alarm sweeps across the whole area.
 * <p>
 * Every shattered vase starts a new alarm. It spreads to natural vases within {@link #ALARM_RADIUS} blocks of the
 * vase that broke, at most {@link #MAX_HOPS} vases deep. Each vase answers an alarm once, remembers its last few
 * alarms so crossing ones can't bounce back and forth, and rests while it is still ringing.
 * <p>
 * This listens immediately, like the sculk catalyst. Vanilla's {@code VibrationListener} only hears events on the
 * next tick (after a broken vase is already gone) and can only track one vibration at a time.
 */
public class AncientVaseBlockEntity extends BlockEntity implements GameEventListener {
    public static final int RANGE = 12;
    private static final int AURA_INTERVAL = 2;
    private static final double AURA_VIEW_DISTANCE = 32.0;
    private static final int MIN_FLIGHT_TICKS = 4;
    /** Vases closer than this still answer each other, but without a vibration flying between them. */
    private static final double MIN_PARTICLE_DISTANCE = 3.0;
    /** How far from the vase that broke an alarm can spread. */
    public static final double ALARM_RADIUS = 16.0;
    /** How many vases an alarm can be passed through. */
    public static final int MAX_HOPS = 2;
    private static final int REMEMBERED_ALARMS = 8;

    /**
     * One alarm spreading from a shattered vase.
     *
     * @param id     unique per shattered vase, never 0
     * @param origin where the vase that started it broke
     * @param hop    how many vases it has been passed through so far
     */
    public record Alarm(long id, Vec3d origin, int hop) {
        public static Alarm start(ServerWorld world, Vec3d origin) {
            long id = world.random.nextLong();
            return new Alarm(id == 0L ? 1L : id, origin, 0);
        }

        public Alarm next() {
            return new Alarm(this.id, this.origin, this.hop + 1);
        }
    }

    /** The alarm being emitted right now. Game events carry no payload, and immediate listeners hear them synchronously. */
    @Nullable
    private static Alarm emittingAlarm;

    private final BlockPositionSource positionSource;
    /** The last few alarms this vase heard or started, so it never answers one twice. */
    private final long[] heardAlarms = new long[REMEMBERED_ALARMS];
    private int nextHeard;
    private long lastScreamTime = Long.MIN_VALUE;
    /** Ticks until the vibration we accepted reaches us. 0 when nothing is on its way. */
    private int alarmDelay;
    @Nullable
    private Alarm pendingAlarm;
    @Nullable
    private Entity alarmSource;

    public AncientVaseBlockEntity(BlockPos pos, BlockState state) {
        super(DeeperDarkBlockEntities.ANCIENT_VASE, pos, state);
        this.positionSource = new BlockPositionSource(pos);
    }

    /**
     * Emits a vase scream belonging to {@code alarm}, so listeners can tell which alarm they're hearing, and sends
     * vibrations to the sculk sensors and shriekers around it.
     */
    public static void emitScream(ServerWorld world, Vec3d origin, GameEvent.Emitter emitter, Alarm alarm) {
        emittingAlarm = alarm;
        try {
            world.emitGameEvent(DeeperDarkGameEvents.VASE_SCREAM, origin, emitter);
        } finally {
            emittingAlarm = null;
        }
        SculkAlerts.alert(world, origin, emitter.sourceEntity());
    }

    /** Like vanilla, a vibration takes one tick per block, but never so few that it's gone before it's seen. */
    public static int flightTicks(double distance) {
        return Math.max(MIN_FLIGHT_TICKS, MathHelper.floor(distance));
    }

    /** Marks this vase as screaming for {@code alarm}, so it won't answer that alarm again. */
    public void joinAlarm(World world, Alarm alarm) {
        this.remember(alarm.id());
        this.lastScreamTime = world.getTime();
    }

    private void remember(long id) {
        this.heardAlarms[this.nextHeard] = id;
        this.nextHeard = (this.nextHeard + 1) % REMEMBERED_ALARMS;
    }

    private boolean hasHeard(long id) {
        for (long heard : this.heardAlarms) {
            if (heard == id) {
                return true;
            }
        }
        return false;
    }

    public boolean screamedThisTick(World world) {
        return this.lastScreamTime == world.getTime();
    }

    /** Old worlds may still have block entities on vases that were placed by players. */
    private static boolean isNatural(BlockState state) {
        return state.contains(AncientVaseBlock.GENERATED) && state.get(AncientVaseBlock.GENERATED);
    }

    public static void serverTick(World world, BlockPos pos, BlockState state, AncientVaseBlockEntity vase) {
        if (vase.alarmDelay > 0 && --vase.alarmDelay == 0
                && world instanceof ServerWorld serverWorld && state.getBlock() instanceof AncientVaseBlock block) {
            Entity source = vase.alarmSource;
            Alarm alarm = vase.pendingAlarm;
            vase.alarmSource = null;
            vase.pendingAlarm = null;
            if (alarm != null) {
                block.scream(serverWorld, pos, state, source, alarm);
            }
        }
    }

    /** Keeps a steady curl of smoke rising from the mouth. Random display ticks are far too rare for this. */
    public static void clientTick(World world, BlockPos pos, BlockState state, AncientVaseBlockEntity vase) {
        long time = world.getTime() + (pos.asLong() & 7);
        if (time % AURA_INTERVAL != 0 || !isNatural(state) || !(state.getBlock() instanceof AncientVaseBlock block)) {
            return;
        }
        Vec3d mouth = block.getMouthPos(pos);
        PlayerEntity player = world.getClosestPlayer(mouth.x, mouth.y, mouth.z, AURA_VIEW_DISTANCE, false);
        if (player != null) {
            // The particle factory reads the vase's left-right axis from the x velocity and the time from the y,
            // so the smoke sweeps across the vase's face and slowly wavers.
            double axis = Math.toRadians(state.get(AncientVaseBlock.FACING).asRotation());
            world.addParticle(DeeperDarkParticles.VASE_AURA, mouth.x, mouth.y, mouth.z, axis, world.getTime() + (pos.asLong() % 997), 0.0);
        }
    }

    @Override
    public boolean listen(ServerWorld world, GameEvent.Message event) {
        Alarm alarm = emittingAlarm;
        BlockState state = this.getCachedState();
        if (this.isRemoved() || event.getEvent() != DeeperDarkGameEvents.VASE_SCREAM || alarm == null
                || alarm.hop() >= MAX_HOPS || this.hasHeard(alarm.id()) || this.alarmDelay > 0
                || !isNatural(state) || state.get(AncientVaseBlock.RINGING)
                || !(state.getBlock() instanceof AncientVaseBlock vase)) {
            return false;
        }

        Vec3d from = event.getEmitterPos();
        Vec3d to = vase.getMouthPos(this.pos);
        if (to.squaredDistanceTo(alarm.origin()) > ALARM_RADIUS * ALARM_RADIUS || isOccluded(world, from, to)) {
            return false;
        }

        int delay = flightTicks(from.distanceTo(to));
        BlockPositionSource destination = new BlockPositionSource(this.pos.up(vase.getHeight() - 1));
        if (from.squaredDistanceTo(to) >= MIN_PARTICLE_DISTANCE * MIN_PARTICLE_DISTANCE) {
            AncientVaseBlock.spawnForEveryone(world, new VibrationParticleEffect(destination, delay), from);
        }

        this.remember(alarm.id());
        this.pendingAlarm = alarm.next();
        this.alarmDelay = delay;
        this.alarmSource = event.getEmitter().sourceEntity();
        return true;
    }

    /** Same rule as vanilla vibrations: blocked only when wool (or similar) is in the way from every side. */
    public static boolean isOccluded(World world, Vec3d start, Vec3d end) {
        Vec3d from = new Vec3d(MathHelper.floor(start.x) + 0.5, MathHelper.floor(start.y) + 0.5, MathHelper.floor(start.z) + 0.5);
        Vec3d to = new Vec3d(MathHelper.floor(end.x) + 0.5, MathHelper.floor(end.y) + 0.5, MathHelper.floor(end.z) + 0.5);
        for (Direction direction : Direction.values()) {
            BlockStateRaycastContext context = new BlockStateRaycastContext(from.withBias(direction, 1.0E-5f), to,
                    state -> state.isIn(BlockTags.OCCLUDES_VIBRATION_SIGNALS));
            if (world.raycast(context).getType() != HitResult.Type.BLOCK) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean shouldListenImmediately() {
        return true;
    }

    @Override
    public PositionSource getPositionSource() {
        return this.positionSource;
    }

    @Override
    public int getRange() {
        return RANGE;
    }

}
