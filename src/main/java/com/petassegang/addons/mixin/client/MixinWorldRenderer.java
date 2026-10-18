package com.petassegang.addons.mixin.client;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.Vec3d;

import com.petassegang.addons.backrooms.portal.client.view.PortalRenderState;
import com.petassegang.addons.backrooms.portal.client.view.PortalStencilRenderer;
import com.petassegang.addons.backrooms.portal.client.world.RemoteWorld;
import com.petassegang.addons.backrooms.portal.client.world.RemoteWorldRegistry;
import com.petassegang.addons.backrooms.portal.client.world.StraddlingEntityRenderer;

/**
 * Points d'accroche du rendu monde pour les portails : dessin des portails avant la couche translucide et fin de l'image
 * sous une vue qui couvre l'ecran, centre de la grille de sections d'une vue substitue a la position du joueur dans
 * {@code setupTerrain}, ciel d'une vue rapporte a sa camera, particules des mondes distants confiees a leur propre
 * contenu, entities a cheval sur un portail laissees a leur dessin en deux moities, et reconstructions de sections
 * repercutees sur la vue secondaire de leur monde.
 */
@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private @Nullable ClientWorld world;

    /**
     * Les portails se dessinent apres les blocs opaques, les entities et les block entities, et avant tout ce qui est
     * translucide : l'eau, le verre teinte, les parties translucides des entities, les particules,
     * les nuages et la pluie du monde du joueur se fondent ensuite par-dessus la vue, au lieu d'en cacher le cadre.
     */
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/TexturedRenderLayers;getEntityTranslucentCull()Lnet/minecraft/client/render/RenderLayer;"))
    private void backrooms$renderPortals(
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {
        PortalRenderState.setBlockOutlineAllowed(renderBlockOutline);
        PortalStencilRenderer.renderAllVisiblePortals(camera, positionMatrix, projectionMatrix, lightmapTextureManager,
                tickCounter.getTickDelta(false));
    }

    /** Fin du rendu du monde : le stencil qui refusait la suite, sous une vue couvrant l'ecran, est rendu. */
    @Inject(method = "render", at = @At("TAIL"))
    private void backrooms$endPortals(CallbackInfo ci) {
        PortalStencilRenderer.endWorldRender();
    }

    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getX()D"))
    private double backrooms$terrainCenterX(ClientPlayerEntity player) {
        return PortalRenderState.hasTerrainCenter() ? PortalRenderState.terrainCenterX() : player.getX();
    }

    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getY()D"))
    private double backrooms$terrainCenterY(ClientPlayerEntity player) {
        return PortalRenderState.hasTerrainCenter() ? PortalRenderState.terrainCenterY() : player.getY();
    }

    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getZ()D"))
    private double backrooms$terrainCenterZ(ClientPlayerEntity player) {
        return PortalRenderState.hasTerrainCenter() ? PortalRenderState.terrainCenterZ() : player.getZ();
    }

    /**
     * Le ciel d'une vue se place par rapport a sa camera, pas a celle du joueur : couleur du ciel, et surtout le plan
     * sombre que vanilla tire sous l'horizon quand le joueur est bas (il noircirait le ciel vu depuis le Level 0).
     */
    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getCameraPosVec(F)Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d backrooms$skyPlayerCameraPos(ClientPlayerEntity player, float tickDelta) {
        Vec3d override = PortalRenderState.viewCameraPos();
        return override != null ? override : player.getCameraPosVec(tickDelta);
    }

    @Redirect(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;getPos()Lnet/minecraft/util/math/Vec3d;"))
    private Vec3d backrooms$skyCameraPos(Camera camera) {
        Vec3d override = PortalRenderState.viewCameraPos();
        return override != null ? override : camera.getPos();
    }

    /**
     * Les particules d'un monde distant (entities, effets des blocs, paquets du serveur) vont dans le contenu de
     * particules de ce monde, dessine dans ses vues ; vanilla les aurait ajoutees au monde du joueur, a ses coordonnees.
     */
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;ZZDDDDDD)V", at = @At("HEAD"), cancellable = true)
    private void backrooms$routeRemoteParticles(
            ParticleEffect parameters,
            boolean shouldAlwaysSpawn,
            boolean important,
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ,
            CallbackInfo ci) {
        RemoteWorld remote = RemoteWorldRegistry.ownerOf((WorldRenderer) (Object) this);
        if (remote != null) {
            ci.cancel();
            remote.spawnParticle(this.client, parameters, shouldAlwaysSpawn, important, x, y, z, velocityX, velocityY, velocityZ);
        } else if (this.world != this.client.world && !PortalRenderState.isRendering()) {
            // Renderer sans monde distant connu : ses particules n'ont nulle part ou aller.
            ci.cancel();
        }
    }

    /** Une entity a cheval sur un portail se dessine en deux moities, chacune coupee au plan : vanilla ne la dessine pas entiere. */
    @Inject(method = "renderEntity", at = @At("HEAD"), cancellable = true)
    private void backrooms$skipEntityDrawnInParts(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta,
            MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (StraddlingEntityRenderer.skipsVanillaDraw(entity, tickDelta)) {
            ci.cancel();
        }
    }

    /**
     * Une section a reconstruire l'est aussi dans la vue secondaire de son monde (portails qui y ramenent). Le monde du
     * renderer designe la vue, pas celui de {@code MinecraftClient}, substitue pendant le rendu des portails : les mises
     * a jour de lumiere faites pendant une vue sont relayees elles aussi.
     */
    @Inject(method = "scheduleChunkRender", at = @At("HEAD"))
    private void backrooms$mirrorChunkRender(int x, int y, int z, boolean important, CallbackInfo ci) {
        RemoteWorldRegistry.onChunkRenderScheduled((WorldRenderer) (Object) this, this.world, x, y, z, important);
    }
}
