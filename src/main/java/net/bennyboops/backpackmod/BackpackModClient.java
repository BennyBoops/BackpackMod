package net.bennyboops.backpackmod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

public class BackpackModClient {

    public static final KeyMapping OPEN_BACKPACK_KEY = new KeyMapping(
            "key.backpackmod.open_backpack",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "category.backpackmod.backpack"
    );

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(BackpackModClient::clientSetup);
        modEventBus.addListener(BackpackModClient::registerKeys);
        modEventBus.addListener(BackpackModClient::addLayers);
        MinecraftForge.EVENT_BUS.addListener(BackpackModClient::onClientTick);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(BackpackMod.BACKPACK_MENU.get(), BackpackScreen::new));
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_BACKPACK_KEY);
    }

    // Render worn backpacks on players and other humanoid mobs (armor stands, zombies, etc.)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            EntityRenderer<?> renderer = event.getSkin(skin);
            if (renderer instanceof LivingEntityRenderer livingRenderer) {
                livingRenderer.addLayer(new BackpackLayer(livingRenderer));
            }
        }

        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            EntityRenderer<?> renderer = event.getRenderer((EntityType) type);
            if (renderer instanceof LivingEntityRenderer livingRenderer && livingRenderer.getModel() instanceof HumanoidModel) {
                livingRenderer.addLayer(new BackpackLayer(livingRenderer));
            }
        }
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        while (OPEN_BACKPACK_KEY.consumeClick()) {
            if (client.player != null) {
                if (client.screen instanceof BackpackScreen) {
                    client.setScreen(null);
                } else if (client.getConnection() != null) {
                    ItemStack chestSlot = client.player.getInventory().getItem(BackpackMod.CHEST_SLOT);
                    if (BackpackMod.isBackpack(chestSlot)) {
                        BackpackMod.CHANNEL.sendToServer(new OpenBackpackPacket());
                    }
                }
            }
        }
    }
}
