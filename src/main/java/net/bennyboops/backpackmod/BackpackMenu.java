package net.bennyboops.backpackmod;

import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

public class BackpackMenu extends AbstractContainerMenu {
    public static final int SIZE = 27;

    private final Inventory playerInventory;
    private final SimpleContainer container;
    // Index in the player's inventory that holds the open backpack (hotbar 0-8, main 9-35, chest 38, offhand 40)
    private final int backpackSlot;
    // The backpack being edited. Only set on the server.
    private final ItemStack backpackStack;
    private final boolean serverSide;
    private boolean loaded = false;

    // Client side
    public BackpackMenu(int containerId, Inventory playerInventory, int backpackSlot) {
        this(containerId, playerInventory, backpackSlot, ItemStack.EMPTY, false);
    }

    // Server side
    public BackpackMenu(int containerId, Inventory playerInventory, int backpackSlot, ItemStack backpackStack) {
        this(containerId, playerInventory, backpackSlot, backpackStack, true);
    }

    private BackpackMenu(int containerId, Inventory playerInventory, int backpackSlot, ItemStack backpackStack, boolean serverSide) {
        super(BackpackMod.BACKPACK_MENU.get(), containerId);
        this.playerInventory = playerInventory;
        this.backpackSlot = backpackSlot;
        this.backpackStack = backpackStack;
        this.serverSide = serverSide;
        this.container = new SimpleContainer(SIZE) {
            @Override
            public void setChanged() {
                super.setChanged();
                saveToBackpack();
            }
        };

        if (!backpackStack.isEmpty()) {
            NonNullList<ItemStack> stacks = NonNullList.withSize(SIZE, ItemStack.EMPTY);
            backpackStack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(stacks);
            for (int i = 0; i < SIZE; i++) {
                this.container.setItem(i, stacks.get(i));
            }
        }
        this.loaded = true;

        container.startOpen(playerInventory.player);

        // Backpack inventory slots
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(container, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
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

    private Slot createPlayerSlot(Container inventory, int index, int x, int y) {
        if (index == this.backpackSlot) {
            return new LockedSlot(inventory, index, x, y);
        }
        return new Slot(inventory, index, x, y);
    }

    private boolean isBackpackStillHeld() {
        return this.backpackSlot >= 0
                && !this.backpackStack.isEmpty()
                && this.playerInventory.getItem(this.backpackSlot) == this.backpackStack;
    }

    // Write the contents to the backpack item right away so nothing is lost or duplicated
    private void saveToBackpack() {
        if (!this.loaded || !this.serverSide || !isBackpackStillHeld()) {
            return;
        }

        List<ItemStack> stacks = new ArrayList<>(SIZE);
        for (int i = 0; i < this.container.getContainerSize(); i++) {
            stacks.add(this.container.getItem(i));
        }
        this.backpackStack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(stacks));
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // The open backpack can't be moved, thrown or swapped while its screen is open
        if (slotId >= 0 && slotId < this.slots.size() && this.slots.get(slotId) instanceof LockedSlot) {
            return;
        }
        if (clickType == ClickType.SWAP && button == this.backpackSlot) {
            return;
        }
        if (this.serverSide && !isBackpackStillHeld()) {
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            stack = slotStack.copy();

            if (BackpackMod.isBackpack(slotStack)) {
                return ItemStack.EMPTY;
            }

            if (index < SIZE) {
                if (!this.moveItemStackTo(slotStack, SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(slotStack, 0, SIZE, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return stack;
    }

    @Override
    public boolean stillValid(Player player) {
        // Close the screen if the backpack is no longer where it was opened from
        return !this.serverSide || isBackpackStillHeld();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        saveToBackpack();
        this.container.stopOpen(player);
    }

    private static class LockedSlot extends Slot {
        public LockedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
