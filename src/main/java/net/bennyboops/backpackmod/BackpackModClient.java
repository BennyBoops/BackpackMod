package net.bennyboops.backpackmod;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

public class BackpackModClient {

    public static final KeyMapping OPEN_BACKPACK_KEY = new KeyMapping(
            "key.backpackmod.open_backpack",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "category.backpackmod.backpack"
    );

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(BackpackModClient::registerScreens);
        modEventBus.addListener(BackpackModClient::registerKeys);
        modEventBus.addListener(BackpackModClient::addLayers);
        NeoForge.EVENT_BUS.addListener(BackpackModClient::onClientTick);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(BackpackMod.BACKPACK_MENU.get(), BackpackScreen::new);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_BACKPACK_KEY);
    }

    // Render worn backpacks on players and other humanoid mobs (armor stands, zombies, etc.)
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            EntityRenderer<?> renderer = event.getSkin(skin);
            if (renderer instanceof LivingEntityRenderer livingRenderer) {
                livingRenderer.addLayer(new BackpackLayer(livingRenderer));
            }
        }

        for (EntityType<?> type : event.getEntityTypes()) {
            EntityRenderer<?> renderer = event.getRenderer(type);
            if (renderer instanceof LivingEntityRenderer livingRenderer && livingRenderer.getModel() instanceof HumanoidModel) {
                livingRenderer.addLayer(new BackpackLayer(livingRenderer));
            }
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        while (OPEN_BACKPACK_KEY.consumeClick()) {
            if (client.player != null) {
                if (client.screen instanceof BackpackScreen) {
                    client.setScreen(null);
                } else if (client.getConnection() != null) {
                    ItemStack chestSlot = client.player.getInventory().getItem(BackpackMod.CHEST_SLOT);
                    if (BackpackMod.isBackpack(chestSlot)) {
                        PacketDistributor.sendToServer(new OpenBackpackPayload());
                    }
                }
            }
        }
    }
}
