package net.bennyboops.backpackmod;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class BackpackDyeRecipe extends CustomRecipe {
    private final Ingredient dye;
    private final Item result;

    public BackpackDyeRecipe(CraftingBookCategory category, Ingredient dye, Item result) {
        super(category);
        this.dye = dye;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack backpackStack = ItemStack.EMPTY;
        ItemStack dyeStack = ItemStack.EMPTY;
        int itemCount = 0;

        for (int i = 0; i < input.size(); i++) {
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
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (BackpackMod.isBackpack(stack)) {
                // Keep all components from the old backpack to preserve contents
                return stack.transmuteCopy(this.result, 1);
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
        public static final MapCodec<BackpackDyeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(CustomRecipe::category),
                        Ingredient.CODEC_NONEMPTY.fieldOf("dye").forGetter(recipe -> recipe.dye),
                        BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(recipe -> recipe.result)
                ).apply(instance, BackpackDyeRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, BackpackDyeRecipe> STREAM_CODEC =
                StreamCodec.composite(
                        CraftingBookCategory.STREAM_CODEC,
                        CustomRecipe::category,
                        Ingredient.CONTENTS_STREAM_CODEC,
                        recipe -> recipe.dye,
                        ByteBufCodecs.registry(Registries.ITEM),
                        recipe -> recipe.result,
                        BackpackDyeRecipe::new
                );

        @Override
        public MapCodec<BackpackDyeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BackpackDyeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
