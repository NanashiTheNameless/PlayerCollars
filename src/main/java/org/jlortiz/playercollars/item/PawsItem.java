package org.jlortiz.playercollars.item;

import io.wispforest.accessories.api.AccessoryItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jlortiz.playercollars.PlayerCollarsMod;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PawsItem extends AccessoryItem {
    public static final RegistryKey<Item> REGISTRY_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(PlayerCollarsMod.MOD_ID, "paws"));

    public PawsItem() {
        super(new Item.Settings().maxCount(1).registryKey(REGISTRY_KEY));
    }

    public static boolean shouldPreventBlockInteraction(ItemStack stack, @NotNull BlockState block) {
        if (block.isIn(PlayerCollarsMod.PAWS_ALLOW_INTERACT)) return false;
        Set<Identifier> allowed = stack.get(PlayerCollarsMod.CAN_INTERACT_COMPONENT_TYPE);
        Optional<RegistryKey<Block>> key = block.getRegistryEntry().getKey();
        return allowed != null && key.isPresent() && !allowed.contains(key.get().getValue());
    }

    public static boolean isSlippery(ItemStack stack) {
        Boolean slippery = stack.get(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
        return slippery != null && slippery;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        if (isSlippery(stack)) tooltip.add(Text.translatable("item.playercollars.paws.slippery"));
    }
}
