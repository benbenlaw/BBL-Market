package com.benbenlaw.market.item;

import com.benbenlaw.market.Market;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(Market.MOD_ID);

    /*
    DeferredItem<Item> INGOT_MOLD = ITEMS.register("ingot_mold",
            () -> new Item(new Item.Properties()));

     */



    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }

}
