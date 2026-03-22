package com.sandenderman.registry;

import com.sandenderman.SandEndermanMod;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class ModItems {

    public static final Item DESERT_SOUL = Registry.register(
            Registries.ITEM,
            new Identifier(SandEndermanMod.MOD_ID, "desert_soul"),
            new Item(new FabricItemSettings().rarity(Rarity.RARE).maxCount(16))
    );

    public static final Item SAND_FRAGMENT = Registry.register(
            Registries.ITEM,
            new Identifier(SandEndermanMod.MOD_ID, "sand_fragment"),
            new Item(new FabricItemSettings().rarity(Rarity.UNCOMMON).maxCount(64))
    );

    public static final Item CURSED_DESERT_EYE = Registry.register(
            Registries.ITEM,
            new Identifier(SandEndermanMod.MOD_ID, "cursed_desert_eye"),
            new Item(new FabricItemSettings().rarity(Rarity.EPIC).maxCount(1))
    );

    public static void register() {
        SandEndermanMod.LOGGER.info("Registering Sand Enderman items...");
    }
}
