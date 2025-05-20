package org.jlortiz.playercollars.leash.mixin;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Items.class)
public abstract class BoneItemMixin {
    @Shadow public static Item register(String id, Item.Settings settings) { return null; }

    @Inject(method = "register(Ljava/lang/String;)Lnet/minecraft/item/Item;", at = @At("HEAD"), cancellable = true)
    private static void injectEquippableBone(String id, CallbackInfoReturnable<Item> cir) {
        if (!id.equals("bone")) return;
        cir.setReturnValue(register(id, new Item.Settings().equippable(EquipmentSlot.HEAD)));
    }
}