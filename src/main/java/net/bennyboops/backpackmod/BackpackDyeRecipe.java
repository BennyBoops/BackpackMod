package net.bennyboops.backpackmod;

import com.google.gson.JsonObject;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.world.World;

public class BackpackDyeRecipe extends SpecialCraftingRecipe {
    private final Ingredient dye;
    private final Item result;

    public BackpackDyeRecipe(Identifier id, CraftingRecipeCategory category, Ingredient dye, Item result) {
        super(id, category);
        this.dye = dye;
        this.result = result;
    }

    @Override
    public boolean matches(RecipeInputInventory input, World world) {
        ItemStack backpackStack = ItemStack.EMPTY;
        ItemStack dyeStack = ItemStack.EMPTY;
        int itemCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getStack(i);
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
    public ItemStack craft(RecipeInputInventory input, DynamicRegistryManager registryManager) {
        ItemStack backpackStack = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getStack(i);
            if (BackpackMod.isBackpack(stack)) {
                backpackStack = stack;
                break;
            }
        }

        if (backpackStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack resultStack = new ItemStack(this.result);

        // Copy the NBT from the old backpack to preserve contents and name
        if (backpackStack.hasNbt()) {
            resultStack.setNbt(backpackStack.getNbt().copy());
        }

        return resultStack;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return BackpackMod.BACKPACK_DYE_RECIPE_SERIALIZER;
    }

    public static class Serializer implements RecipeSerializer<BackpackDyeRecipe> {
        @Override
        public BackpackDyeRecipe read(Identifier id, JsonObject json) {
            CraftingRecipeCategory category = CraftingRecipeCategory.CODEC.byId(JsonHelper.getString(json, "category", null), CraftingRecipeCategory.MISC);
            Ingredient dye = Ingredient.fromJson(JsonHelper.getElement(json, "dye"));
            Item result = Registries.ITEM.get(new Identifier(JsonHelper.getString(json, "result")));
            return new BackpackDyeRecipe(id, category, dye, result);
        }

        @Override
        public BackpackDyeRecipe read(Identifier id, PacketByteBuf buf) {
            CraftingRecipeCategory category = buf.readEnumConstant(CraftingRecipeCategory.class);
            Ingredient dye = Ingredient.fromPacket(buf);
            Item result = Registries.ITEM.get(buf.readIdentifier());
            return new BackpackDyeRecipe(id, category, dye, result);
        }

        @Override
        public void write(PacketByteBuf buf, BackpackDyeRecipe recipe) {
            buf.writeEnumConstant(recipe.getCategory());
            recipe.dye.write(buf);
            buf.writeIdentifier(Registries.ITEM.getId(recipe.result));
        }
    }
}
