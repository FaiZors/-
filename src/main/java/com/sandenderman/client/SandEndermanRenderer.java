package com.sandenderman.client;

import com.sandenderman.SandEndermanMod;
import com.sandenderman.entity.SandEndermanEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EndermanEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class SandEndermanRenderer extends MobEntityRenderer<SandEndermanEntity, EndermanEntityModel<SandEndermanEntity>> {

    private static final Identifier TEXTURE =
            new Identifier(SandEndermanMod.MOD_ID, "textures/entity/sand_enderman.png");

    public SandEndermanRenderer(EntityRendererFactory.Context context) {
        super(context, new EndermanEntityModel<>(context.getPart(EntityModelLayers.ENDERMAN)), 0.8f);
    }

    @Override
    public Identifier getTexture(SandEndermanEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(SandEndermanEntity entity, MatrixStack matrices, float tickDelta) {
        matrices.scale(1.15f, 1.15f, 1.15f);
    }

    @Override
    public void render(SandEndermanEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}
