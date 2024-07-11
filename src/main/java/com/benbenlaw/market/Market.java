package com.benbenlaw.market;

import com.benbenlaw.market.block.ModBlockEntities;
import com.benbenlaw.market.block.ModBlocks;
import com.benbenlaw.market.item.ModItems;
import com.benbenlaw.market.screen.MarketScreen;
import com.benbenlaw.market.screen.ModMenuTypes;
import com.mojang.logging.LogUtils;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Market.MOD_ID)
public class Market {
    public static final String MOD_ID = "market";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Market(IEventBus modEventBus) {

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
    //    ModCreativeModTab.register(modEventBus);
        modEventBus.addListener(this::registerCapabilities);
        ModMenuTypes.register(modEventBus);
        ModBlockEntities.register(modEventBus);
    //    ModRecipes.register(modEventBus);
    //
    //    modEventBus.addListener(this::commonSetup);

    }

    public void registerCapabilities(RegisterCapabilitiesEvent event) {
         ModBlockEntities.registerCapabilities(event);
    }

    public void commonSetup(RegisterPayloadHandlersEvent event) {
    }

    @EventBusSubscriber(modid = Market.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(ModMenuTypes.MARKET_MENU.get(), MarketScreen::new);
        }
    }
}
