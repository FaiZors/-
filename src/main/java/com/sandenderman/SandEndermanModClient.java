package com.sandenderman;

import com.sandenderman.client.SandEndermanRenderer;
import com.sandenderman.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class SandEndermanModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.SAND_ENDERMAN, SandEndermanRenderer::new);
    }
}
