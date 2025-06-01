package org.jlortiz.playercollars.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;
import org.jlortiz.playercollars.OwnerComponent;
import org.jlortiz.playercollars.PlayerCollarsMod;

import java.util.Optional;
import java.util.UUID;

public record PacketStampDeed(UUID stamper) implements CustomPayload {
    public static final CustomPayload.Id<PacketStampDeed> ID = new CustomPayload.Id<>(Identifier.of(PlayerCollarsMod.MOD_ID, "stamp_deed"));
    public static final PacketCodec<RegistryByteBuf, PacketStampDeed> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, PacketStampDeed::stamper, PacketStampDeed::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void handle(ServerPlayNetworking.Context context) {
        context.server().execute(() -> {
            ItemStack is = context.player().getMainHandStack();
            if (!is.isEmpty() && is.isOf(PlayerCollarsMod.DEED_OF_OWNERSHIP)) {
                OwnerComponent owner = is.get(PlayerCollarsMod.OWNER_COMPONENT_TYPE);
                if (owner == null || owner.owned().isPresent()) return;
                String plrName = context.player().getName().getString();
                is = new ItemStack(PlayerCollarsMod.DEED_OF_OWNERSHIP_STAMPED);
                is.set(PlayerCollarsMod.OWNER_COMPONENT_TYPE, new OwnerComponent(
                   owner.uuid(), owner.name(), Optional.of(stamper), Optional.of(plrName)
                ));
                context.player().getInventory().setStack(context.player().getInventory().selectedSlot, is);
            }
        });
    }
}
