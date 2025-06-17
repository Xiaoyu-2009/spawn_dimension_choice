package com.xiaoyu.spawn_dimension_choice.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SpawnDimensionConfig {
    public static final CommonConfig COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        final Pair<CommonConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(CommonConfig::new);
        COMMON = specPair.getLeft();
        COMMON_SPEC = specPair.getRight();
    }

    public static class CommonConfig {
        public final ForgeConfigSpec.ConfigValue<List<String>> whitelistDimensions;
        public final ForgeConfigSpec.ConfigValue<List<String>> blacklistDimensions;
        public final ForgeConfigSpec.ConfigValue<String> platformBlockId;

        public CommonConfig(ForgeConfigSpec.Builder builder) {
            builder.comment("Spawn Dimension Choice - Common Configuration")
                   .push("safety_features");
                   
            whitelistDimensions = builder
                    .comment("Dimensions where safety features will be applied (glass platform, clearing blocks)",
                            "If the list is empty, safety features will apply to all dimensions except those in blacklist",
                            "Format: [\"minecraft:overworld\", \"minecraft:the_nether\", \"modid:dimension_id\"]")
                    .define("whitelistDimensions", Collections.emptyList());
            
            blacklistDimensions = builder
                    .comment("Dimensions where safety features will NOT be applied",
                            "These dimensions will remain unmodified when player spawns",
                            "Format: [\"minecraft:overworld\", \"minecraft:the_nether\", \"modid:dimension_id\"]")
                    .define("blacklistDimensions", Arrays.asList("minecraft:overworld", "minecraft:the_end"));
                    
            platformBlockId = builder
                    .comment("The block to use for spawning platforms")
                    .define("platformBlockId", "minecraft:glass");
                    
            builder.pop();
        }
    }
} 