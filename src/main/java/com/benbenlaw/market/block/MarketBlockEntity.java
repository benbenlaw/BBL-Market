package com.benbenlaw.market.block;

import com.benbenlaw.market.recipe.MarketRecipe;
import com.benbenlaw.market.screen.MarketMenu;
import com.benbenlaw.market.utils.ModTags;
import com.benbenlaw.opolisutilities.block.entity.custom.OpolisBlockEntity;
import com.benbenlaw.opolisutilities.block.entity.custom.handler.InputOutputItemHandler;
import com.benbenlaw.opolisutilities.util.inventory.IInventoryHandlingBlockEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.apache.commons.lang3.ArrayUtils;
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
    public int[] previousOrders = ArrayUtils.remove(new int[1], 0);
    public double demand = 1;
    public boolean useNBT = false;

    public int LICENCE_SLOT = 0;

    private final IItemHandler marketItemHandler = new InputOutputItemHandler(itemHandler,
            (i, stack) -> {
                // Prevent input into output slots (10 to 12)
                if (i >= 10 && i <= 12) {
                    return false;
                }

                // Check if the slot index is within the range 1 to 9
                if (i >= 1 && i <= 9) {
                    Item itemToInsert = stack.getItem();

                    // Check if the item is already inserted in any of the input slots (1 to 9)
                    for (int slot = 1; slot <= 9; slot++) {
                        ItemStack existingStack = itemHandler.getStackInSlot(slot);
                        if (!existingStack.isEmpty() && existingStack.getItem() == itemToInsert) {
                            // Item is already present in one of the slots
                            if (i == slot) {
                                // If the item is already in the same slot, allow insertion up to max stack size
                                int maxStackSize = existingStack.getMaxStackSize();
                                int currentStackSize = existingStack.getCount();
                                int availableSpace = maxStackSize - currentStackSize;
                                return stack.getCount() <= availableSpace;
                            }
                            return false; // Item is already in another slot
                        }
                    }

                    // If the slot is empty or contains a different item
                    if (itemHandler.getStackInSlot(i).isEmpty()) {
                        // Allow insertion of the item up to its max stack size
                        int maxStackSize = stack.getMaxStackSize();
                        return stack.getCount() <= maxStackSize;
                    }

                    // Prevent insertion if the slot is occupied by a different item
                    return false;
                }

                // Check if the slot index is 0
                if (i == 0) {
                    // Allow only license items
                    return stack.getItem().asItem().getDefaultInstance().is(ModTags.LICENSES);
                }

                // Prevent insertion into output slots (10 to 12) and any other slots not covered by above conditions
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
        compoundTag.putIntArray("previousOrders", previousOrders);
        compoundTag.putDouble("demand", demand);

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
        previousOrders = compoundTag.getIntArray("previousOrders");
        demand = compoundTag.getDouble("demand");


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

            //Tick all previous order timers down, remove if at 0, and adjust demand accordingly
            for (int i = previousOrders.length - 1; i >= 0; i--) {
                previousOrders[i]--;
                if (previousOrders[i] == 0) {
                    previousOrders = ArrayUtils.remove(previousOrders, i);
                    demand += 0.1;
                    if (demand > 1) {
                        demand = 1;
                    }
                }
            }

            //Recalculate demand
            if (!itemHandler.getStackInSlot(LICENCE_SLOT).getItem().asItem().getDefaultInstance().is(ModTags.NO_DEMAND)) {
                if (previousOrders.length > 2) {
                    demand = 1 - (0.1 * (previousOrders.length / 3));
                    if (demand < 0.2) {
                        demand = 0.2;
                    }
                }
            } else {
                demand = 1;
            }

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

            //Find what the input size needs to be to satisfy the order
            int inputAmount;
            if (currentRecipe.value().inputWithNbt().isEmpty()) {
                inputAmount = (int) ((currentRecipe.value().input().count() + orderVariation) / demand);
                if (inputAmount > currentRecipe.value().input().getItems()[0].getMaxStackSize()) {
                    inputAmount = currentRecipe.value().input().getItems()[0].getMaxStackSize();
                }
                useNBT = false;
            } else {
                inputAmount = (int) ((currentRecipe.value().inputWithNbt().getCount() + orderVariation) / demand);
                if (inputAmount > currentRecipe.value().inputWithNbt().getMaxStackSize()) {
                    inputAmount = currentRecipe.value().inputWithNbt().getMaxStackSize();
                }
                useNBT = true;
            }

            //Getting the total amount of items that match the order, and their associated slots
            int totalAmount = 0;
            List<Integer> validSlots = new ArrayList<>();
            if (!useNBT) {
                for (int i = 1; i < 10; i++) {
                    ItemStack slotStack = itemHandler.getStackInSlot(i);
                    if (!slotStack.isEmpty()) {
                        for (ItemStack itemStack : currentRecipe.value().input().getItems()) {
                            if (itemStack.getItem() == slotStack.getItem()) {
                                validSlots.add(i);
                                totalAmount += slotStack.getCount();
                            }
                        }
                    }
                }
            } else {
                for (int i = 0; i < 10; i++) {
                    if (itemHandler.getStackInSlot(i).isEmpty() || itemHandler.getStackInSlot(i).getItem() != currentRecipe.value().inputWithNbt().getItem()) {
                        continue;
                    }

                    DataComponentMap recipeComponents = currentRecipe.value().inputWithNbt().getComponents();
                    DataComponentMap inputComponents = itemHandler.getStackInSlot(i).getComponents();
                    if (recipeComponents.equals(inputComponents)) {
                        validSlots.add(i);
                        totalAmount += itemHandler.getStackInSlot(i).getCount();
                    }
                }
            }

            if (totalAmount < inputAmount) {
                return;
            }

            //Get the total amount of space available to put the output and returning if it isn't enough
            int outputSpace = 0;
            for (int k = 10; k <= 12; k++) {
                if (itemHandler.getStackInSlot(k).isEmpty()) {
                    outputSpace += currentRecipe.value().output().getMaxStackSize();
                } else if (itemHandler.getStackInSlot(k).getItem() == currentRecipe.value().output().getItem()) {
                    outputSpace += itemHandler.getStackInSlot(k).getMaxStackSize() - itemHandler.getStackInSlot(k).getCount();
                }
            }

            if (outputSpace < currentRecipe.value().output().getCount()) {
                return;
            }

            //Consume input
            for (int slot : validSlots) {
                if (inputAmount > itemHandler.getStackInSlot(slot).getCount()) {
                    inputAmount -= itemHandler.getStackInSlot(slot).getCount();
                    itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
                } else {
                    itemHandler.getStackInSlot(slot).shrink(inputAmount);
                    inputAmount = 0;
                }
            }

            //Add output
            int outputAmount = currentRecipe.value().output().getCount();
            for (int i = 10; i <= 12; i++) {
                if (itemHandler.getStackInSlot(i).isEmpty()) {
                    if (outputAmount <= currentRecipe.value().output().getMaxStackSize()) {
                        itemHandler.setStackInSlot(i, new ItemStack(currentRecipe.value().output().getItem(), outputAmount));
                        previousOrders = ArrayUtils.add(previousOrders, 600);
                        needNewRecipe = true;
                        break;
                    } else {
                        itemHandler.setStackInSlot(i, new ItemStack(currentRecipe.value().output().getItem(), currentRecipe.value().output().getMaxStackSize()));
                    }
                } else if (itemHandler.getStackInSlot(i).getItem() == currentRecipe.value().output().getItem()) {
                    int slotSpace = itemHandler.getStackInSlot(i).getMaxStackSize() - itemHandler.getStackInSlot(i).getCount();
                    if (outputAmount <= slotSpace) {
                        itemHandler.getStackInSlot(i).grow(outputAmount);
                        previousOrders = ArrayUtils.add(previousOrders, 600);
                        needNewRecipe = true;
                        break;
                    } else {
                        itemHandler.getStackInSlot(i).setCount(itemHandler.getStackInSlot(i).getMaxStackSize());
                        outputAmount -= slotSpace;
                    }
                }
            }

            /*for (int i = 1; i < 10; i++) {
                if (!itemHandler.getStackInSlot(i).isEmpty()) {
                    ItemStack inputStack = itemHandler.getStackInSlot(i).copy();
                    //Rather than changing the value in the recipe, we change the value that the recipe is checking itself again
                    inputStack.shrink(orderVariation);

                    DataComponentMap dataComponentMapInput = inputStack.getComponents();
                    DataComponentMap dataComponentMapRecipe = currentRecipe.value().inputWithNbt().getComponents();

                    DataComponentMap dataComponentMapRecipeOutput = currentRecipe.value().output().getComponents();
                    ItemStack output = new ItemStack(currentRecipe.value().output().getItem(), currentRecipe.value().output().getCount());
                    output.applyComponents(dataComponentMapRecipeOutput);

                    /*
                    System.out.println("output: " + output); // Debug log
                    System.out.println("recipe: " + currentRecipe); // Debug log


                    boolean isDataMapEqual = dataComponentMapInput.equals(dataComponentMapRecipe);


                    //normal recipes
                    if (currentRecipe.value().input().test(inputStack)) {
                        if (isDataMapEqual) {
                            completeRecipe(currentRecipe.value().inputWithNbt().getCount() + orderVariation, i, output);
                        } else {
                            completeRecipe(currentRecipe.value().input().count() + orderVariation, i, output);
                        }
                    }
                }
            }*/

        }
    }

    private void completeRecipe(int inputCount, int slot, ItemStack output) {
        int outputCount = output.getCount();
        int outputSpace = 0;

        //Check if the output slots have enough space to hold the output, saves on extra checks later on
        for (int k = 10; k <= 12; k++) {
            if (itemHandler.getStackInSlot(k).isEmpty()) {
                outputSpace += output.getMaxStackSize();
            }
            if (itemHandler.getStackInSlot(k).getItem() == output.getItem()) {
                outputSpace += itemHandler.getStackInSlot(k).getMaxStackSize() - itemHandler.getStackInSlot(k).getCount();
            }
        }

        if (outputSpace < outputCount) {
            return;
        }

        for (int i = 10; i <= 12; i++) {
            ItemStack outputSlot = itemHandler.getStackInSlot(i);
            if (outputSlot.isEmpty()) {
                if (outputCount <= output.getMaxStackSize()) {
                    itemHandler.setStackInSlot(i, output.copy());
                    itemHandler.getStackInSlot(slot).shrink(inputCount);
                    needNewRecipe = true;
                    previousOrders = ArrayUtils.add(previousOrders, 1200);
                    //Only break when the output count reaches 0, otherwise move onto the next slot
                    break;
                } else {
                    itemHandler.setStackInSlot(i, new ItemStack(output.getItem(), output.getMaxStackSize()));
                }
            } else if (outputSlot.getItem() == output.getItem()) {
                if (outputSlot.getCount() + outputCount <= outputSlot.getMaxStackSize()) {
                    outputSlot.grow(outputCount);
                    itemHandler.getStackInSlot(slot).shrink(inputCount);
                    needNewRecipe = true;
                    previousOrders = ArrayUtils.add(previousOrders, 1200);
                    break;
                } else {
                    output.shrink(outputSlot.getMaxStackSize() - outputSlot.getCount());
                    outputSlot.setCount(outputSlot.getMaxStackSize());
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