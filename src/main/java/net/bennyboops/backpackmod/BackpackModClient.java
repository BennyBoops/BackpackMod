package net.bennyboops.backpackmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.glfw.GLFW;

public class BackpackModClient implements ClientModInitializer {

    public static KeyBinding OPEN_BACKPACK_KEY;

    @Override
    public void onInitializeClient() {
        HandledScreens.register(
                BackpackMod.BACKPACK_SCREEN_HANDLER,
                BackpackScreen::new
        );

        for (BackpackColor color : BackpackColor.values()) {
            ArmorRenderer.register((matrices, vertexConsumers, stack, entity, slot, light, contextModel) -> {
                if (slot == EquipmentSlot.CHEST) {
                    matrices.push();

                    contextModel.body.rotate(matrices);
                    matrices.translate(0.0, 0.3, 0.2);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
                    matrices.scale(1.0f, 1.0f, 1.0f);

                    ItemRenderer itemRenderer = net.minecraft.client.MinecraftClient.getInstance().getItemRenderer();
                    itemRenderer.renderItem(stack, ModelTransformationMode.FIXED, light, 0, matrices, vertexConsumers, entity.getWorld(), 0);

                    matrices.pop();
                }
            }, BackpackMod.BACKPACK_ITEMS.get(color));
        }

        OPEN_BACKPACK_KEY = new KeyBinding(
                "key.backpackmod.open_backpack",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                "category.backpackmod.backpack"
        );
        KeyBindingHelper.registerKeyBinding(OPEN_BACKPACK_KEY);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_BACKPACK_KEY.wasPressed()) {
                if (client.player != null) {
                    if (client.currentScreen instanceof BackpackScreen) {
                        client.setScreen(null);
                    } else if (client.getNetworkHandler() != null) {
                        ItemStack chestSlot = client.player.getInventory().getArmorStack(2);
                        if (BackpackMod.isBackpack(chestSlot)) {
                            client.getNetworkHandler().sendPacket(new CustomPayloadC2SPacket(new OpenBackpackPayload()));
                        }
                    }
                }
            }
        });
    }
}