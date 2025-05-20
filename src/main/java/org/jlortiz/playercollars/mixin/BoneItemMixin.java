package org.jlortiz.playercollars.mixin;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Items.class)
public abstract class BoneItemMixin {
    @Shadow public static Item register(String id, Item.Settings settings) { return null; }

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/Items;register(Ljava/lang/String;)Lnet/minecraft/item/Item;", ordinal = 29))
    private static Item injectEquippableBone(String id) {
        return register(id, new Item.Settings().equippable(EquipmentSlot.HEAD));
    }
}