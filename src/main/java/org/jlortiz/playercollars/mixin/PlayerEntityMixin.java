package org.jlortiz.playercollars.mixin;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotEntryReference;
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
        AccessoriesCapability cap = AccessoriesCapability.get(this);
        if (cap == null) return ret;
        if (cap.getEquipped(PlayerCollarsMod.PAWS_ITEM).isEmpty()) return ret;
        return (ret - 1) * 0.125f + 1;
    }

    @Redirect(method="attack", at= @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getAttributeValue(Lnet/minecraft/registry/entry/RegistryEntry;)D", ordinal=0))
    private double getAttributeValue(PlayerEntity instance, RegistryEntry<EntityAttribute> registryEntry) {
        double ret = instance.getAttributeValue(registryEntry);
        AccessoriesCapability cap = AccessoriesCapability.get(this);
        if (cap == null) return ret;
        if (cap.getEquipped(PlayerCollarsMod.PAWS_ITEM).isEmpty()) return ret;
        return (ret - 1) * 0.75f + 1;
    }

    @Inject(method = "createPlayerAttributes", at = @At("RETURN"))
    private static void playercollars$addAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.getReturnValue().add(PlayerCollarsMod.ATTR_LEASH_DISTANCE).add(PlayerCollarsMod.ATTR_CLICKER_DISTANCE);
    }

    @Inject(method = "tickMovement", at= @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;updateItems()V", shift = At.Shift.AFTER))
    private void playercollars$dropPawItems(CallbackInfo ci) {
        AccessoriesCapability cap = AccessoriesCapability.get(this);
        if (cap == null) return;
        for (SlotEntryReference sr : cap.getEquipped(PlayerCollarsMod.PAWS_ITEM)) {
            if (PawsItem.isSlippery(sr.stack())) {
                ItemStack stack = inventory.dropSelectedItem(true);
                if (!stack.isEmpty()) dropItem(stack, true);
                stack = inventory.removeStack(PlayerInventory.OFF_HAND_SLOT);
                if (!stack.isEmpty()) dropItem(stack, true);
                return;
            }
        }
    }
}
