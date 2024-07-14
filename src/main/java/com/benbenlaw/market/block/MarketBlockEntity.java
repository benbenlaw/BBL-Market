package com.benbenlaw.market.block;

import com.benbenlaw.market.recipe.MarketRecipe;
import com.benbenlaw.market.screen.MarketMenu;
import com.benbenlaw.opolisutilities.block.entity.custom.handler.InputOutputItemHandler;
import com.benbenlaw.opolisutilities.util.inventory.IInventoryHandlingBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MarketBlockEntity extends BlockEntity implements MenuProvider, IInventoryHandlingBlockEntity {
    private final ItemStackHandler itemHandler = new ItemStackHandler(13) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            sync();
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            if(slot == LICENCE_SLOT) {
                return 1;
            }
            return super.getStackLimit(slot, stack);
        }
    };

    public void sync() {
        if (level instanceof ServerLevel serverLevel) {
            LevelChunk chunk = serverLevel.getChunkAt(getBlockPos());
            if (Objects.requireNonNull(chunk.getLevel()).getChunkSource() instanceof ServerChunkCache chunkCache) {
                chunkCache.chunkMap.getPlayers(chunk.getPos(), false).forEach(this::syncContents);
            }
        }
    }

    public void syncContents(ServerPlayer player) {
        player.connection.send(Objects.requireNonNull(getUpdatePacket()));
    }

    public final ContainerData data;
    public int progress = 0;
    public int maxProgress = 220;

    public MarketRecipe currentRecipe;
    public int orderVariation;
    public int orderTimeRemaining;
    public boolean needNewRecipe = true;
    public boolean onCooldown = false;
    public int cooldownTimer;

    public int LICENCE_SLOT = 0;
    // INPUT_SLOTS= [1,2,3,4,5,6,7,8,9];
    public int OUTPUT_SLOT_1 = 10;
    public int OUTPUT_SLOT_2 = 11;
    public int OUTPUT_SLOT_3 = 12;

    private final IItemHandler marketItemHandler = new InputOutputItemHandler(itemHandler,
            (i, stack) -> i >= 1 && i <= 9,
            i -> i >= 10 && i <= 12
    );

    public @Nullable IItemHandler getItemHandlerCapability(@Nullable Direction side) {
        return marketItemHandler;
    }

    public void setHandler(ItemStackHandler handler) {
        for (int i = 0; i < handler.getSlots(); i++) {
            this.itemHandler.setStackInSlot(i, handler.getStackInSlot(i));
        }
    }

    public ItemStackHandler getItemStackHandler() {
        return this.itemHandler;
    }

    public MarketBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MARKET_BLOCK_ENTITY.get(), pos, state);
        this.data = new ContainerData() {
            public int get(int index) {
                return switch (index) {
                    case 0 -> MarketBlockEntity.this.progress;
                    case 1 -> MarketBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            public void set(int index, int value) {
                switch (index) {
                    case 0 -> MarketBlockEntity.this.progress = value;
                    case 1 -> MarketBlockEntity.this.maxProgress = value;
                }
            }

            public int getCount() {
                return 2;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.market.market");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int container, @NotNull Inventory inventory, @NotNull Player player) {
        return new MarketMenu(container, inventory, this.getBlockPos(), data);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.setChanged();
    }
    @Override
    public void handleUpdateTag(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider provider) {
        super.loadAdditional(compoundTag, provider);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider provider) {
        CompoundTag compoundTag = new CompoundTag();
        saveAdditional(compoundTag, provider);
        return compoundTag;
    }

    @Override
    public void onDataPacket(@NotNull Connection connection, @NotNull ClientboundBlockEntityDataPacket clientboundBlockEntityDataPacket,
                             HolderLookup.@NotNull Provider provider) {
        super.onDataPacket(connection, clientboundBlockEntityDataPacket, provider);
    }

    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }


    @Override
    protected void saveAdditional(@NotNull CompoundTag compoundTag, HolderLookup.@NotNull Provider provider) {
        super.saveAdditional(compoundTag, provider);
        compoundTag.put("inventory", this.itemHandler.serializeNBT(provider));
        compoundTag.putInt("progress", progress);
        compoundTag.putInt("maxProgress", maxProgress);


    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.@NotNull Provider provider) {
        this.itemHandler.deserializeNBT(provider, compoundTag.getCompound("inventory"));
        progress = compoundTag.getInt("progress");
        maxProgress = compoundTag.getInt("maxProgress");
        super.loadAdditional(compoundTag, provider);
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        assert this.level != null;
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public void tick() {

        assert level != null;
        if (!level.isClientSide()) {
            sync();

            RecipeInput inventory = new RecipeInput() {
                @Override
                public @NotNull ItemStack getItem(int index) {
                    return itemHandler.getStackInSlot(index);
                }

                @Override
                public int size() {
                    return itemHandler.getSlots();
                }
            };



            if (!onCooldown) {
                if (needNewRecipe && hasValidLicense()) {
                    RandomSource random = RandomSource.create();
                    //getRandomRecipe will probably crash if a non-license item is put in.
                    //I'll fix it just...don't do that for now
                    currentRecipe = getRandomRecipe(random);

                    System.out.println("Recipe selected: " + currentRecipe.input().getItems()[0].toString() + " for " + currentRecipe.output().toString());

                    needNewRecipe = false;
                    int variation = currentRecipe.variation();
                    orderVariation = random.nextIntBetweenInclusive(-variation, variation);

                    System.out.println("Variation is " + orderVariation);
                    System.out.println("Expected input size is " + (orderVariation + currentRecipe.input().count()));

                    orderTimeRemaining = 600;
                    //30 seconds default, this can be changed to be in the recipe later on if we want more control over it
                }

                if (currentRecipe == null) {
                    return;
                }

                for (int i = 0; i < 10; i++) {
                    if (!itemHandler.getStackInSlot(i).isEmpty()) {
                        ItemStack inputStack = itemHandler.getStackInSlot(i).copy();
                        //Rather than changing the value in the recipe, we change the value that the recipe is checking itself again
                        inputStack.setCount(inputStack.getCount() - orderVariation);
                        if (currentRecipe.input().test(inputStack)) {
                            if (canInsertItemIntoOutputSlot(inventory, currentRecipe.output()) &&
                                    hasOutputSpace(this, currentRecipe) /*&&
                                    //I don't think hasLicense is needed any
                                    hasLicence(this, currentRecipe)*/) {

                                itemHandler.getStackInSlot(i).shrink(currentRecipe.input().count() + orderVariation);
                                needNewRecipe = true;
                                //This allows a new order to be completed every single tick, maybe add some sort of
                                //additional cooldown between orders to slow things down a little?

                                //Recipe executing logic. You wrote this, thanks :)
                                for (int k = 10; k <= 12; k++) {
                                    ItemStack slotStack = itemHandler.getStackInSlot(k);
                                    if (slotStack.isEmpty()) {
                                        //For some reason during testing I got a carrot order that paid 4 bucks instead of 2 and another that paid nothing. Will continue testing
                                        itemHandler.setStackInSlot(k, currentRecipe.output());
                                        break;
                                    } else if (slotStack.getItem() == currentRecipe.output().getItem()) {
                                        int newCount = slotStack.getCount() + currentRecipe.output().getCount();
                                        if (newCount <= slotStack.getMaxStackSize()) {
                                            slotStack.setCount(newCount);
                                            itemHandler.setStackInSlot(k, slotStack);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            //State change logic between cooldown and usable
            if(onCooldown) {
                cooldownTimer--;
                if (cooldownTimer <= 0) {
                    onCooldown = false;
                    needNewRecipe = true;
                }
            } else if (!itemHandler.getStackInSlot(LICENCE_SLOT).isEmpty()) { //Checking the license slot prevents it from going on cooldown as soon as it's placed
                orderTimeRemaining--;
                if (orderTimeRemaining == 0) {
                    onCooldown = true;
                    cooldownTimer = 30;
                    return;
                }
            }

            sync();

            /*
            Optional<RecipeHolder<MarketRecipe>> match = level.getRecipeManager()
                    .getRecipeFor(MarketRecipe.Type.INSTANCE, inventory, level);*/

            /*
            if (match.isPresent()) {
                if (canInsertItemIntoOutputSlot(inventory, match.get().value().output()) &&
                        hasOutputSpace(this, match.get().value()) &&
                        hasLicence(this, match.get().value())) {

                    ItemStack output = match.get().value().output();
                    SizedIngredient input = match.get().value().input();

                    for (int i = 10; i <= 12; i++) {
                        ItemStack slotStack = itemHandler.getStackInSlot(i);
                        if (slotStack.isEmpty()) {
                            itemHandler.setStackInSlot(i, new ItemStack(output.getItem(), output.getCount()));
                            break;
                        } else if (slotStack.getItem() == output.getItem()) {
                            int newCount = slotStack.getCount() + output.getCount();
                            if (newCount <= slotStack.getMaxStackSize()) {
                                slotStack.setCount(newCount);
                                itemHandler.setStackInSlot(i, slotStack);
                                break;
                            }
                        }
                    }

                    for (int i = 1; i <= 9; i++) {
                        ItemStack slotStack = itemHandler.getStackInSlot(i);
                        if (!slotStack.isEmpty() && input.test(slotStack)) {
                            slotStack.shrink(input.count());
                            break;
                        }

                    }



                    sync();
                }
            }*/
        }
    }

    private boolean canInsertItemIntoOutputSlot(RecipeInput inventory, ItemStack output) {
        for (int i = 10; i <= 12; i++) {
            ItemStack slotStack = inventory.getItem(i);
            if (slotStack.isEmpty() || (slotStack.getItem() == output.getItem() && slotStack.getCount() + output.getCount() <= slotStack.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasOutputSpace(MarketBlockEntity entity, MarketRecipe recipe) {
        ItemStack resultStack = recipe.getResultItem(Objects.requireNonNull(getLevel()).registryAccess());

        for (int i = 10; i <= 12; i++) {
            ItemStack outputSlotStack = entity.itemHandler.getStackInSlot(i);
            if (outputSlotStack.isEmpty()) {
                if (resultStack.getCount() <= resultStack.getMaxStackSize()) {
                    return true;
                }
            } else if (outputSlotStack.getItem() == resultStack.getItem()) {
                if (outputSlotStack.getCount() + resultStack.getCount() <= outputSlotStack.getMaxStackSize()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasLicence(MarketBlockEntity entity, MarketRecipe recipe) {
        Ingredient licence = recipe.license();
        return licence.test(entity.itemHandler.getStackInSlot(LICENCE_SLOT));
    }

    private boolean hasValidLicense() {
        if (itemHandler.getStackInSlot(LICENCE_SLOT).isEmpty()) {
            return false;
        }
        List<RecipeHolder<MarketRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(MarketRecipe.Type.INSTANCE);
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).value().license().test(itemHandler.getStackInSlot(LICENCE_SLOT))) {
                return true;
            }
        }

        return false;
    }

    private MarketRecipe getRandomRecipe(RandomSource random) {
        //Get all market recipes
        List<RecipeHolder<MarketRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(MarketRecipe.Type.INSTANCE);

        //getAllRecipesFor returns an immutable list, so make a new mutable list
        List<MarketRecipe> availableRecipes = new ArrayList<>();

        //Filter them so that only those whose license item matches the license item in the block remain
        for (int i = recipes.size() - 1; i >= 0; i--) {
            if (recipes.get(i).value().license().test(itemHandler.getStackInSlot(LICENCE_SLOT))) {
                availableRecipes.add(recipes.get(i).value());
            }
        }

        //Pick a random recipe from the remaining list
        int index = random.nextIntBetweenInclusive(0, availableRecipes.size() - 1);
        return availableRecipes.get(index);
    }
}
