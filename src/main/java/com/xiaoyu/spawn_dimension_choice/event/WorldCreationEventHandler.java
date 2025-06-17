package com.xiaoyu.spawn_dimension_choice.event;

import com.xiaoyu.spawn_dimension_choice.SpawnDimensionChoice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.List;

/**
 * 处理世界生成创建相关的事件
 */
@Mod.EventBusSubscriber(modid = SpawnDimensionChoice.MOD_ID, value = Dist.CLIENT)
public class WorldCreationEventHandler {
    // 预设维度列表
    private static final List<String> PRESET_DIMENSIONS = Arrays.asList(
            "overworld", "the_nether", "the_end"
    );
    private static EditBox customDimensionInput;
    private static boolean useCustomDimension = false;
    // 预设维度选择
    private static String lastSelectedPresetDimension = "overworld";
    // 自定义维度输入值
    private static String lastCustomDimensionValue = "";
    private static CycleButton<String> dimensionButton;
    private static Button customDimensionButton;
    private static CreateWorldScreen currentScreen;
    
    // 第一个按钮的X坐标
    private static final int FIRST_BUTTON_X = 85;
    // 第一个按钮的Y坐标
    private static final int FIRST_BUTTON_Y = 170;
    // 按钮宽度
    private static final int BUTTON_WIDTH = 150;
    // 按钮高度
    private static final int BUTTON_HEIGHT = 20;
    // 按钮之间的间距 
    private static final int BUTTON_SPACING = 10;
    
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init event) {
        if (event.getScreen() instanceof CreateWorldScreen worldScreen) {
            currentScreen = worldScreen;
            
            String currentDimension = SpawnDimensionChoice.getSelectedDimension();
            if (PRESET_DIMENSIONS.contains(currentDimension)) {
                lastSelectedPresetDimension = currentDimension;
                useCustomDimension = false;
            } else {
                lastCustomDimensionValue = currentDimension;
                useCustomDimension = true;
            }
            
            dimensionButton = CycleButton.<String>builder(dimension ->
                    Component.translatable("gui." + SpawnDimensionChoice.MOD_ID + ".dimension." + dimension))
                    .withValues(PRESET_DIMENSIONS)
                    .withInitialValue(lastSelectedPresetDimension)
                    .create(FIRST_BUTTON_X, FIRST_BUTTON_Y, BUTTON_WIDTH, BUTTON_HEIGHT, 
                            Component.translatable("gui." + SpawnDimensionChoice.MOD_ID + ".spawn_dimension"),
                            (button, newValue) -> {
                                lastSelectedPresetDimension = newValue;
                                
                                if (!useCustomDimension) {
                                    SpawnDimensionChoice.setSelectedDimension(newValue);
                                }
                            });

            customDimensionButton = Button.builder(
                    Component.translatable("gui." + SpawnDimensionChoice.MOD_ID + ".custom_dimension"),
                    button -> {
                        useCustomDimension = !useCustomDimension;
                        if (customDimensionInput != null) {
                            customDimensionInput.setVisible(useCustomDimension);
                            
                            if (useCustomDimension) {
                                if (!lastCustomDimensionValue.isEmpty()) {
                                    SpawnDimensionChoice.setSelectedDimension(lastCustomDimensionValue);
                                }
                            } else {
                                SpawnDimensionChoice.setSelectedDimension(lastSelectedPresetDimension);
                            }
                            refreshScreen();
                        }
                    })
                    .pos(FIRST_BUTTON_X + BUTTON_WIDTH + BUTTON_SPACING, FIRST_BUTTON_Y)
                    .size(BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();

            customDimensionInput = new EditBox(
                    Minecraft.getInstance().font,
                    FIRST_BUTTON_X, FIRST_BUTTON_Y + BUTTON_HEIGHT + BUTTON_SPACING, 
                    BUTTON_WIDTH * 2 + BUTTON_SPACING, BUTTON_HEIGHT,
                    Component.translatable("gui." + SpawnDimensionChoice.MOD_ID + ".dimension_input")
            );
            customDimensionInput.setValue(lastCustomDimensionValue.isEmpty() ? "minecraft:overworld" : lastCustomDimensionValue);
            customDimensionInput.setVisible(useCustomDimension);
            customDimensionInput.setResponder(text -> {
                if (!text.isEmpty()) {
                    lastCustomDimensionValue = text;
                    if (useCustomDimension) {
                        SpawnDimensionChoice.setSelectedDimension(text);
                    }
                }
            });
            
            event.addListener(dimensionButton);
            event.addListener(customDimensionButton);
            event.addListener(customDimensionInput);
        }
    }

    private static void refreshScreen() {
        if (currentScreen != null && Minecraft.getInstance() != null) {
            currentScreen.resize(Minecraft.getInstance(), currentScreen.width, currentScreen.height);
            
            if (dimensionButton != null) {
                dimensionButton.setValue(lastSelectedPresetDimension);
            }
        }
    }

    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        if (event.getScreen() instanceof CreateWorldScreen) {
            if (useCustomDimension) {
                if (!lastCustomDimensionValue.isEmpty()) {
                    SpawnDimensionChoice.setSelectedDimension(lastCustomDimensionValue);
                }
            } else {
                SpawnDimensionChoice.setSelectedDimension(lastSelectedPresetDimension);
            }
            
            useCustomDimension = false;
            currentScreen = null;
        }
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render event) {
        if (event.getScreen() instanceof CreateWorldScreen) {
            if (customDimensionInput != null) {
                customDimensionInput.setVisible(useCustomDimension);
            }
            
            if (dimensionButton != null) {
                if (!dimensionButton.getValue().equals(lastSelectedPresetDimension)) {
                    dimensionButton.setValue(lastSelectedPresetDimension);
                }
            }
        }
    }
} 