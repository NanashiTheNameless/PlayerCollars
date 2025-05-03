package org.jlortiz.playercollars.item;

import io.wispforest.accessories.api.AccessoryItem;
import net.fabricmc.fabric.api.tag.convention.v2.TagUtil;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockPredicatesChecker;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jlortiz.playercollars.PlayerCollarsMod;

import java.util.List;

public class PawsItem extends AccessoryItem {
    public static final RegistryKey<Item> REGISTRY_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(PlayerCollarsMod.MOD_ID, "paws"));

    public PawsItem() {
        super(new Item.Settings().maxCount(1).registryKey(REGISTRY_KEY));
    }

    public static boolean shouldPreventBlockInteraction(ItemStack stack, CachedBlockPosition block) {
        if (block.getBlockState() == null) return false;
        if(TagUtil.isIn(PlayerCollarsMod.PAWS_ALLOW_INTERACT, block.getBlockState().getBlock())) return false;
        BlockPredicatesChecker allowed = stack.get(DataComponentTypes.CAN_BREAK);
        return allowed == null || !allowed.check(block);
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
