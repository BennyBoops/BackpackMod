package net.bennyboops.backpackmod;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class BackpackScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final ItemStack backpackStack;

    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, ItemStack.EMPTY);
    }

    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory, ItemStack backpackStack) {
        super(BackpackMod.BACKPACK_SCREEN_HANDLER, syncId);
        this.backpackStack = backpackStack;
        this.inventory = new SimpleInventory(27);

        if (!backpackStack.isEmpty()) {
            ContainerComponent container = backpackStack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT);
            for (int i = 0; i < Math.min(container.stream().count(), this.inventory.size()); i++) {
                this.inventory.setStack(i, container.stream().toList().get(i));
            }
        }

        inventory.onOpen(playerInventory.player);

        // Backpack inventory slots
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean canInsert(ItemStack stack) {
                        return !BackpackMod.isBackpack(stack);
                    }
                });
            }
        }

        // Player inventory slots
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 86 + row * 18 - 2));
            }
        }

        // Player hotbar slots
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 144 - 2));
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasStack()) {
            ItemStack slotStack = slot.getStack();
            stack = slotStack.copy();

            if (BackpackMod.isBackpack(slotStack)) {
                return ItemStack.EMPTY;
            }

            if (index < 27) {
                if (!this.insertItem(slotStack, 27, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.insertItem(slotStack, 0, 27, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return stack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);

        if (!backpackStack.isEmpty() && !player.getWorld().isClient) {
            java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
            for (int i = 0; i < this.inventory.size(); i++) {
                stacks.add(this.inventory.getStack(i));
            }
            backpackStack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(stacks));
        }

        this.inventory.onClose(player);
    }
}