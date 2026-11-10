package net.bennyboops.backpackmod;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

@Mod(BackpackMod.MOD_ID)
public class BackpackMod {
    public static final String MOD_ID = "backpackmod";

    // Player inventory index of the chest armor slot
    public static final int CHEST_SLOT = 38;

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, MOD_ID);

    // Store all backpack items by color
    public static final Map<BackpackColor, DeferredItem<Item>> BACKPACK_ITEMS = new EnumMap<>(BackpackColor.class);

    static {
        for (BackpackColor color : BackpackColor.values()) {
            BACKPACK_ITEMS.put(color, ITEMS.register("backpack_" + color.getName(),
                    () -> new BackpackItem(new Item.Properties().stacksTo(1), color)));
        }
    }

    public static final DeferredHolder<MenuType<?>, MenuType<BackpackMenu>> BACKPACK_MENU = MENUS.register("backpack",
            () -> IMenuTypeExtension.create((containerId, inventory, buf) -> new BackpackMenu(containerId, inventory, buf.readVarInt())));

    public static final DeferredHolder<RecipeSerializer<?>, BackpackDyeRecipe.Serializer> BACKPACK_DYE_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("backpack_dye", BackpackDyeRecipe.Serializer::new);

    public BackpackMod(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);

        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::addCreative);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            BackpackModClient.init(modEventBus);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(OpenBackpackPayload.TYPE, OpenBackpackPayload.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    Player player = context.player();
                    ItemStack chestSlot = player.getInventory().getItem(CHEST_SLOT);
                    if (isBackpack(chestSlot)) {
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.00F, 0.75F);

                        openBackpack(player, CHEST_SLOT);
                    }
                }));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            for (BackpackColor color : BackpackColor.values()) {
                event.accept(BACKPACK_ITEMS.get(color).get());
            }
        }
    }

    // Opens the backpack stored in the given player inventory slot
    public static void openBackpack(Player player, int slot) {
        ItemStack stack = player.getInventory().getItem(slot);
        if (!isBackpack(stack)) {
            return;
        }

        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new BackpackMenu(containerId, playerInventory, slot, stack),
                BackpackItem.getDisplayName(stack)
        ), buf -> buf.writeVarInt(slot));
    }

    public static boolean isBackpack(ItemStack stack) {
        return stack.getItem() instanceof BackpackItem;
    }
}
