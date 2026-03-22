package com.sandenderman.registry;

import com.sandenderman.SandEndermanMod;
import com.sandenderman.entity.SandEndermanEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {

    public static final EntityType<SandEndermanEntity> SAND_ENDERMAN = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(SandEndermanMod.MOD_ID, "sand_enderman"),
            FabricEntityTypeBuilder.<SandEndermanEntity>create(SpawnGroup.MONSTER, SandEndermanEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9f, 5.0f))
                    .build()
    );

    public static void register() {
        FabricDefaultAttributeRegistry.register(SAND_ENDERMAN, SandEndermanEntity.createSandEndermanAttributes());
        SandEndermanMod.LOGGER.info("Registering Sand Enderman entity...");
    }
}
