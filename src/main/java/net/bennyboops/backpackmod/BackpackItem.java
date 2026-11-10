package net.bennyboops.backpackmod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

import java.util.List;

public class BackpackItem extends Item implements Equipable {
    private final BackpackColor color;

    public BackpackItem(Properties properties, BackpackColor color) {
        super(properties);
        this.color = color;
    }

    public BackpackColor getColor() {
        return color;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 1.00F, 0.75F);

            int slot = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : Inventory.SLOT_OFFHAND;
            BackpackMod.openBackpack(player, slot);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        int occupied = 0;
        int shown = 0;

        for (ItemStack item : contents.nonEmptyItems()) {
            occupied++;
            if (shown < 4) {
                tooltip.add(Component.literal("  ")
                        .append(item.getHoverName())
                        .append(" x" + item.getCount())
                        .withStyle(ChatFormatting.GRAY));
                shown++;
            }
        }

        if (occupied > 4) {
            tooltip.add(Component.translatable("container.shulkerBox.more", occupied - 4).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
        }
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return false;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.CHEST;
    }

    @Override
    public Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }

    public static Component getDisplayName(ItemStack stack) {
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            return stack.getHoverName();
        }

        if (stack.getItem() instanceof BackpackItem backpackItem) {
            return Component.translatable("container.backpackmod.backpack." + backpackItem.getColor().getName());
        }

        // Fallback
        return Component.translatable("container.backpackmod.backpack");
    }
}
