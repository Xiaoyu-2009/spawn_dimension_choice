package com.xiaoyu.spawn_dimension_choice.event;

import com.xiaoyu.spawn_dimension_choice.SpawnDimensionChoice;
import com.xiaoyu.spawn_dimension_choice.config.SpawnDimensionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
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
                    
                    // 直接使用维度ID字符串
                    ServerLevel targetLevel = null;
                    
                    // 遍历所有维度，查找匹配的维度
                    for (ServerLevel level : player.getServer().getAllLevels()) {
                        String levelId = level.dimension().location().toString();
                        // 检查是否包含维度ID (部分匹配也接受)
                        if (levelId.contains(selectedDimension) || selectedDimension.contains(levelId)) {
                            targetLevel = level;
                            break;
                        }
                    }
                    
                    // 如果还没找到，尝试原版维度
                    if (targetLevel == null) {
                        if (selectedDimension.contains("overworld")) {
                            targetLevel = player.getServer().getLevel(Level.OVERWORLD);
                        } else if (selectedDimension.contains("nether")) {
                            targetLevel = player.getServer().getLevel(Level.NETHER);
                        } else if (selectedDimension.contains("end")) {
                            targetLevel = player.getServer().getLevel(Level.END);
                        }
                    }

                    if (targetLevel == null && selectedDimension.contains(":")) {
                        try {
                            String[] parts = selectedDimension.split(":");
                            if (parts.length >= 2) {
                                String namespace = parts[0];
                                String path = parts[1];
                                
                                // 尝试匹配所有维度的命名空间和路径
                                for (ServerLevel level : player.getServer().getAllLevels()) {
                                    String levelNamespace = level.dimension().location().getNamespace();
                                    String levelPath = level.dimension().location().getPath();
                                    
                                    if (levelNamespace.contains(namespace) || namespace.contains(levelNamespace)) {
                                        if (levelPath.contains(path) || path.contains(levelPath)) {
                                            targetLevel = level;
                                            break;
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                    
                    if (targetLevel != null) {
                        final ServerLevel finalTargetLevel = targetLevel;
                        player.getServer().execute(() -> {
                            try {
                                BlockPos spawnPos;
                                
                                if (finalTargetLevel.dimension() == Level.NETHER) {
                                    spawnPos = findSafeNetherPosition(finalTargetLevel);
                                } else {
                                    spawnPos = finalTargetLevel.getSharedSpawnPos();
                                }
                                
                                boolean applySafety = shouldApplySafetyFeatures(finalTargetLevel.dimension());
                                
                                if (applySafety) {
                                    PlayerSpawnHandler.ensurePlatformExists(finalTargetLevel, spawnPos);
                                    PlayerSpawnHandler.clearSpaceAroundPlayer(finalTargetLevel, spawnPos);
                                }
                                
                                player.teleportTo(finalTargetLevel, spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 
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
} 