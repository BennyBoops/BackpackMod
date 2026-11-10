package net.bennyboops.backpackmod;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Equipment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

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

            int slot = hand == Hand.MAIN_HAND ? player.getInventory().selectedSlot : PlayerInventory.OFF_HAND_SLOT;
            BackpackMod.openBackpack(player, slot);
        }

        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        int occupied = 0;
        int shown = 0;

        for (ItemStack item : BackpackStorage.load(stack)) {
            if (!item.isEmpty()) {
                occupied++;
                if (shown < 4) {
                    tooltip.add(Text.literal("  ")
                            .append(item.getName())
                            .append(" x" + item.getCount())
                            .formatted(Formatting.GRAY));
                    shown++;
                }
            }
        }

        if (occupied > 4) {
            tooltip.add(Text.translatable("container.shulkerBox.more", occupied - 4).formatted(Formatting.ITALIC, Formatting.GRAY));
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
    public SoundEvent getEquipSound() {
        return SoundEvents.ITEM_ARMOR_EQUIP_LEATHER;
    }

    public static Text getDisplayName(ItemStack stack) {
        if (stack.hasCustomName()) {
            return stack.getName();
        }

        if (stack.getItem() instanceof BackpackItem backpackItem) {
            return Text.translatable("container.backpackmod.backpack." + backpackItem.getColor().getName());
        }

        // Fallback
        return Text.translatable("container.backpackmod.backpack");
    }
}
