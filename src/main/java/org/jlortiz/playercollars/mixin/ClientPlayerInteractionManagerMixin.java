package org.jlortiz.playercollars.mixin;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
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
        TrinketsApi.getTrinketComponent(player).map((x) -> x.getEquipped((y) -> y.isIn(PlayerCollarsMod.PAWS_TAG)))
                .ifPresent((ls) -> {
                    BlockState block = (new CachedBlockPosition(player.getWorld(), hitResult.getBlockPos(), false)).getBlockState();
                    for (Pair<SlotReference, ItemStack> p : ls) {
                        if (PawsItem.shouldPreventBlockInteraction(p.getRight(), block)) {
                            cir.setReturnValue(ActionResult.PASS);
                            return;
                        }
                    }
                });
    }
}
