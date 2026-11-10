package net.bennyboops.backpackmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

public class BackpackMod implements ModInitializer {
    public static final String MOD_ID = "backpackmod";

    // Store all backpack items by color
    public static final Map<BackpackColor, Item> BACKPACK_ITEMS = new HashMap<>();

    public static ScreenHandlerType<BackpackScreenHandler> BACKPACK_SCREEN_HANDLER;
    public static RecipeSerializer<BackpackDyeRecipe> BACKPACK_DYE_RECIPE_SERIALIZER;

    // Player inventory index of the chest armor slot
    public static final int CHEST_SLOT = 38;

    public static final Identifier OPEN_BACKPACK_PACKET_ID = new Identifier(MOD_ID, "open_backpack");

    @Override
    public void onInitialize() {
        for (BackpackColor color : BackpackColor.values()) {
            Item backpackItem = new BackpackItem(new Item.Settings().maxCount(1), color);
            Registry.register(Registries.ITEM,
                    new Identifier(MOD_ID, "backpack_" + color.getName()),
                    backpackItem);
            BACKPACK_ITEMS.put(color, backpackItem);
        }

        BACKPACK_SCREEN_HANDLER = Registry.register(
                Registries.SCREEN_HANDLER,
                new Identifier(MOD_ID, "backpack"),
                new ExtendedScreenHandlerType<>((syncId, inventory, buf) -> new BackpackScreenHandler(syncId, inventory, buf.readVarInt()))
        );

        BACKPACK_DYE_RECIPE_SERIALIZER = Registry.register(
                Registries.RECIPE_SERIALIZER,
                new Identifier(MOD_ID, "backpack_dye"),
                new BackpackDyeRecipe.Serializer()
        );

        ServerPlayNetworking.registerGlobalReceiver(OPEN_BACKPACK_PACKET_ID, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                ItemStack chestSlot = player.getInventory().getStack(CHEST_SLOT);
                if (isBackpack(chestSlot)) {
                    player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_BUNDLE_INSERT, SoundCategory.PLAYERS, 1.00F, 0.75F);

                    openBackpack(player, CHEST_SLOT);
                }
            });
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
            for (BackpackColor color : BackpackColor.values()) {
                content.add(BACKPACK_ITEMS.get(color));
            }
        });
    }

    // Opens the backpack stored in the given player inventory slot
    public static void openBackpack(PlayerEntity player, int slot) {
        ItemStack stack = player.getInventory().getStack(slot);
        if (!isBackpack(stack)) {
            return;
        }

        player.openHandledScreen(new ExtendedScreenHandlerFactory() {
            @Override
            public void writeScreenOpeningData(ServerPlayerEntity serverPlayer, PacketByteBuf buf) {
                buf.writeVarInt(slot);
            }

            @Override
            public Text getDisplayName() {
                return BackpackItem.getDisplayName(stack);
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity p) {
                return new BackpackScreenHandler(syncId, playerInventory, slot, stack);
            }
        });
    }

    public static boolean isBackpack(ItemStack stack) {
        return BACKPACK_ITEMS.containsValue(stack.getItem());
    }
}
