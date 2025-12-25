package net.bennyboops.backpackmod;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record OpenBackpackPayload() implements CustomPayload {
    public static final CustomPayload.Id<OpenBackpackPayload> ID =
            new CustomPayload.Id<>(BackpackMod.OPEN_BACKPACK_PACKET_ID);

    public static final PacketCodec<RegistryByteBuf, OpenBackpackPayload> CODEC =
            PacketCodec.unit(new OpenBackpackPayload());

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}