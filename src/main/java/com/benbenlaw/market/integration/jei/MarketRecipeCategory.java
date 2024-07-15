package com.benbenlaw.market.integration.jei;

import com.benbenlaw.market.Market;
import com.benbenlaw.market.block.ModBlocks;
import com.benbenlaw.market.recipe.MarketRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class MarketRecipeCategory implements IRecipeCategory<MarketRecipe>{

    public final static ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Market.MOD_ID, "market");
    public final static ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Market.MOD_ID, "textures/gui/jei_market.png");

    static final RecipeType<MarketRecipe> RECIPE_TYPE = RecipeType.create(Market.MOD_ID, "market",
            MarketRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public MarketRecipeCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 0, 0, 103, 19);

        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.MARKET.get()));
    }

    public ResourceLocation getUid () {
        return UID;
    }

    @Override
    public RecipeType<MarketRecipe> getRecipeType () {
        return JEIMarketPlugin.MARKET_RECIPE;
    }

    @Override
    public Component getTitle () {
        return Component.literal("Market");
    }

    @Override
    public IDrawable getBackground () {
        return this.background;
    }

    @Nullable
    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MarketRecipe recipe, IFocusGroup focusGroup) {

        builder.addSlot(RecipeIngredientRole.CATALYST, 4, 2 ).addIngredients(recipe.license());
        builder.addSlot(RecipeIngredientRole.INPUT, 40, 2).addIngredients(VanillaTypes.ITEM_STACK, Arrays.asList(recipe.input().getItems()))
                .addTooltipCallback((variation, addTooltip) -> addTooltip.add(Component.literal("Variation: +/- " + recipe.variation()).withStyle(ChatFormatting.GREEN)));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 84, 2).addItemStack(recipe.output());



    }
}
