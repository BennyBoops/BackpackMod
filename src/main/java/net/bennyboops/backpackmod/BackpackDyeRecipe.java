package net.bennyboops.backpackmod;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class BackpackDyeRecipe extends CustomRecipe {
    private final Ingredient dye;
    private final Item result;

    public BackpackDyeRecipe(ResourceLocation id, CraftingBookCategory category, Ingredient dye, Item result) {
        super(id, category);
        this.dye = dye;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingContainer input, Level level) {
        ItemStack backpackStack = ItemStack.EMPTY;
        ItemStack dyeStack = ItemStack.EMPTY;
        int itemCount = 0;

        for (int i = 0; i < input.getContainerSize(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                itemCount++;
                if (BackpackMod.isBackpack(stack)) {
                    backpackStack = stack;
                } else if (this.dye.test(stack)) {
                    dyeStack = stack;
                }
            }
        }

        return itemCount == 2 && !backpackStack.isEmpty() && !dyeStack.isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registryAccess) {
        for (int i = 0; i < input.getContainerSize(); i++) {
            ItemStack stack = input.getItem(i);
            if (BackpackMod.isBackpack(stack)) {
                ItemStack resultStack = new ItemStack(this.result);
                // Copy the NBT from the old backpack to preserve contents and name
                if (stack.hasTag()) {
                    resultStack.setTag(stack.getTag().copy());
                }
                return resultStack;
            }
        }

        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BackpackMod.BACKPACK_DYE_RECIPE_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<BackpackDyeRecipe> {
        @Override
        public BackpackDyeRecipe fromJson(ResourceLocation id, JsonObject json) {
            CraftingBookCategory category = CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", null), CraftingBookCategory.MISC);
            Ingredient dye = Ingredient.fromJson(GsonHelper.getNonNull(json, "dye"));
            Item result = BuiltInRegistries.ITEM.get(new ResourceLocation(GsonHelper.getAsString(json, "result")));
            return new BackpackDyeRecipe(id, category, dye, result);
        }

        @Override
        public BackpackDyeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
            CraftingBookCategory category = buf.readEnum(CraftingBookCategory.class);
            Ingredient dye = Ingredient.fromNetwork(buf);
            Item result = BuiltInRegistries.ITEM.get(buf.readResourceLocation());
            return new BackpackDyeRecipe(id, category, dye, result);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buf, BackpackDyeRecipe recipe) {
            buf.writeEnum(recipe.category());
            recipe.dye.toNetwork(buf);
            buf.writeResourceLocation(BuiltInRegistries.ITEM.getKey(recipe.result));
        }
    }
}
