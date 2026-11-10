package net.bennyboops.backpackmod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record OpenBackpackPacket() {
    public void encode(FriendlyByteBuf buf) {
    }

    public static OpenBackpackPacket decode(FriendlyByteBuf buf) {
        return new OpenBackpackPacket();
    }

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null) {
            BackpackMod.openFromChestSlot(player);
        }
    }
}
