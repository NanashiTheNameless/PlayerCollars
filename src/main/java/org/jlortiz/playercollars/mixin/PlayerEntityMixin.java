package org.jlortiz.playercollars.mixin;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Pair;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.jlortiz.playercollars.item.PawsItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    @Shadow @Final PlayerInventory inventory;

    @Shadow @Nullable public abstract ItemEntity dropItem(ItemStack stack, boolean retainOwnership);

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Redirect(method = "getBlockBreakingSpeed", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getBlockBreakingSpeed(Lnet/minecraft/block/BlockState;)F"))
    private float getBlockBreakingSpeed(PlayerInventory instance, BlockState block) {
        float ret = instance.getBlockBreakingSpeed(block);
        if (ret == 1) return ret;
        return TrinketsApi.getTrinketComponent(this).map((x) -> x.getEquipped(PlayerCollarsMod.PAWS_ITEM))
                .filter((x) -> !x.isEmpty()).map((x) -> (ret - 1) * 0.125f + 1).orElse(ret);
    }

    @Redirect(method="attack", at= @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getAttributeValue(Lnet/minecraft/registry/entry/RegistryEntry;)D", ordinal=0))
    private double getAttributeValue(PlayerEntity instance, RegistryEntry<EntityAttribute> registryEntry) {
        double ret = instance.getAttributeValue(registryEntry);
        return TrinketsApi.getTrinketComponent(this).map((x) -> x.getEquipped(PlayerCollarsMod.PAWS_ITEM))
                .filter((x) -> !x.isEmpty()).map((x) -> (ret - 1) * 0.75f + 1).orElse(ret);
    }

    @Inject(method = "createPlayerAttributes", at = @At("RETURN"))
    private static void playercollars$addAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.getReturnValue().add(PlayerCollarsMod.ATTR_LEASH_DISTANCE).add(PlayerCollarsMod.ATTR_CLICKER_DISTANCE);
    }

    @Inject(method = "tickMovement", at= @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;updateItems()V", shift = At.Shift.AFTER))
    private void playercollars$dropPawItems(CallbackInfo ci) {
        TrinketsApi.getTrinketComponent(this).map((x) -> x.getEquipped(PlayerCollarsMod.COLLAR_ITEM))
                .ifPresent((ls) -> {
                    for (Pair<SlotReference, ItemStack> p : ls) {
                        if (PawsItem.isSlippery(p.getRight())) {
                            ItemStack stack = inventory.dropSelectedItem(true);
                            if (!stack.isEmpty()) dropItem(stack, true);
                            stack = inventory.removeStack(PlayerInventory.OFF_HAND_SLOT);
                            if (!stack.isEmpty()) dropItem(stack, true);
                            return;
                        }
                    }
                });
    }
}
