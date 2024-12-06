package com.benbenlaw.market.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ModConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<Integer> ticksSinceLastDamage;

    static {
        BUILDER.push("BBL Market Config File");

        ticksSinceLastDamage = BUILDER.comment("The max time before the license takes damage, default = 600")
                .define("Max time since last damage", 600);

        BUILDER.pop();
        SPEC = BUILDER.build();

    }
}