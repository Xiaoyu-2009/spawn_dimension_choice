package com.xiaoyu.spawn_dimension_choice.event;

import com.xiaoyu.spawn_dimension_choice.SpawnDimensionChoice;
import com.xiaoyu.spawn_dimension_choice.config.SpawnDimensionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@Mod.EventBusSubscriber(modid = SpawnDimensionChoice.MOD_ID)
公共 class PlayerSpawnHandler {
    private static final int PLATFORM_SIZE = 5;
    private static final int CLEAR_WIDTH = 5;
    private static final int CLEAR_HEIGHT = 4;
    private static final int CLEAR_LENGTH = 5;

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!player.getPersistentData().contains("SpawnDimension_FirstSpawn")) {
                player.getPersistentData().putBoolean("SpawnDimension_FirstSpawn", true);
                
                try {
                    String selectedDimension = SpawnDimensionChoice.getSelectedDimension();
                    ServerLevel targetLevel = null;

                    for (ServerLevel level : player.getServer().getAllLevels()) {
                        String levelId = level.dimension().location().toString();
                        if (levelId.contains(selectedDimension) || selectedDimension.contains(levelId)) {
                            targetLevel = level;
                            break;
                        }
                    }

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
                                BlockPos initialSearchPos;
                                
                                if (finalTargetLevel.dimension() == Level.NETHER) {
                                    initialSearchPos = new BlockPos(0, 65, 0);
                                } else {
                                    initialSearchPos = new BlockPos(0, 64, 0);
                                }
                                
                                BlockPos spawnPos = findSafeSpawnLocation(finalTargetLevel, initialSearchPos);
                                boolean applySafety = shouldApplySafetyFeatures(finalTargetLevel.dimension());
                                
                                if (applySafety) {
                                    ensurePlatformExists(finalTargetLevel, spawnPos);
                                    clearSpaceAroundPlayer(finalTargetLevel, spawnPos);
                                }
                                
                                player.teleportTo(finalTargetLevel, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 
                                        player.getYRot(), player.getXRot());
                                
                            } catch (Exception e) {}
                        });
                    } else {
                        ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
                        if (overworld != null) {
                            BlockPos initialSearchPos = new BlockPos(0, 64, 0);
                            BlockPos spawnPos = findSafeSpawnLocation(overworld, initialSearchPos);
                            
                            boolean applySafety = shouldApplySafetyFeatures(Level.OVERWORLD);
                            
                            if (applySafety) {
                                ensurePlatformExists(overworld, spawnPos);
                                clearSpaceAroundPlayer(overworld, spawnPos);
                            }
                            
                            player.teleportTo(overworld, 
                                    spawnPos.getX() + 0.5,
                                    spawnPos.getY(),
                                    spawnPos.getZ() + 0.5,
                                    player.getYRot(), player.getXRot());
                        }
                    }
                } catch (Exception e) {}
            }
        }
    }

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

    public static void ensurePlatformExists(ServerLevel level, BlockPos playerPos) {
        BlockPos belowPos = playerPos.below();
        BlockState belowState = level.getBlockState(belowPos);
        
        if (belowState.isAir() || !belowState.isSolidRender(level, belowPos)) {
            String platformBlockId = SpawnDimensionConfig.COMMON.platformBlockId.get();
            BlockState platformBlock;
            
            try {
                ResourceLocation blockId = new ResourceLocation(platformBlockId);
                if (ForgeRegistries.BLOCKS.containsKey(blockId)) {
                    platformBlock = ForgeRegistries.BLOCKS.getValue(blockId).defaultBlockState();
                } else {
                    platformBlock = Blocks.GLASS.defaultBlockState();
                }
            } catch (Exception e) {
                platformBlock = Blocks.GLASS.defaultBlockState();
            }
            
            int radius = PLATFORM_SIZE / 2;
            
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos platformPos = new BlockPos(playerPos.getX() + x, playerPos.getY() - 1, playerPos.getZ() + z);
                    level.setBlockAndUpdate(platformPos, platformBlock);
                }
            }
        }
    }

    public static void clearSpaceAroundPlayer(ServerLevel level, BlockPos playerPos) {
        BlockState atPos = level.getBlockState(playerPos);
        BlockState abovePos = level.getBlockState(playerPos.above());
        
        if (!atPos.isAir() || !abovePos.isAir()) {
            int widthRadius = CLEAR_WIDTH / 2;
            int lengthRadius = CLEAR_LENGTH / 2;
            
            for (int x = -widthRadius; x <= widthRadius; x++) {
                for (int y = 0; y < CLEAR_HEIGHT; y++) {
                    for (int z = -lengthRadius; z <= lengthRadius; z++) {
                        BlockPos clearPos = new BlockPos(playerPos.getX() + x, playerPos.getY() + y, playerPos.getZ() + z);
                        level.setBlockAndUpdate(clearPos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }

    private static BlockPos findSafeSpawnLocation(ServerLevel level, BlockPos initialPos) {
        int x = 0;
        int z = 0;
        
        boolean isNether = level.dimension() == Level.NETHER;
        int yStart = initialPos.getY();
        
        if (isNether) {
            for (int y = yStart; y > 40; y--) {
                BlockPos pos = new BlockPos(x, y, z);
                
                BlockPos belowPos = pos.below();
                BlockState belowState = level.getBlockState(belowPos);
                
                BlockState atPos = level.getBlockState(pos);
                BlockState abovePos = level.getBlockState(pos.above());
                
                if (!belowState.isAir() && belowState.isSolidRender(level, belowPos) && 
                    atPos.isAir() && abovePos.isAir()) {
                    return pos;
                }
            }
        }
        
        try {
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            
            if (isNether && surfaceY > 125) {
                surfaceY = 65;
            }
            
            BlockPos surfacePos = new BlockPos(x, surfaceY, z);
            if (isSafeLocation(level, surfacePos)) {
                return surfacePos;
            }
        } catch (Exception e) {}
        
        for (int radius = 4; radius <= 256; radius *= 2) {
            if (isNether) {
                for (int y = 65; y > 40; y -= 5) {
                    BlockPos newPos = new BlockPos(x, y, z);
                    if (isSafeLocation(level, newPos)) {
                        return newPos;
                    }
                }
            } else {
                try {
                    for (int offsetY = 0; offsetY < radius; offsetY += 4) {
                        int baseY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                        
                        for (int dy = -offsetY; dy <= offsetY; dy += 4) {
                            int newY = baseY + dy;
                            if (newY < 0) continue;
                            
                            BlockPos newPos = new BlockPos(x, newY, z);
                            if (isSafeLocation(level, newPos)) {
                                return newPos;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            if (radius > 32) {
                for (int dx = -radius; dx <= radius; dx += 4) {
                    for (int dz = -radius; dz <= radius; dz += 4) {
                        if (dx == 0 && dz == 0) continue;
                        
                        int newX = dx;
                        int newZ = dz;
                        
                        try {
                            if (isNether) {
                                for (int newY = 65; newY > 40; newY -= 5) {
                                    BlockPos newPos = new BlockPos(newX, newY, newZ);
                                    if (isSafeLocation(level, newPos)) {
                                        return newPos;
                                    }
                                }
                            } else {
                                int newY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, newX, newZ);
                                BlockPos newPos = new BlockPos(newX, newY, newZ);
                                
                                if (isSafeLocation(level, newPos)) {
                                    return newPos;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        
        if (isNether) {
            return new BlockPos(0, 65, 0);
        } else {
            return new BlockPos(0, 64, 0);
        }
    }

    private static boolean isSafeLocation(ServerLevel level, BlockPos pos) {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        
        BlockState atPos = level.getBlockState(pos);
        BlockState abovePos = level.getBlockState(pos.above());
        
        boolean isSolid = !belowState.isAir() && belowState.isSolidRender(level, belowPos);
        boolean hasSpace = atPos.isAir() && abovePos.isAir();
        
        return isSolid && hasSpace;
    }
} 
