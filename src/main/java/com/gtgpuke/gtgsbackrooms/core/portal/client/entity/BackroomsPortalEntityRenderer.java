package com.gtgpuke.gtgsbackrooms.core.portal.client.entity;

import org.joml.Matrix4f;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import com.gtgpuke.gtgsbackrooms.core.ModConfig;
import com.gtgpuke.gtgsbackrooms.core.portal.client.view.PortalRenderState;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.PortalFacing;

/**
 * Renderer de l'entity portail : rien quand le rendu distant est actif (le contenu passe par le
 * stencil) ou quand le portail est invisible, un cadre sombre sinon, pour que le portail reste visible.
 */
public final class BackroomsPortalEntityRenderer extends EntityRenderer<BackroomsPortalEntity> {

    /** Texture jamais echantillonnee ; une texture vanilla evite un asset mort. */
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/misc/white.png");

    /** Renderer enregistre pour le type d'entity portail. */
    public BackroomsPortalEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public void render(
            BackroomsPortalEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light) {
        if (ModConfig.ENABLE_PORTAL_FEATURE && PortalRenderState.remoteRenderingAvailable() || !entity.visible()) {
            return;
        }
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        PortalFacing facing = entity.facingSource();
        double halfWidth = entity.width() / 2.0;
        double halfHeight = entity.height() / 2.0;
        for (int corner = 0; corner < 4; corner++) {
            double sw = (corner == 1 || corner == 2) ? halfWidth : -halfWidth;
            double sh = corner >= 2 ? halfHeight : -halfHeight;
            consumer.vertex(matrix,
                            (float) (facing.axisW.x * sw + facing.axisH.x * sh),
                            (float) (facing.axisW.y * sw + facing.axisH.y * sh),
                            (float) (facing.axisW.z * sw + facing.axisH.z * sh))
                    .color(0, 0, 0, 255);
        }
    }

    @Override
    public Identifier getTexture(BackroomsPortalEntity entity) {
        return TEXTURE;
    }
}
