package org.jlortiz.playercollars.mixin;

import com.mojang.authlib.GameProfile;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jlortiz.playercollars.OwnerWalkerImpl;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.jlortiz.playercollars.leash.LeashProxyEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntity implements OwnerWalkerImpl {
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
        if (isSneaking() || playercollars$ownerToWalkTo.isRemoved()) {
            playercollars$ownerToWalkTo.proxyRemove();
            playercollars$ownerToWalkTo = null;
            return;
        }

        if (hasVehicle()) {
            stopRiding();
        }

        ActionResult result = PlayerCollarsMod.pullPlayerTowards((ServerPlayerEntity) (Object) this,
                playercollars$ownerToWalkTo.getPos(), 0.5, 1024d, (x) -> 0.6);
        if (result == ActionResult.FAIL || (result == ActionResult.PASS && !playercollars$ownerToWalkTo.isNavigating())) {
            playercollars$ownerToWalkTo.proxyRemove();
            playercollars$ownerToWalkTo = null;
        }
    }

    @Override
    public void playercollars$walkToOwner(LivingEntity owner, double maxDistance) {
        if (playercollars$ownerToWalkTo != null) playercollars$ownerToWalkTo.proxyRemove();
        LeashProxyEntity proxy = new LeashProxyEntity(this, owner.getBlockPos(), (int) maxDistance);
        getWorld().spawnEntity(proxy);
        playercollars$ownerToWalkTo = proxy;
    }
}
