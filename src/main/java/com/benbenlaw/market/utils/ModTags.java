package com.benbenlaw.market.utils;

import com.benbenlaw.market.Market;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {


    public static final TagKey<Item> LICENSES = tag("licenses");
    public static final TagKey<Item> NO_DEMAND = tag("no_demand");

    private static TagKey<Item> tag(String name) {
        return ItemTags.create(ResourceLocation.fromNamespaceAndPath(Market.MOD_ID, name));
    }
}
