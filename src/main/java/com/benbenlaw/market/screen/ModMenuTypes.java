package com.benbenlaw.market.screen;

import com.benbenlaw.market.Market;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(BuiltInRegistries.MENU, Market.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<MarketMenu>> MARKET_MENU;


    static {
        MARKET_MENU = MENUS.register("market_menu", () ->
                IMenuTypeExtension.create(MarketMenu::new));


    }


    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);


    }
}