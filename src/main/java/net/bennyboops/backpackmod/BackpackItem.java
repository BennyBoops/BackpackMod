package net.bennyboops.backpackmod;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class BackpackItem extends Item implements Equipment {
    private final BackpackColor color;

    public BackpackItem(Settings settings, BackpackColor color) {
        super(settings);
        this.color = color;
    }

    public BackpackColor getColor() {
        return color;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        if (!world.isClient) {
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.ITEM_BUNDLE_INSERT, SoundCategory.PLAYERS, 1.00F, 0.75F);

            player.openHandledScreen(createScreenHandlerFactory(stack));
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);

        ContainerComponent container = stack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);

        if (!container.stream().allMatch(ItemStack::isEmpty)) {
            int occupied = 0;

            for (ItemStack item : container.stream().toList()) {
                if (!item.isEmpty()) {
                    occupied++;
                }
            }

            int shown = 0;
            for (ItemStack item : container.stream().toList()) {
                if (!item.isEmpty() && shown < 4) {
                    Text itemText = Text.literal("  ")
                            .append(item.getName())
                            .append(" x" + item.getCount())
                            .formatted(Formatting.GRAY);
                    tooltip.add(itemText);
                    shown++;
                }
            }

            if (occupied > 4) {
                tooltip.add(Text.translatable("container.shulkerBox.more", occupied - 4).formatted(Formatting.ITALIC, Formatting.GRAY));
            }
        }
    }

    @Override
    public boolean canBeNested() {
        return false;
    }

    @Override
    public EquipmentSlot getSlotType() {
        return EquipmentSlot.CHEST;
    }

    @Override
    public RegistryEntry<SoundEvent> getEquipSound() {
        return SoundEvents.ITEM_ARMOR_EQUIP_LEATHER;
    }

    private NamedScreenHandlerFactory createScreenHandlerFactory(ItemStack stack) {
        return new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, player) ->
                        new BackpackScreenHandler(syncId, playerInventory, stack),
                getDisplayName(stack)
        );
    }

    public static Text getDisplayName(ItemStack stack) {
        if (stack.contains(DataComponentTypes.CUSTOM_NAME)) {
            return stack.getName();
        }

        if (stack.getItem() instanceof BackpackItem backpackItem) {
            return Text.translatable("container.backpackmod.backpack." + backpackItem.getColor().getName());
        }

        // Fallback
        return Text.translatable("container.backpackmod.backpack");
    }
}