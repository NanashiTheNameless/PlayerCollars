package org.jlortiz.playercollars.client;

import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import org.jlortiz.playercollars.PlayerCollarsMod;
import org.jlortiz.playercollars.item.FootPawsItem;
import org.jlortiz.playercollars.network.PacketLookAtLerped;

@Environment(EnvType.CLIENT)
public class RegisterClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AccessoriesRendererRegistry.registerRenderer(PlayerCollarsMod.COLLAR_ITEM, CollarRenderer::new);
        PawRenderer renderer = new PawRenderer();
        for (FootPawsItem p : PlayerCollarsMod.PAWS_ITEMS)
            AccessoriesRendererRegistry.registerRenderer(p, () -> renderer);
        FootPawRenderer renderer2 = new FootPawRenderer();
        for (FootPawsItem p : PlayerCollarsMod.FOOT_PAWS_ITEMS)
            AccessoriesRendererRegistry.registerRenderer(p, () -> renderer2);
        ClientPlayNetworking.registerGlobalReceiver(PacketLookAtLerped.ID, (payload, context) ->
                context.client().execute(() -> RotationLerpHandler.beginClickTurn(payload.vec())));
        WorldRenderEvents.END.register(RotationLerpHandler::turnTowardsClick);
    }
}
