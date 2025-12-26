package org.jlortiz.playercollars.client;

import io.wispforest.accessories.api.client.renderers.AccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotPath;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import org.joml.Quaternionf;

import java.util.Optional;

public class CollarRenderer implements AccessoryRenderer {

    @Override
    public <S extends LivingEntityRenderState> void render(ItemStack itemStack, SlotPath slotReference, MatrixStack matrixStack, EntityModel<S> entityModel, S s, VertexConsumerProvider vertexConsumerProvider, int i, float v) {
        Optional<LivingEntity> entity = s.getEntityForState();
        if (entity.isEmpty() || !(entity.get() instanceof PlayerEntity p)) return;
        ModelPart body = ((PlayerEntityModel) entityModel).body;
        boolean hasChestplate = !p.getEquippedStack(EquipmentSlot.CHEST).isEmpty();

        matrixStack.translate(body.originX * 0.0625f, body.originY * 0.0625f, body.originZ * 0.0625f);
        matrixStack.multiply(new Quaternionf().rotateXYZ(body.pitch, body.yaw, body.roll + (float) Math.PI));
        matrixStack.scale((hasChestplate ? 0.7f : 0.85f) * body.xScale, 0.85f * body.yScale, (hasChestplate ? 1.1f : 0.85f) * body.zScale);
        matrixStack.translate(0, hasChestplate ? 0.475 : 0.4125, -0.005);
        ItemStack is2 = itemStack.copy();
        is2.remove(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE);
        is2.remove(DataComponentTypes.ENCHANTMENTS);
        MinecraftClient.getInstance().getItemRenderer().renderItem(is2, ItemDisplayContext.HEAD, i, OverlayTexture.DEFAULT_UV, matrixStack, vertexConsumerProvider, p.getWorld(), 0);
    }
}
