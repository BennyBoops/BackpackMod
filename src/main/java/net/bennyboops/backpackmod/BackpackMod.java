package net.bennyboops.backpackmod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

@Mod(BackpackMod.MOD_ID)
public class BackpackMod {
    public static final String MOD_ID = "backpackmod";

    // Player inventory index of the chest armor slot
    public static final int CHEST_SLOT = 38;

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MOD_ID);

    // Store all backpack items by color
    public static final Map<BackpackColor, RegistryObject<Item>> BACKPACK_ITEMS = new EnumMap<>(BackpackColor.class);

    static {
        for (BackpackColor color : BackpackColor.values()) {
            BACKPACK_ITEMS.put(color, ITEMS.register("backpack_" + color.getName(),
                    () -> new BackpackItem(new Item.Properties().stacksTo(1), color)));
        }
    }

    public static final RegistryObject<MenuType<BackpackMenu>> BACKPACK_MENU = MENUS.register("backpack",
            () -> IForgeMenuType.create((containerId, inventory, buf) -> new BackpackMenu(containerId, inventory, buf.readVarInt())));

    public static final RegistryObject<BackpackDyeRecipe.Serializer> BACKPACK_DYE_RECIPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("backpack_dye", BackpackDyeRecipe.Serializer::new);

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public BackpackMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ITEMS.register(modEventBus);
        MENUS.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);

        modEventBus.addListener(this::addCreative);

        CHANNEL.registerMessage(0, OpenBackpackPacket.class,
                OpenBackpackPacket::encode,
                OpenBackpackPacket::decode,
                OpenBackpackPacket::handle);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            BackpackModClient.init(modEventBus);
        }
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            for (BackpackColor color : BackpackColor.values()) {
                event.accept(BACKPACK_ITEMS.get(color));
            }
        }
    }

    // Called on the server when a player presses the backpack key
    public static void openFromChestSlot(Player player) {
        ItemStack chestSlot = player.getInventory().getItem(CHEST_SLOT);
        if (isBackpack(chestSlot)) {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.00F, 0.75F);

            openBackpack(player, CHEST_SLOT);
        }
    }

    // Opens the backpack stored in the given player inventory slot
    public static void openBackpack(Player player, int slot) {
        ItemStack stack = player.getInventory().getItem(slot);
        if (!isBackpack(stack) || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new BackpackMenu(containerId, playerInventory, slot, stack),
                BackpackItem.getDisplayName(stack)
        ), buf -> buf.writeVarInt(slot));
    }

    public static boolean isBackpack(ItemStack stack) {
        return stack.getItem() instanceof BackpackItem;
    }
}
