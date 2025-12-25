package net.bennyboops.backpackmod;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.recipe.input.CraftingRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.World;

public class BackpackDyeRecipe extends SpecialCraftingRecipe {
    private final Ingredient dye;
    private final Item result;

    public BackpackDyeRecipe(CraftingRecipeCategory category, Ingredient dye, Item result) {
        super(category);
        this.dye = dye;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        ItemStack backpackStack = ItemStack.EMPTY;
        ItemStack dyeStack = ItemStack.EMPTY;
        int itemCount = 0;

        for (int i = 0; i < input.getSize(); i++) {
            ItemStack stack = input.getStackInSlot(i);
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
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        ItemStack backpackStack = ItemStack.EMPTY;

        for (int i = 0; i < input.getSize(); i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (BackpackMod.isBackpack(stack)) {
                backpackStack = stack;
                break;
            }
        }

        if (backpackStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack resultStack = new ItemStack(this.result);

        // Copy all components from the old backpack to preserve contents
        resultStack.applyComponentsFrom(backpackStack.getComponents());

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
        public static final MapCodec<BackpackDyeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        CraftingRecipeCategory.CODEC.fieldOf("category").orElse(CraftingRecipeCategory.MISC).forGetter(recipe -> CraftingRecipeCategory.MISC),
                        Ingredient.DISALLOW_EMPTY_CODEC.fieldOf("dye").forGetter(recipe -> recipe.dye),
                        Registries.ITEM.getCodec().fieldOf("result").forGetter(recipe -> recipe.result)
                ).apply(instance, BackpackDyeRecipe::new)
        );

        public static final PacketCodec<RegistryByteBuf, BackpackDyeRecipe> PACKET_CODEC =
                PacketCodec.tuple(
                        CraftingRecipeCategory.PACKET_CODEC,
                        recipe -> CraftingRecipeCategory.MISC,
                        Ingredient.PACKET_CODEC,
                        recipe -> recipe.dye,
                        PacketCodecs.registryEntry(Registries.ITEM.getKey()),
                        recipe -> Registries.ITEM.getEntry(recipe.result),
                        (category, dye, result) -> new BackpackDyeRecipe(category, dye, result.value())
                );

        @Override
        public MapCodec<BackpackDyeRecipe> codec() {
            return CODEC;
        }

        @Override
        public PacketCodec<RegistryByteBuf, BackpackDyeRecipe> packetCodec() {
            return PACKET_CODEC;
        }
    }
}