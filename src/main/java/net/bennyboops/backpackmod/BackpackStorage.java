package net.bennyboops.backpackmod;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;

// Reads and writes backpack contents stored in the item's NBT
public final class BackpackStorage {
    public static final int SIZE = 27;
    private static final String ITEMS_KEY = "Items";

    private BackpackStorage() {
    }

    public static NonNullList<ItemStack> load(ItemStack backpack) {
        NonNullList<ItemStack> stacks = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        CompoundTag tag = backpack.getTag();
        if (tag != null && tag.contains(ITEMS_KEY)) {
            ContainerHelper.loadAllItems(tag, stacks);
        }
        return stacks;
    }

    public static void save(ItemStack backpack, NonNullList<ItemStack> stacks) {
        if (stacks.stream().allMatch(ItemStack::isEmpty)) {
            CompoundTag tag = backpack.getTag();
            if (tag != null) {
                tag.remove(ITEMS_KEY);
                if (tag.isEmpty()) {
                    backpack.setTag(null);
                }
            }
            return;
        }

        CompoundTag tag = backpack.getOrCreateTag();
        tag.remove(ITEMS_KEY);
        ContainerHelper.saveAllItems(tag, stacks, false);
    }
}
