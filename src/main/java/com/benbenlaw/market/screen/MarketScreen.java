package com.benbenlaw.market.screen;

import com.benbenlaw.market.Market;
import com.benbenlaw.market.recipe.MarketRecipe;
import com.benbenlaw.opolisutilities.util.MouseUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;

import java.util.Arrays;
import java.util.Optional;

public class MarketScreen extends AbstractContainerScreen<MarketMenu> {

    Level level;
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Market.MOD_ID, "textures/gui/market_gui.png");

    public MarketScreen(MarketMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);
        this.level = menu.level;
    }

    @Override
    protected void init() {
        super.init();
    }


    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        renderProgressBars(guiGraphics);  // Draw progress bars over the item stacks

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        renderProgressBars(guiGraphics);
        renderTooltip(guiGraphics, mouseX, mouseY);
        renderCooldown(guiGraphics, mouseX, mouseY, x, y);
        renderCurrentRecipeInformationAboveArea(guiGraphics, mouseX, mouseY, x, y);

    }

    private void renderCooldown(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        Optional<RecipeHolder<?>> recipe = this.level.getRecipeManager().byKey(this.menu.blockEntity.recipeID);

        if (recipe.isPresent()) {
            MarketRecipe r = (MarketRecipe) recipe.get().value();

            //Render Input

            ItemStack itemInput;

            if (r.inputWithNbt().getItem() != ItemStack.EMPTY.getItem()) {
                itemInput = new ItemStack (r.inputWithNbt().getItem(), r.inputWithNbt().getCount() + this.menu.blockEntity.orderVariation);
                itemInput.applyComponents(r.inputWithNbt().getComponents());
            } else {
                itemInput = new ItemStack (r.input().getItems()[0].getItem(), r.input().count() + this.menu.blockEntity.orderVariation);
            }



            guiGraphics.renderItemDecorations(this.font, itemInput, x + 76, y + 16);
            guiGraphics.renderFakeItem(itemInput, x + 76, y + 15);

            //Render Output
            ItemStack itemOutput = new ItemStack (r.output().getItem(), r.output().getCount());
            guiGraphics.renderItemDecorations(this.font, itemOutput, x + 125, y + 16);
            guiGraphics.renderFakeItem(itemOutput, x + 125, y + 16);



        }
    }

    private void renderCurrentRecipeInformationAboveArea(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {

        if (MouseUtil.isMouseAboveArea(mouseX, mouseY, x, y, 75, 15, 67, 16)) {
            guiGraphics.renderTooltip(this.font, Component.literal("Current Demand"), mouseX, mouseY);

        }

    }



    private void renderProgressBars(GuiGraphics guiGraphics) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x + 122 , y + 35 , 176, 30, menu.getScaledProgress(), 16);
    }
}
