package org.jlortiz.playercollars.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jlortiz.playercollars.PlayerCollarsMod;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public class PawSetupItem extends Item {
    public PawSetupItem() {
        super(new Item.Settings().maxCount(1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        BlockState blockState = (new CachedBlockPosition(context.getWorld(), context.getBlockPos(), false)).getBlockState();
        if (blockState == null) return ActionResult.FAIL;
        if (blockState.isIn(PlayerCollarsMod.PAWS_ALLOW_INTERACT)) {
            if (context.getPlayer() != null && context.getWorld().isClient)
                context.getPlayer().sendMessage(Text.translatable(
                        "item.playercollars.paw_configurator.no_remove",
                        blockState.getBlock().getName()).formatted(Formatting.RED)
                        , false);
            return ActionResult.PASS;
        }
        Optional<RegistryKey<Block>> key = blockState.getRegistryEntry().getKey();
        if (key.isEmpty()) return ActionResult.PASS;
        Identifier id = key.get().getValue();

        Set<Identifier> checked = context.getStack().get(PlayerCollarsMod.CAN_INTERACT_COMPONENT_TYPE);
        if (checked == null) checked = Set.of();
        Stream<Identifier> output;
        if (checked.contains(id)) {
            output = checked.stream().filter((ident) -> !ident.equals(id));
            if (context.getPlayer() != null && context.getWorld().isClient)
                context.getPlayer().sendMessage(Text.translatable(
                        "item.playercollars.paw_configurator.removed",
                        blockState.getBlock().getName()), false);
        } else {
            output = Stream.concat(checked.stream(), Stream.of(id));
            if (context.getPlayer() != null && context.getWorld().isClient)
                context.getPlayer().sendMessage(Text.translatable(
                        "item.playercollars.paw_configurator.added",
                        blockState.getBlock().getName()), false);
        }

        checked = Set.of(output.toArray(Identifier[]::new));
        context.getStack().set(PlayerCollarsMod.CAN_INTERACT_COMPONENT_TYPE, checked);
        return ActionResult.SUCCESS;
    }
}
