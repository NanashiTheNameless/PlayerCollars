package io.github.nanashithenameless.playercollars.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import io.github.nanashithenameless.playercollars.OwnerComponent;
import io.github.nanashithenameless.playercollars.PlayerCollarsMod;

import java.util.List;

public class StampedDeedItem extends Item {
    public static final RegistryKey<Item> REGISTRY_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(PlayerCollarsMod.MOD_ID, "stamped_deed_of_ownership"));

    public StampedDeedItem() {
        super(new Item.Settings().maxCount(1).registryKey(REGISTRY_KEY));
    }

    @Override
    public Text getName(ItemStack stack) {
        OwnerComponent owner = stack.get(PlayerCollarsMod.OWNER_COMPONENT_TYPE);
        if (owner == null || owner.ownedName().isEmpty()) return Text.translatable("item.playercollars.stamped_deed_of_ownership.invalid");
        return Text.translatable("item.playercollars.stamped_deed_of_ownership", owner.ownedName().get());
    }


    @Override
    public ItemStack getRecipeRemainder(ItemStack stack) {
        return stack.copy();
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);
        OwnerComponent owner = stack.get(PlayerCollarsMod.OWNER_COMPONENT_TYPE);
        if (owner != null) {
            tooltip.add(Text.translatable("item.playercollars.collar.owner", owner.name()).formatted(Formatting.GRAY));
        }
    }
}
