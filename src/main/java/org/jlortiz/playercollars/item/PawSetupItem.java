package org.jlortiz.playercollars.item;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.Block;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.jlortiz.playercollars.network.PawsConfigScreenHandler;

import java.util.List;
import java.util.Optional;

public class PawSetupItem extends Item {
    public static final RegistryKey<Item> REGISTRY_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(PlayerCollarsMod.MOD_ID, "paw_configurator"));

    public PawSetupItem() {
        super(new Item.Settings().maxCount(1).registryKey(REGISTRY_KEY));
    }

    @Override
    public ItemStack getRecipeRemainder(ItemStack stack) {
        return stack;
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack is = user.getStackInHand(hand);
        if (!user.isSneaking() || world.isClient) return ActionResult.PASS;
        return useOnEntity(is, user, user, hand);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(entity instanceof PlayerEntity player) || user.getWorld().isClient) return ActionResult.PASS;
        AccessoriesCapability cap = AccessoriesCapability.get(player);
        if (cap == null) return ActionResult.PASS;

        ItemStack collarStack = PlayerCollarsMod.filterStacksByOwner(cap.getEquipped((x) -> x.isIn(PlayerCollarsMod.COLLAR_TAG)), user.getUuid());
        if (collarStack == null) {
            user.sendMessage(Text.translatable("item.playercollars.paw_configurator.no_set_non_owner").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }
        List<SlotEntryReference> pawsStack = cap.getEquipped((y) -> y.isIn(PlayerCollarsMod.PAWS_TAG));
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
                    ps[i] = pawsStack.get(i).stack();
                sc.setPawsStack(ps);
                return sc;
            }

            @Override
            public Text getDisplayName() {
                return Text.translatable("gui.playercollars.paw_block_configurator.title", user.getName());
            }

            @Override
            public Object getScreenOpeningData(ServerPlayerEntity player) {
                return Optional.ofNullable(pawsStack.get(0).stack().get(PlayerCollarsMod.CAN_INTERACT_COMPONENT_TYPE)).orElse(List.of());
            }
        });
        return ActionResult.SUCCESS;
    }
}
