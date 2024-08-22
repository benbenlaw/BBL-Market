package com.benbenlaw.market.screen;

import com.benbenlaw.market.Market;
import com.benbenlaw.market.recipe.MarketRecipe;
import com.benbenlaw.opolisutilities.util.MouseUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.crafting.SizedIngredient;
import org.antlr.v4.runtime.atn.SemanticContext;

import java.util.Arrays;
import java.util.List;
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
        renderOrder(guiGraphics, mouseX, mouseY, x, y);
        renderCurrentRecipeInformationAboveArea(guiGraphics, mouseX, mouseY, x, y);

    }

    private int currentItemIndex = 0;
    private long lastCycleTime = 0;
    private static final long CYCLE_INTERVAL = 1000; // Time in milliseconds between cycles

    private void renderOrder(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        Optional<RecipeHolder<?>> recipe = this.level.getRecipeManager().byKey(this.menu.blockEntity.recipeID);

        if (recipe.isPresent()) {
            MarketRecipe r = (MarketRecipe) recipe.get().value();

            List<ItemStack> validInputs = Arrays.asList(r.input().getItems());

            // Check if validInputs is not empty before proceeding
            if (validInputs.isEmpty()) {
                return; // No items to render
            }

            // Cycle to the next item if enough time has passed
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastCycleTime > CYCLE_INTERVAL) {
                currentItemIndex = (currentItemIndex + 1) % validInputs.size();
                lastCycleTime = currentTime;
            }

            // Ensure currentItemIndex is within bounds
            currentItemIndex = Math.max(0, Math.min(currentItemIndex, validInputs.size() - 1));

            // Get the current item to display
            ItemStack validInput = validInputs.get(currentItemIndex);
            ItemStack itemInput;

            int inputCount = (int) ((validInput.getCount() + this.menu.blockEntity.orderVariation) / this.menu.blockEntity.demand);
            if (inputCount > validInput.getMaxStackSize()) {
                inputCount = validInput.getMaxStackSize();
            }

            if (!r.inputWithNbt().isEmpty()) {
                itemInput = new ItemStack(validInput.getItem(), inputCount);
                itemInput.applyComponents(validInput.getComponents());
            } else {
                itemInput = new ItemStack(validInput.getItem(), inputCount);
            }

            // Render the currently selected input item
            guiGraphics.renderItemDecorations(this.font, itemInput, x + 76, y + 16);
            guiGraphics.renderItem(itemInput, x + 76, y + 16);

            if (MouseUtil.isMouseAboveArea(mouseX, mouseY, x, y, 76, 15, 16, 16)) {
                String tagName = "";
                SizedIngredient ingredient = r.input();
                boolean usesTags = ingredient.getItems().length > 1;

                for (ItemStack stack : ingredient.getItems()) {
                    if (stack.getItem().getDefaultInstance().getTags().iterator().hasNext()) {
                        tagName = stack.getItem().getDefaultInstance().getTags().iterator().next().location().toString();
                        break;
                    }
                }

                // Get the default tooltip lines
                List<Component> tooltipComponents = itemInput.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.ADVANCED);

                // Add the tag name to the tooltip if available
                if (!tagName.isEmpty() && usesTags) {
                    tooltipComponents.add(Component.literal("Required Tag: " + tagName));
                }

                // Render the tooltip with the modified list of components
                guiGraphics.renderTooltip(this.font, tooltipComponents, Optional.empty(), itemInput, mouseX, mouseY);
            }

            // Render Output
            ItemStack itemOutput = new ItemStack(r.output().getItem(), r.output().getCount());
            guiGraphics.renderItemDecorations(this.font, itemOutput, x + 125, y + 16);
            guiGraphics.renderItem(itemOutput, x + 125, y + 16);

            if (MouseUtil.isMouseAboveArea(mouseX, mouseY, x, y, 125, 15, 16, 16)) {
                guiGraphics.renderTooltip(this.font, itemOutput, mouseX, mouseY);
            }

            String demandToShow = String.valueOf(this.menu.blockEntity.demand);
            if (this.menu.blockEntity.demand > 0.2 && this.menu.blockEntity.demand < 0.5) {
                if (this.menu.blockEntity.demand > 0.3) {
                    demandToShow = "0.4";
                } else {
                    demandToShow = "0.3";
                }
            }

            guiGraphics.drawString(this.font, "Demand: " + demandToShow, x + 69, y + 56, 0x3F3F3F, false);
        }
    }



    private void renderCurrentRecipeInformationAboveArea(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {

        if (MouseUtil.isMouseAboveArea(mouseX, mouseY, x, y, 98, 15, 16, 16)) {
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
