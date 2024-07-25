package com.benbenlaw.market.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.jetbrains.annotations.NotNull;

public record MarketRecipe(SizedIngredient input, ItemStack inputWithNbt, Ingredient license, ItemStack output, int variation) implements Recipe<RecipeInput> {

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.createWithCapacity(2);
     //   ingredients.add(input.getFirst().ingredient());
        return ingredients;
    }

    @Override
    public boolean matches(RecipeInput container, @NotNull Level level) {
        for (int i = 1; i <= 9; i++) {
            if (input.test(container.getItem(i)) || inputWithNbt.getItem() == container.getItem(i).getItem()) {
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean canCraftInDimensions(int pWidth, int pHeight) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.Provider provider) {
        return this.output.copy();
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull RecipeInput container, HolderLookup.@NotNull Provider provider) {
        return this.output.copy();
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return MarketRecipe.Serializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return MarketRecipe.Type.INSTANCE;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public static class Type implements RecipeType<MarketRecipe> {
        private Type() {}
        public static final MarketRecipe.Type INSTANCE = new MarketRecipe.Type();
    }

    public static class Serializer implements RecipeSerializer<MarketRecipe> {
        public static final MarketRecipe.Serializer INSTANCE = new MarketRecipe.Serializer();

        public final MapCodec<MarketRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        SizedIngredient.FLAT_CODEC.optionalFieldOf("input", SizedIngredient.of(Items.AIR, 1)).forGetter(MarketRecipe::input),
                        ItemStack.OPTIONAL_CODEC.optionalFieldOf("inputWithNbt", ItemStack.EMPTY).forGetter(MarketRecipe::inputWithNbt),
                        Ingredient.CODEC.fieldOf("license").forGetter(MarketRecipe::license),
                        ItemStack.CODEC.fieldOf("output").forGetter(MarketRecipe::output),
                        Codec.INT.fieldOf("variation").forGetter(MarketRecipe::variation)
                ).apply(instance, Serializer::createMarketRecipe));

        public static final StreamCodec<RegistryFriendlyByteBuf, MarketRecipe> STREAM_CODEC = StreamCodec.of(
                MarketRecipe.Serializer::write, MarketRecipe.Serializer::read);

        @Override
        public MapCodec<MarketRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MarketRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static MarketRecipe read(RegistryFriendlyByteBuf buffer) {
            SizedIngredient input = SizedIngredient.STREAM_CODEC.decode(buffer);
            ItemStack inputWithNbt = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            Ingredient license = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
            ItemStack output = ItemStack.STREAM_CODEC.decode(buffer);
            int variation = buffer.readInt();
            return new MarketRecipe(input, inputWithNbt, license, output, variation);
        }

        private static void write(RegistryFriendlyByteBuf buffer, MarketRecipe recipe) {
            SizedIngredient.STREAM_CODEC.encode(buffer, recipe.input);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, recipe.inputWithNbt);
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.license);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.output);
            buffer.writeInt(recipe.variation);
        }
        static MarketRecipe createMarketRecipe(SizedIngredient input, ItemStack inputWithNbt, Ingredient license, ItemStack output, int variation) {
            return new MarketRecipe(input, inputWithNbt, license, output, variation);
        }
    }
}
