package org.jlortiz.playercollars.leash;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeNavigator;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkCache;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

public final class LeashProxyEntity extends TurtleEntity {
    private final LivingEntity target;
    private final boolean shouldNavigate;

    private boolean proxyUpdate() {
        if (proxyIsRemoved()) return false;

        if (target == null) return true;
        if (target.getWorld() != getWorld() || !target.isAlive()) return true;
        if (shouldNavigate) {
            navigation.tick();
            moveControl.tick();
            tickMovement();
            return false;
        }

        Vec3d posActual = this.getPos();
        Vec3d posTarget = target.getPos().add(0.0D, 1.3D, -0.15D);

        if (!Objects.equals(posActual, posTarget)) {
            setRotation(0.0F, 0.0F);
            setPos(posTarget.x, posTarget.y, posTarget.z);
            setBoundingBox(getDimensions(EntityPose.DYING).getBoxAt(posTarget));
        }

        return false;
    }

    @Override
    public void tick() {
        if (this.getWorld().isClient) return;
        if (proxyUpdate() && !proxyIsRemoved()) {
            proxyRemove();
        }
    }

    public boolean proxyIsRemoved() {
        return this.isRemoved();
    }

    public void proxyRemove() {
        super.remove(RemovalReason.DISCARDED);
    }

    @Override
    public void remove(RemovalReason reason) {
    }

    public static final String TEAM_NAME = "leashplayersimpl";

    public LeashProxyEntity(LivingEntity target) {
        this(target, null, 16);
    }

    public LeashProxyEntity(LivingEntity target, @Nullable BlockPos navigateTo, int followRange) {
        super(EntityType.TURTLE, target.getWorld());

        this.target = target;
        this.shouldNavigate = navigateTo != null;
        if (shouldNavigate) {
            setPosition(target.getPos());
            PathNodeMaker nodeMaker = new LandPathNodeMaker();
            nodeMaker.setCanSwim(true);
            PathNodeNavigator navigation = new PathNodeNavigator(nodeMaker, followRange);
            int i = followRange + 8;
            ChunkCache cache = new ChunkCache(this.getWorld(), this.getBlockPos().add(-i, -i, -i), this.getBlockPos().add(i, i, i));
            Path path = navigation.findPathToAny(cache, this, Set.of(navigateTo), followRange, 3, 1);
            getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).setBaseValue(1.75f);
            getAttributeInstance(EntityAttributes.JUMP_STRENGTH).setBaseValue(0.65f);
            this.navigation.startMovingAlong(path, 1);
        }

        setHealth(1.0F);
        setInvulnerable(true);

        setBaby(true);
        setInvisible(true);
        noClip = !shouldNavigate;

        MinecraftServer server = getServer();
        if (server != null) {
            ServerScoreboard scoreboard = server.getScoreboard();

            Team team = scoreboard.getTeam(TEAM_NAME);
            if (team == null) {
                team = scoreboard.addTeam(TEAM_NAME);
            }
            if (team.getCollisionRule() != Team.CollisionRule.NEVER) {
                team.setCollisionRule(Team.CollisionRule.NEVER);
            }

            scoreboard.addScoreHolderToTeam(getNameForScoreboard(), team);
        }
    }

    @Override
    public float getHealth() {
        return 1.0F;
    }

    @Override
    public void detachLeash() {
    }

    @Override
    public void detachLeashWithoutDrop() {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected void initGoals() {
    }

    @Override
    protected void pushAway(Entity entity) {
    }

    @Override
    public void pushAwayFrom(Entity entity) {
    }

    @Override
    public void onPlayerCollision(PlayerEntity player) {
    }
}