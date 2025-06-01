package org.jlortiz.playercollars.leash.mixin;

import com.mojang.authlib.GameProfile;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.jlortiz.playercollars.leash.LeashImpl;
import org.jlortiz.playercollars.leash.LeashProxyEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements LeashImpl {
    @Shadow public abstract boolean isDisconnected();

    @Shadow public ServerPlayNetworkHandler networkHandler;
    @Unique
    private LeashProxyEntity leashplayers$proxy;
    @Unique
    private Entity leashplayers$holder;
    @Unique
    private int leashplayers$lastage;
    @Unique
    private double leashplayer$loyalty;

    public MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Unique
    private void leashplayers$update() {
        if (
                leashplayers$holder != null && (
                        !leashplayers$holder.isAlive()
                                || !isAlive()
                                || isDisconnected()
                                || hasVehicle()
                )
        ) {
            leashplayers$detach();
            leashplayers$drop();
        }

        if (leashplayers$proxy != null) {
            if (leashplayers$proxy.proxyIsRemoved()) {
                leashplayers$proxy = null;
            }
            else {
                Entity holderActual = leashplayers$holder;
                Entity holderTarget = leashplayers$proxy.getLeashHolder();

                if (holderTarget == null && holderActual != null) {
                    leashplayers$detach();
                    leashplayers$drop();
                }
                else if (holderTarget != holderActual) {
                    leashplayers$attach(holderTarget);
                }
            }
        }

        leashplayers$apply();
    }

    @Unique
    private void leashplayers$apply() {
        Entity holder = leashplayers$holder;
        if (holder == null) return;
        if (holder.getWorld() != getWorld()) return;

        float distance = distanceTo(holder);
        if (distance < leashplayer$loyalty) {
            return;
        }
        if (distance > 6 + leashplayer$loyalty) {
            leashplayers$detach();
            leashplayers$drop();
            return;
        }

        double dx = (holder.getX() - getX()) / (double) distance;
        double dy = (holder.getY() - getY()) / (double) distance;
        double dz = (holder.getZ() - getZ()) / (double) distance;
        final double factor = 0.4d + 0.1d * leashplayer$loyalty;

        addVelocity(
                Math.copySign(dx * dx * factor, dx),
                Math.copySign(dy * dy * factor, dy),
                Math.copySign(dz * dz * factor, dz)
        );

        networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(this));
        velocityDirty = false;
    }

    @Unique
    private void leashplayers$attach(Entity entity) {
        leashplayers$holder = entity;

        if (leashplayers$proxy == null) {
            leashplayers$proxy = new LeashProxyEntity(this);
            leashplayers$proxy.setPos(getX(), getY(), getZ());
            getWorld().spawnEntity(leashplayers$proxy);
        }
        leashplayers$proxy.attachLeash(leashplayers$holder, true);

        if (hasVehicle()) {
            stopRiding();
        }

        leashplayers$lastage = age;
    }

    @Unique
    private void leashplayers$detach() {
        leashplayers$holder = null;

        if (leashplayers$proxy != null) {
            if (leashplayers$proxy.isAlive() || !leashplayers$proxy.proxyIsRemoved()) {
                leashplayers$proxy.proxyRemove();
            }
            leashplayers$proxy = null;
        }
    }

    @Unique
    private void leashplayers$drop() {
        dropItem(new ItemStack(Items.LEAD), false, true);
    }

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void leashplayers$tick(CallbackInfo info) {
        leashplayers$update();
    }

    @Unique
    public Entity leashplayers$getProxyLeashHolder() {
        return leashplayers$proxy == null ? null : leashplayers$proxy.getLeashHolder();
    }

    @Override
    public ActionResult leashplayers$interact(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.getItem() == Items.LEAD && leashplayers$holder == null) {
            AtomicBoolean found = new AtomicBoolean(false);
            TrinketsApi.getTrinketComponent(this).map((x) -> x.getEquipped((y) -> y.isIn(PlayerCollarsMod.COLLAR_TAG)))
                    .map((x) -> PlayerCollarsMod.filterStacksByOwner(x, player.getUuid()))
                    .ifPresent((stack1) -> {
                        found.set(true);
                        leashplayer$loyalty = getAttributeValue(PlayerCollarsMod.ATTR_LEASH_DISTANCE);
                    });
            if (!found.get()) return ActionResult.PASS;
            if (!player.isCreative()) {
                stack.decrement(1);
            }
            leashplayers$attach(player);
            return ActionResult.SUCCESS;
        }

        if (leashplayers$holder == player && leashplayers$lastage + 20 < age) {
            if (!player.isCreative()) {
                leashplayers$drop();
            }
            leashplayers$detach();
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }
}