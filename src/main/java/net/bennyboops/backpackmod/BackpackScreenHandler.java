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
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.List;

public class BackpackScreenHandler extends ScreenHandler {
    public static final int SIZE = 27;

    private final PlayerInventory playerInventory;
    private final SimpleInventory inventory;
    // Index in the player's inventory that holds the open backpack (hotbar 0-8, main 9-35, chest 38, offhand 40)
    private final int backpackSlot;
    // The backpack being edited. Only set on the server.
    private final ItemStack backpackStack;
    private final boolean serverSide;
    private boolean loaded = false;

    // Client side
    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory, Integer backpackSlot) {
        this(syncId, playerInventory, backpackSlot, ItemStack.EMPTY, false);
    }

    // Server side
    public BackpackScreenHandler(int syncId, PlayerInventory playerInventory, int backpackSlot, ItemStack backpackStack) {
        this(syncId, playerInventory, backpackSlot, backpackStack, true);
    }

    private BackpackScreenHandler(int syncId, PlayerInventory playerInventory, int backpackSlot, ItemStack backpackStack, boolean serverSide) {
        super(BackpackMod.BACKPACK_SCREEN_HANDLER, syncId);
        this.playerInventory = playerInventory;
        this.backpackSlot = backpackSlot;
        this.backpackStack = backpackStack;
        this.serverSide = serverSide;
        this.inventory = new SimpleInventory(SIZE) {
            @Override
            public void markDirty() {
                super.markDirty();
                saveToBackpack();
            }
        };

        if (!backpackStack.isEmpty()) {
            DefaultedList<ItemStack> stacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
            backpackStack.getOrDefault(DataComponentTypes.CONTAINER, ContainerComponent.DEFAULT).copyTo(stacks);
            for (int i = 0; i < SIZE; i++) {
                this.inventory.setStack(i, stacks.get(i));
            }
        }
        this.loaded = true;

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
                this.addSlot(createPlayerSlot(playerInventory, col + row * 9 + 9, 8 + col * 18, 86 + row * 18 - 2));
            }
        }

        // Player hotbar slots
        for (int col = 0; col < 9; ++col) {
            this.addSlot(createPlayerSlot(playerInventory, col, 8 + col * 18, 144 - 2));
        }
    }

    private Slot createPlayerSlot(Inventory inventory, int index, int x, int y) {
        if (index == this.backpackSlot) {
            return new LockedSlot(inventory, index, x, y);
        }
        return new Slot(inventory, index, x, y);
    }

    private boolean isBackpackStillHeld() {
        return this.backpackSlot >= 0
                && !this.backpackStack.isEmpty()
                && this.playerInventory.getStack(this.backpackSlot) == this.backpackStack;
    }

    // Write the contents to the backpack item right away so nothing is lost or duplicated
    private void saveToBackpack() {
        if (!this.loaded || !this.serverSide || !isBackpackStillHeld()) {
            return;
        }

        List<ItemStack> stacks = new ArrayList<>(SIZE);
        for (int i = 0; i < this.inventory.size(); i++) {
            stacks.add(this.inventory.getStack(i));
        }
        this.backpackStack.set(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(stacks));
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // The open backpack can't be moved, thrown or swapped while its screen is open
        if (slotIndex >= 0 && slotIndex < this.slots.size() && this.slots.get(slotIndex) instanceof LockedSlot) {
            return;
        }
        if (actionType == SlotActionType.SWAP && button == this.backpackSlot) {
            return;
        }
        if (this.serverSide && !isBackpackStillHeld()) {
            return;
        }

        super.onSlotClick(slotIndex, button, actionType, player);
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

            if (index < SIZE) {
                if (!this.insertItem(slotStack, SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.insertItem(slotStack, 0, SIZE, false)) {
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
        // Close the screen if the backpack is no longer where it was opened from
        return !this.serverSide || isBackpackStillHeld();
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        saveToBackpack();
        this.inventory.onClose(player);
    }

    private static class LockedSlot extends Slot {
        public LockedSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return false;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }
    }
}
