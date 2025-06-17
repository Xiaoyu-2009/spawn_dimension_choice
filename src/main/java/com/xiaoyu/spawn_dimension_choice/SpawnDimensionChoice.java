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
public class SpawnDimensionChoice {
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
    
    /**
     * 设置选定的维度
     * @param dimension 维度ID
     */
    public static void setSelectedDimension(String dimension) {
        if (dimension == null || dimension.isEmpty()) {
            selectedDimension = "overworld";
            return;
        }
        
        // 验证维度ID格式是否符合ResourceLocation规范
        try {
            new ResourceLocation(dimension);
            selectedDimension = dimension;
        } catch (Exception e) {
            selectedDimension = "overworld";
        }
    }
    
    /**
     * 获取当前选择的维度
     * @return 维度ID
     */
    public static String getSelectedDimension() {
        return selectedDimension;
    }
} 