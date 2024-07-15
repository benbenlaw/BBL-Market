package com.benbenlaw.market.integration.jei;

import com.benbenlaw.market.Market;
import com.benbenlaw.market.block.ModBlocks;
import com.benbenlaw.market.recipe.MarketRecipe;
import com.benbenlaw.market.recipe.ModRecipes;
import com.benbenlaw.opolisutilities.integration.jei.CatalogueRecipeCategory;
import com.benbenlaw.opolisutilities.integration.jei.DryingTableRecipeCategory;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

@JeiPlugin
public class JEIMarketPlugin implements IModPlugin {

    public static IDrawableStatic slotDrawable;

    public static RecipeType<MarketRecipe> MARKET_RECIPE =
            new RecipeType<>(MarketRecipeCategory.UID, MarketRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Market.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MARKET.get()), MarketRecipeCategory.RECIPE_TYPE);
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {

        IGuiHelper guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(new
                MarketRecipeCategory(registration.getJeiHelpers().getGuiHelper()));

        slotDrawable = guiHelper.getSlotDrawable();

    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        assert Minecraft.getInstance().level != null;
        final var recipeManager = Minecraft.getInstance().level.getRecipeManager();

        registration.addRecipes(MarketRecipeCategory.RECIPE_TYPE,
                recipeManager.getAllRecipesFor(ModRecipes.MARKET_TYPE.get()).stream().map(RecipeHolder::value).toList());

    }
}
