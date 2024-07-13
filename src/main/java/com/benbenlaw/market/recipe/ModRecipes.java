package com.benbenlaw.market.recipe;

import com.benbenlaw.market.Market;
import com.benbenlaw.opolisutilities.OpolisUtilities;
import com.benbenlaw.opolisutilities.recipe.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZER =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Market.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Market.MOD_ID);


    //Market
    public static final Supplier<RecipeSerializer<MarketRecipe>> MARKET_SERIALIZER =
            SERIALIZER.register("market", () -> MarketRecipe.Serializer.INSTANCE);
    public static final Supplier<RecipeType<MarketRecipe>> MARKET_TYPE =
            TYPES.register("market", () -> MarketRecipe.Type.INSTANCE);


    public static void register(IEventBus eventBus) {
        SERIALIZER.register(eventBus);
        TYPES.register(eventBus);
    }




}
