package org.jlortiz.playercollars.client;

import dev.emi.trinkets.api.client.TrinketRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BedBlock;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.item.BedItem;
import net.minecraft.util.Identifier;
import org.jlortiz.playercollars.PacketLookAtLerped;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.jlortiz.playercollars.item.CollarItem;
import org.jlortiz.playercollars.item.PawsItem;

@Environment(EnvType.CLIENT)
public class RegisterClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> switch (tintIndex) {
            case 0 -> CollarItem.getColor(stack) | 0xff000000;
            case 1 -> CollarItem.getPawColor(stack) | 0xff000000;
            default -> -1;
        }, PlayerCollarsMod.COLLAR_ITEM);
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> tintIndex == 0 ? PlayerCollarsMod.CLICKER_ITEM.getColor(stack) | 0xff000000 : -1, PlayerCollarsMod.CLICKER_ITEM);
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> tintIndex == 0 ? ((BedBlock) ((BedItem) stack.getItem()).getBlock()).getColor().getFireworkColor() | 0xff000000 : -1, PlayerCollarsMod.DOG_BED_ITEMS);
        ColorProviderRegistry.ITEM.register((stack, tintIndex) -> switch (tintIndex) {
            case 0 -> PawsItem.getColor(stack);
            case 1 -> PawsItem.getBeanColor(stack);
            default -> -1;
        }, PlayerCollarsMod.PAWS_ITEMS);
        ModelPredicateProviderRegistry.register(PlayerCollarsMod.CLICKER_ITEM, Identifier.ofVanilla("cast"), (itemStack, clientWorld, livingEntity, seed) -> livingEntity != null && livingEntity.isUsingItem() && livingEntity.getActiveItem() == itemStack ? 1 : 0);

        TrinketRendererRegistry.registerRenderer(PlayerCollarsMod.COLLAR_ITEM, new CollarRenderer());
        PawRenderer pr = new PawRenderer();
        for (PawsItem x : PlayerCollarsMod.PAWS_ITEMS)
            TrinketRendererRegistry.registerRenderer(x, pr);
        ClientPlayNetworking.registerGlobalReceiver(PacketLookAtLerped.ID, (payload, context) -> context.client().execute(() -> RotationLerpHandler.beginClickTurn(payload.vec())));
        WorldRenderEvents.END.register(RotationLerpHandler::turnTowardsClick);
    }
}
