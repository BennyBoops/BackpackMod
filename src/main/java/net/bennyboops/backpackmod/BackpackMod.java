package net.bennyboops.backpackmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
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

    public static final Identifier OPEN_BACKPACK_PACKET_ID = Identifier.of(MOD_ID, "open_backpack");

    @Override
    public void onInitialize() {
        for (BackpackColor color : BackpackColor.values()) {
            Item backpackItem = new BackpackItem(new Item.Settings().maxCount(1), color);
            Registry.register(Registries.ITEM,
                    Identifier.of(MOD_ID, "backpack_" + color.getName()),
                    backpackItem);
            BACKPACK_ITEMS.put(color, backpackItem);
        }

        BACKPACK_SCREEN_HANDLER = Registry.register(
                Registries.SCREEN_HANDLER,
                Identifier.of(MOD_ID, "backpack"),
                new ScreenHandlerType<>(BackpackScreenHandler::new, null)
        );

        BACKPACK_DYE_RECIPE_SERIALIZER = Registry.register(
                Registries.RECIPE_SERIALIZER,
                Identifier.of(MOD_ID, "backpack_dye"),
                new BackpackDyeRecipe.Serializer()
        );

        PayloadTypeRegistry.playC2S().register(OpenBackpackPayload.ID, OpenBackpackPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(OpenBackpackPayload.ID, (payload, context) -> {
            context.player().server.execute(() -> {
                ItemStack chestSlot = context.player().getInventory().getArmorStack(2);
                if (isBackpack(chestSlot)) {
                    context.player().getWorld().playSound(null, context.player().getX(), context.player().getY(), context.player().getZ(),
                            SoundEvents.ITEM_BUNDLE_INSERT, net.minecraft.sound.SoundCategory.PLAYERS, 1.00F, 0.75F);

                    context.player().openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, playerInventory, p) ->
                                    new BackpackScreenHandler(syncId, playerInventory, chestSlot),
                            Text.translatable("container.backpackmod.backpack")
                    ));
                }
            });
        });

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(content -> {
            for (BackpackColor color : BackpackColor.values()) {
                content.add(BACKPACK_ITEMS.get(color));
            }
        });
    }

    public static boolean isBackpack(ItemStack stack) {
        return BACKPACK_ITEMS.containsValue(stack.getItem());
    }
}