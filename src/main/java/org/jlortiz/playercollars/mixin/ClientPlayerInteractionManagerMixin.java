package org.jlortiz.playercollars.mixin;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.jlortiz.playercollars.item.PawsItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Inject(method = "interactBlock", at=@At("HEAD"), cancellable = true)
    private void playercollars$cancelPawInteractions(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (hand == Hand.OFF_HAND || player.isSpectator()) return;
        if (player.isSneaking() && !player.getStackInHand(hand).isEmpty()) return;
        AccessoriesCapability cap = AccessoriesCapability.get(player);
        if (cap == null) return;
        BlockState block = (new CachedBlockPosition(player.getWorld(), hitResult.getBlockPos(), false)).getBlockState();
        if (block == null) return;
        for (SlotEntryReference sr : cap.getEquipped(PlayerCollarsMod.PAWS_ITEM)) {
            if (PawsItem.shouldPreventBlockInteraction(sr.stack(), block)) {
                cir.setReturnValue(ActionResult.PASS);
                return;
            }
        }
    }
}
