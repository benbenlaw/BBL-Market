package com.benbenlaw.market.integration.jei;

import com.benbenlaw.market.Market;
import com.benbenlaw.market.block.ModBlocks;
import com.benbenlaw.market.recipe.MarketRecipe;
import com.benbenlaw.opolisutilities.integration.jei.OpolisIRecipeSlotTooltipCallback;
import com.benbenlaw.opolisutilities.recipe.CatalogueRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
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
        this.background = helper.createDrawable(TEXTURE, 0, 0, 103, 28);

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
    public @Nullable ResourceLocation getRegistryName(MarketRecipe recipe) {
        assert Minecraft.getInstance().level != null;
        return Minecraft.getInstance().level.getRecipeManager().getAllRecipesFor(MarketRecipe.Type.INSTANCE).stream()
                .filter(recipeHolder -> recipeHolder.value().equals(recipe))
                .map(RecipeHolder::id)
                .findFirst()
                .orElse(null);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MarketRecipe recipe, IFocusGroup focusGroup) {

        builder.addSlot(RecipeIngredientRole.CATALYST, 4, 2 ).addIngredients(recipe.license());

        if (recipe.inputWithNbt().getItem() != ItemStack.EMPTY.getItem()) {

            ItemStack itemStack = new ItemStack(recipe.inputWithNbt().getItem(), recipe.inputWithNbt().getCount());
            DataComponentMap components = recipe.inputWithNbt().getComponents();

            itemStack.applyComponents(components);

            builder.addSlot(RecipeIngredientRole.INPUT, 40, 2).addItemStack(itemStack);

        } else {
            builder.addSlot(RecipeIngredientRole.INPUT, 40, 2).addIngredients(VanillaTypes.ITEM_STACK, Arrays.asList(recipe.input().getItems()));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 84, 2).addItemStack(recipe.output());
    }

    @Override
    public void draw(MarketRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        final Minecraft minecraft = Minecraft.getInstance();
        guiGraphics.drawString(minecraft.font.self(), Component.literal("Variation: +/- " + recipe.variation()), 2, 20, Color.GRAY.getRGB(), false);

    }
}
