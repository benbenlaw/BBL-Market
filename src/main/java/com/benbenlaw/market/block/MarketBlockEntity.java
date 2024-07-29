package com.benbenlaw.market.block;

import com.benbenlaw.market.recipe.MarketRecipe;
import com.benbenlaw.market.screen.MarketMenu;
import com.benbenlaw.market.utils.ModTags;
import com.benbenlaw.opolisutilities.block.entity.custom.handler.InputOutputItemHandler;
import com.benbenlaw.opolisutilities.util.inventory.IInventoryHandlingBlockEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
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
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MarketBlockEntity extends BlockEntity implements MenuProvider, IInventoryHandlingBlockEntity {
    private final ItemStackHandler itemHandler = new ItemStackHandler(13) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            sync();
        }

        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            if (slot == LICENCE_SLOT) {
                return 1;
            }
            return super.getStackLimit(slot, stack);
        }
    };

    private FakePlayer fakePlayer;

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

    public RecipeHolder<MarketRecipe> currentRecipe;

    public ResourceLocation recipeID = ResourceLocation.parse("market:null");
    public int orderVariation;
    public boolean needNewRecipe = true;
    public int ticksSinceLastDamage;

    public int LICENCE_SLOT = 0;
    // INPUT_SLOTS= [1,2,3,4,5,6,7,8,9];
    public int OUTPUT_SLOT_1 = 10;
    public int OUTPUT_SLOT_2 = 11;
    public int OUTPUT_SLOT_3 = 12;

    private final IItemHandler marketItemHandler = new InputOutputItemHandler(itemHandler,
            (i, stack) -> {
                if (i >= 1 && i <= 9) {
                    return !stack.getItem().asItem().getDefaultInstance().is(ModTags.LICENSES);
                }
                if (i == 0) {
                    return stack.getItem().asItem().getDefaultInstance().is(ModTags.LICENSES);
                }
                return false;
            },
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
        if (recipeID != null) {
            compoundTag.putString("recipeID", this.recipeID.toString());
        } else {
            compoundTag.putString("recipeID", "null");
        }
        compoundTag.putInt("orderVariation", orderVariation);
        compoundTag.putInt("ticksSinceLastDamage", ticksSinceLastDamage);

    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.@NotNull Provider provider) {
        this.itemHandler.deserializeNBT(provider, compoundTag.getCompound("inventory"));
        if (compoundTag.getString("recipeID").equals("null")) {
            recipeID = null;
        } else {
            this.recipeID = ResourceLocation.parse(compoundTag.getString("recipeID"));
        }
        orderVariation = compoundTag.getInt("orderVariation");
        ticksSinceLastDamage = compoundTag.getInt("ticksSinceLastDamage");


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

            //Create fake player to damage license item
            if (this.fakePlayer == null && level instanceof ServerLevel serverLevel) {
                this.fakePlayer = createFakePlayer(serverLevel);
            }

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

            //Only used for loading a world, as currentRecipe cannot be set in loadAdditional
            if (recipeID != null && currentRecipe == null) {
                Optional<RecipeHolder<?>> recipe = level.getRecipeManager().byKey(recipeID);
                if (recipe.isPresent()) {
                    currentRecipe = (RecipeHolder<MarketRecipe>) recipe.get();
                    needNewRecipe = false;
                }
            }

            if (needNewRecipe && hasValidLicense()) {
                RandomSource random = RandomSource.create();
                currentRecipe = getRandomRecipe(random);
                recipeID = ResourceLocation.parse(currentRecipe.toString());

                needNewRecipe = false;
                int variation = currentRecipe.value().variation();
                orderVariation = random.nextIntBetweenInclusive(-variation, variation);
            }

            if (currentRecipe == null) {
                return;
            }

            for (int i = 1; i < 10; i++) {
                if (!itemHandler.getStackInSlot(i).isEmpty()) {
                    ItemStack inputStack = itemHandler.getStackInSlot(i).copy();
                    //Rather than changing the value in the recipe, we change the value that the recipe is checking itself again
                    inputStack.setCount(inputStack.getCount() - orderVariation);

                    DataComponentMap dataComponentMapInput = inputStack.getComponents();
                    DataComponentMap dataComponentMapRecipe = currentRecipe.value().inputWithNbt().getComponents();

                    DataComponentMap dataComponentMapRecipeOutput = currentRecipe.value().output().getComponents();
                    ItemStack output = new ItemStack(currentRecipe.value().output().getItem(), currentRecipe.value().output().getCount());
                    output.applyComponents(dataComponentMapRecipeOutput);

                        /*
                        System.out.println("output: " + output); // Debug log
                        System.out.println("recipe: " + currentRecipe); // Debug log
                        */

                        boolean isDataMapEqual = dataComponentMapInput.equals(dataComponentMapRecipe);


                        //normal recipes
                        if (currentRecipe.value().input().test(inputStack) && !isDataMapEqual) {
                            completeRecipe(currentRecipe.value().input().count() + orderVariation, inventory, i, output);
                        }

                        //Recipes if the input has NBT data
                        else if (isDataMapEqual) {
                            completeRecipe(currentRecipe.value().inputWithNbt().getCount(), inventory, i, output);
                        }
                }
            }

            //Damage the license if it's been a certain amount of time since the last damage
            ticksSinceLastDamage++;
            if (ticksSinceLastDamage >= 600 /*TODO: Change to config value please Ben thanks Ben idk how to do it*/) {
                itemHandler.getStackInSlot(LICENCE_SLOT).hurtAndBreak(1, fakePlayer, fakePlayer.getEquipmentSlotForItem(ItemStack.EMPTY));
                ticksSinceLastDamage = 0;
            }

            //Remove current order if the license slot is empty, whether by damage or by player intervention
            if (itemHandler.getStackInSlot(LICENCE_SLOT).isEmpty()) {
                currentRecipe = null;
                recipeID = null;
                needNewRecipe = true;
            }
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

    private void completeRecipe(int inputCount, RecipeInput inventory, int slot, ItemStack output) {
        if (canInsertItemIntoOutputSlot(inventory, currentRecipe.value().output()) && hasOutputSpace(this, currentRecipe.value())) {

            itemHandler.getStackInSlot(slot).shrink(inputCount + orderVariation);
            needNewRecipe = true;

            for (int k = 10; k <= 12; k++) {
                ItemStack slotStack = itemHandler.getStackInSlot(k).copy();
                if (slotStack.isEmpty()) {
                    itemHandler.setStackInSlot(k, output);
                    break;
                } else if (slotStack.getItem() == output.getItem()) {
                    int newCount = slotStack.getCount() + output.getCount();
                    if (newCount <= slotStack.getMaxStackSize()) {
                        slotStack.setCount(newCount);
                        itemHandler.setStackInSlot(k, slotStack);
                        break;
                    }
                }
            }
        }
    }

    private boolean hasValidLicense() {
        if (itemHandler.getStackInSlot(LICENCE_SLOT).isEmpty()) {
            return false;
        }
        assert level != null;
        List<RecipeHolder<MarketRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(MarketRecipe.Type.INSTANCE);
        for (int i = 0; i < recipes.size(); i++) {
            if (recipes.get(i).value().license().test(itemHandler.getStackInSlot(LICENCE_SLOT))) {
                return true;
            }
        }

        return false;
    }

    private RecipeHolder<MarketRecipe> getRandomRecipe(RandomSource random) {
        // Get all market recipes
        assert level != null;
        List<RecipeHolder<MarketRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(MarketRecipe.Type.INSTANCE);

        // getAllRecipesFor returns an immutable list, so make a new mutable list
        List<RecipeHolder<MarketRecipe>> availableRecipes = new ArrayList<>();

        // Filter them so that only those whose license item matches the license item in the block remain
        for (int i = recipes.size() - 1; i >= 0; i--) {
            if (recipes.get(i).value().license().test(itemHandler.getStackInSlot(LICENCE_SLOT))) {
                availableRecipes.add(recipes.get(i));
            }
        }

        // Pick a random recipe from the remaining list
        int index = random.nextIntBetweenInclusive(0, availableRecipes.size() - 1);
        return availableRecipes.get(index);
    }

    private FakePlayer createFakePlayer(ServerLevel level) {
        return new FakePlayer(level, new GameProfile(UUID.randomUUID(), "Market"));
    }

}