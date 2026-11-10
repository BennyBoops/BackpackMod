package net.bennyboops.backpackmod;

import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;

// Reads and writes backpack contents stored in the item's NBT
public final class BackpackStorage {
    public static final int SIZE = 27;
    private static final String ITEMS_KEY = "Items";

    private BackpackStorage() {
    }

    public static DefaultedList<ItemStack> load(ItemStack backpack) {
        DefaultedList<ItemStack> stacks = DefaultedList.ofSize(SIZE, ItemStack.EMPTY);
        NbtCompound nbt = backpack.getNbt();
        if (nbt != null && nbt.contains(ITEMS_KEY)) {
            Inventories.readNbt(nbt, stacks);
        }
        return stacks;
    }

    public static void save(ItemStack backpack, DefaultedList<ItemStack> stacks) {
        if (stacks.stream().allMatch(ItemStack::isEmpty)) {
            NbtCompound nbt = backpack.getNbt();
            if (nbt != null) {
                nbt.remove(ITEMS_KEY);
                if (nbt.isEmpty()) {
                    backpack.setNbt(null);
                }
            }
            return;
        }

        NbtCompound nbt = backpack.getOrCreateNbt();
        nbt.remove(ITEMS_KEY);
        Inventories.writeNbt(nbt, stacks, false);
    }
}
