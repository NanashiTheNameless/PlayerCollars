package org.jlortiz.playercollars.mixin;

import com.mojang.authlib.GameProfile;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jlortiz.playercollars.OwnerWalkerImpl;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.jlortiz.playercollars.leash.LeashProxyEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements OwnerWalkerImpl {
    @Shadow public abstract boolean teleport(ServerWorld world, double destX, double destY, double destZ, Set<PositionFlag> flags, float yaw, float pitch, boolean resetCamera);

    @Shadow public ServerPlayNetworkHandler networkHandler;

    public ServerPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Unique private LeashProxyEntity playercollars$ownerToWalkTo;

    @Inject(at=@At("TAIL"), method="damage")
    private void checkCollarThorns(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.getAttacker() != null) {
            AccessoriesCapability cap = AccessoriesCapability.get(this);
            if (cap == null) return;
            for (SlotEntryReference ser : cap.getEquipped((x) -> x.isIn(PlayerCollarsMod.COLLAR_TAG)))
                EnchantmentHelper.onTargetDamaged(world, source.getAttacker(), source, ser.stack());
        }
    }


    @Inject(method="tick", at=@At("TAIL"))
    private void tickWalkToOwner(CallbackInfo ci) {
        if (getWorld().isClient || playercollars$ownerToWalkTo == null) return;
        if (playercollars$ownerToWalkTo.isRemoved()) {
            playercollars$ownerToWalkTo = null;
            return;
        }
        if (isSneaking() || (!playercollars$ownerToWalkTo.isNavigating() && squaredDistanceTo(playercollars$ownerToWalkTo) < 0.25f)) {
            playercollars$ownerToWalkTo.proxyRemove();
            playercollars$ownerToWalkTo = null;
            return;
        }

        if (hasVehicle()) {
            stopRiding();
        }

        double dx = (playercollars$ownerToWalkTo.getX() - getX());
        double dy = (playercollars$ownerToWalkTo.getY() - getY());
        double dz = (playercollars$ownerToWalkTo.getZ() - getZ());
        setVelocity(dx * 0.6, dy * 0.6, dz * 0.6);
        networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(this));
        velocityDirty = false;
    }

    @Override
    public void playercollars$walkToOwner(LivingEntity owner, double maxDistance) {
        if (playercollars$ownerToWalkTo != null) playercollars$ownerToWalkTo.proxyRemove();
        LeashProxyEntity proxy = new LeashProxyEntity(this, owner.getBlockPos(), (int) maxDistance);
        getWorld().spawnEntity(proxy);
        playercollars$ownerToWalkTo = proxy;
    }
}
