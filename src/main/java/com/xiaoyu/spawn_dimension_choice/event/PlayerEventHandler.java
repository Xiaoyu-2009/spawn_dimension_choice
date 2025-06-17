package com.xiaoyu.spawn_dimension_choice.event;

import com.xiaoyu.spawn_dimension_choice.SpawnDimensionChoice;
import com.xiaoyu.spawn_dimension_choice.config.SpawnDimensionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = SpawnDimensionChoice.MOD_ID)
public class PlayerEventHandler {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!player.getPersistentData().contains("SpawnDimension_FirstLogin")) {
                player.getPersistentData().putBoolean("SpawnDimension_FirstLogin", true);
                
                try {
                    String selectedDimension = SpawnDimensionChoice.getSelectedDimension();
                    ResourceKey<Level> targetDimension = getTargetDimension(selectedDimension);
                    
                    ServerLevel targetLevel = player.getServer().getLevel(targetDimension);
                    if (targetLevel != null) {
                        player.getServer().execute(() -> {
                            try {
                                BlockPos spawnPos;
                                
                                if (targetDimension == Level.NETHER) {
                                    spawnPos = findSafeNetherPosition(targetLevel);
                                } else {
                                    spawnPos = targetLevel.getSharedSpawnPos();
                                }
                                
                                boolean applySafety = shouldApplySafetyFeatures(targetDimension);
                                
                                if (applySafety) {
                                    PlayerSpawnHandler.ensurePlatformExists(targetLevel, spawnPos);
                                    PlayerSpawnHandler.clearSpaceAroundPlayer(targetLevel, spawnPos);
                                }
                                
                                player.teleportTo(targetLevel, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 
                                        player.getYRot(), player.getXRot());
                            } catch (Exception e) {}
                        });
                    } else {
                        ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
                        if (overworld != null) {
                            boolean applySafety = shouldApplySafetyFeatures(Level.OVERWORLD);
                            BlockPos spawnPos = overworld.getSharedSpawnPos();
                            
                            if (applySafety) {
                                PlayerSpawnHandler.ensurePlatformExists(overworld, spawnPos);
                                PlayerSpawnHandler.clearSpaceAroundPlayer(overworld, spawnPos);
                            }
                            
                            player.teleportTo(overworld, 
                                    spawnPos.getX(),
                                    spawnPos.getY(),
                                    spawnPos.getZ(),
                                    player.getYRot(), player.getXRot());
                        }
                    }
                } catch (Exception e) {}
            }
        }
    }
    
    /**
     * 查找安全的下界位置 [避开上层基岩]
     * @param netherLevel 下界维度
     * @return 安全的出生位置
     */
    private static BlockPos findSafeNetherPosition(ServerLevel netherLevel) {
        int x = 0;
        int z = 0;
        
        for (int y = 65; y > 40; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (isSafeLocation(netherLevel, pos)) {
                return pos;
            }
        }
        
        return new BlockPos(0, 65, 0);
    }
    
    /**
     * 检查位置是否安全
     * @param level 世界
     * @param pos 位置
     * @return 是否安全
     */
    private static boolean isSafeLocation(ServerLevel level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        
        BlockState atPos = level.getBlockState(pos);
        BlockState abovePos = level.getBlockState(pos.above());
        
        return !belowState.isAir() && belowState.isSolidRender(level, belowPos) && 
               atPos.isAir() && abovePos.isAir();
    }
    
    /**
     * 检查是否应该在指定维度应用安全功能
     * @param dimension 目标维度
     * @return 是否应用安全功能
     */
    private static boolean shouldApplySafetyFeatures(ResourceKey<Level> dimension) {
        String dimensionId = dimension.location().toString();
        
        List<String> whitelist = SpawnDimensionConfig.COMMON.whitelistDimensions.get();
        List<String> blacklist = SpawnDimensionConfig.COMMON.blacklistDimensions.get();
        
        if (blacklist.contains(dimensionId)) {
            return false;
        }
        
        if (!whitelist.isEmpty() && !whitelist.contains(dimensionId)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 根据维度ID获取对应的维度ResourceKey
     * @param dimensionName 维度ID
     * @return 维度ResourceKey
     */
    private static ResourceKey<Level> getTargetDimension(String dimensionName) {
        if (dimensionName.equals("overworld")) return Level.OVERWORLD;
        if (dimensionName.equals("the_nether")) return Level.NETHER;
        if (dimensionName.equals("the_end")) return Level.END;
        
        try {
            ResourceLocation dimLocation = new ResourceLocation(dimensionName);
            return ResourceKey.create(ResourceKey.createRegistryKey(new ResourceLocation("dimension")), dimLocation);
        } catch (Exception e) {
            return Level.OVERWORLD;
        }
    }
} 