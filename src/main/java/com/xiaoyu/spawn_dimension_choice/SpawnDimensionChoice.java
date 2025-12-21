package com.xiaoyu.spawn_dimension_choice;

import com.xiaoyu.spawn_dimension_choice.config.SpawnDimensionConfig;
import com.xiaoyu.spawn_dimension_choice.event.PlayerEventHandler;
import com.xiaoyu.spawn_dimension_choice.event.PlayerSpawnHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SpawnDimensionChoice.MOD_ID)
公共 class SpawnDimensionChoice {
    public static final String MOD_ID = "spawn_dimension_choice";
    
    private static String selectedDimension = "overworld";

    public SpawnDimensionChoice() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SpawnDimensionConfig.COMMON_SPEC);
        modEventBus.addListener(this::setup);
        
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(PlayerEventHandler.class);
        MinecraftForge.EVENT_BUS.register(PlayerSpawnHandler.class);
    }

    private void setup(final FMLCommonSetupEvent event) {}

    public static void setSelectedDimension(String dimension) {
        if (dimension == null || dimension.isEmpty()) {
            selectedDimension = "overworld";
            return;
        }

        try {
            if (dimension.equals("overworld") || dimension.equals("the_nether") || dimension.equals("the_end")) {
                selectedDimension = dimension;
                return;
            }

            ResourceLocation dimLocation = new ResourceLocation(dimension);
            selectedDimension = dimLocation.toString();
        } catch (Exception e) {
            selectedDimension = "overworld";
        }
    }

    public static String getSelectedDimension() {
        return selectedDimension;
    }
} 
