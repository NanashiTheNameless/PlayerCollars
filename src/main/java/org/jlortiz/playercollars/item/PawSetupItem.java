package org.jlortiz.playercollars.item;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.jlortiz.playercollars.network.PawsConfigScreenHandler;

import java.util.List;
import java.util.Optional;

public class PawSetupItem extends Item {
    public PawSetupItem() {
        super(new Settings().maxCount(1));
    }

    @Override
    public ItemStack getRecipeRemainder(ItemStack stack) {
        return stack;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack is = user.getStackInHand(hand);
        if (!user.isSneaking() || world.isClient) return TypedActionResult.pass(is);
        return switch (useOnEntity(is, user, user, hand)) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> TypedActionResult.success(is);
            case CONSUME, CONSUME_PARTIAL -> TypedActionResult.consume(is);
            case PASS -> TypedActionResult.pass(is);
            case FAIL -> TypedActionResult.fail(is);
        };
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(entity instanceof PlayerEntity player) || user.getWorld().isClient) return ActionResult.PASS;
        Optional<TrinketComponent> optComponent = TrinketsApi.getTrinketComponent(player);
        if (optComponent.isEmpty()) return ActionResult.PASS;
        TrinketComponent component = optComponent.get();

        ItemStack collarStack = PlayerCollarsMod.filterStacksByOwner(component.getEquipped((y) -> y.isIn(PlayerCollarsMod.COLLAR_TAG)), user.getUuid(), player.getUuid());
        if (collarStack == null) {
            user.sendMessage(Text.translatable("item.playercollars.paw_configurator.no_set_non_owner").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }
        List<Pair<SlotReference, ItemStack>> pawsStack = component.getEquipped((y) -> y.isIn(PlayerCollarsMod.PAWS_TAG));
        if (pawsStack.isEmpty()) {
            user.sendMessage(Text.translatable("item.playercollars.paw_configurator.no_paws").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        player.openHandledScreen(new ExtendedScreenHandlerFactory<>() {
            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                PawsConfigScreenHandler<Block> sc =  new PawsConfigScreenHandler.PawsBlockConfigScreenHandler(syncId, playerInventory);
                ItemStack[] ps = new ItemStack[pawsStack.size()];
                for (int i = 0; i < pawsStack.size(); i++)
                    ps[i] = pawsStack.get(i).getRight();
                sc.setPawsStack(ps);
                return sc;
            }

            @Override
            public Text getDisplayName() {
                return Text.translatable("gui.playercollars.paw_block_configurator.title", user.getName());
            }

            @Override
            public Object getScreenOpeningData(ServerPlayerEntity player) {
                return Optional.ofNullable(pawsStack.get(0).getRight().get(PlayerCollarsMod.CAN_INTERACT_COMPONENT_TYPE)).orElse(List.of());
            }
        });
        return ActionResult.SUCCESS;
    }
}
