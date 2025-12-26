package org.jlortiz.playercollars.leash.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jlortiz.playercollars.leash.LeashImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class MixinLeashKnotEntity {
    // Why is this in Entity and not LeashKnotEntity???
    @Inject(method = "snipAllHeldLeashes", at = @At(value = "HEAD"), cancellable = true)
    private void preventBreakKnot(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        Entity holder = ((LeashImpl) player).leashplayers$getProxyLeashHolder();
        if (holder.equals(this)) {
            player.sendMessage(Text.translatable("message.playercollars.no_break_fence").formatted(Formatting.RED), true);
            cir.setReturnValue(false);
        }
    }
}
