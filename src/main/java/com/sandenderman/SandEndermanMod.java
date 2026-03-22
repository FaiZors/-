package com.sandenderman;

import com.sandenderman.entity.SandEndermanEntity;
import com.sandenderman.registry.ModEntities;
import com.sandenderman.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.world.biome.BiomeKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SandEndermanMod implements ModInitializer {

    public static final String MOD_ID = "sandenderman";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModEntities.register();
        ModItems.register();

        BiomeModifications.addSpawn(
                BiomeSelectors.includeByKey(
                        BiomeKeys.DESERT,
                        BiomeKeys.BADLANDS,
                        BiomeKeys.ERODED_BADLANDS,
                        BiomeKeys.WOODED_BADLANDS
                ),
                SpawnGroup.MONSTER,
                ModEntities.SAND_ENDERMAN,
                1,
                1,
                1
        );
    }
}
