package org.jlortiz.playercollars.item;

import io.wispforest.accessories.api.AccessoryItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.component.type.MapColorComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jlortiz.playercollars.PlayerCollarsMod;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public class PawsItem extends AccessoryItem {
    public static final RegistryKey<Item> REGISTRY_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(PlayerCollarsMod.MOD_ID, "paws"));

    public PawsItem(int color, int pawColor) {
        super(new Item.Settings().maxCount(1).registryKey(REGISTRY_KEY)
                .component(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color | 0xFF000000, false))
                .component(DataComponentTypes.MAP_COLOR, new MapColorComponent(pawColor))
        );
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

    @Override
    public ItemStack getRecipeRemainder(ItemStack stack) {
        return stack;
    }

    public static RegistryKey<Item> getRegistryKey(DyeColor c) {
        return RegistryKey.of(RegistryKeys.ITEM, Identifier.of(PlayerCollarsMod.MOD_ID, c.getName() + "_paws"));
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (!user.isSneaking()) return super.use(world, user, hand);
        if (Boolean.TRUE.equals(user.getStackInHand(hand).get(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE)))
            return super.use(world, user, hand);
        Hand otherHand = hand == Hand.MAIN_HAND ? Hand.OFF_HAND : Hand.MAIN_HAND;
        if (user.getStackInHand(otherHand).getItem() != Items.HONEY_BOTTLE)
            return super.use(world, user, hand);

        if (world.isClient) {
            user.playSound(SoundEvents.BLOCK_HONEY_BLOCK_PLACE);
            return ActionResult.CONSUME;
        }

        user.getStackInHand(hand).set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        if (!user.isCreative()) user.setStackInHand(otherHand, new ItemStack(Items.GLASS_BOTTLE));
        return ActionResult.CONSUME;
    }
}
