package org.jlortiz.playercollars.mixin;

import com.mojang.authlib.GameProfile;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Pair;
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
    private void checkCollarThorns(DamageSource p_9037_, float p_9038_, CallbackInfoReturnable<Boolean> cir) {
        if (p_9037_.getAttacker() != null) {
            TrinketsApi.getTrinketComponent(this).map((x) -> x.getEquipped((y) -> y.isIn(PlayerCollarsMod.COLLAR_TAG)))
                    .ifPresent((ls) -> {
                        for (Pair<SlotReference, ItemStack> p : ls) {
                            EnchantmentHelper.onTargetDamaged((ServerWorld) getWorld(), p_9037_.getAttacker(), p_9037_, p.getRight());
                        }
                    });
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
