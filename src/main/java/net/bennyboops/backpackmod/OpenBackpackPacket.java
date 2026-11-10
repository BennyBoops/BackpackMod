package net.bennyboops.backpackmod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenBackpackPacket() {
    public void encode(FriendlyByteBuf buf) {
    }

    public static OpenBackpackPacket decode(FriendlyByteBuf buf) {
        return new OpenBackpackPacket();
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                BackpackMod.openFromChestSlot(player);
            }
        });
        context.setPacketHandled(true);
    }
}
