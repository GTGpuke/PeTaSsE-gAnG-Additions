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
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.math.Vec3d;

import com.petassegang.addons.backrooms.portal.client.view.PortalRenderState;
import com.petassegang.addons.backrooms.portal.client.view.PortalStencilRenderer;
import com.petassegang.addons.backrooms.portal.client.world.RemoteWorldRegistry;

/**
 * Hooks du rendu monde pour les portails : dessin des portails en fin de frame, substitution de la
 * position du joueur par la camera distante quand {@code setupTerrain} centre le terrain d'une vue,
 * particules des mondes distants ignorees, et reconstructions de sections repercutees sur la vue
 * secondaire du monde principal.
 */
@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private @Nullable ClientWorld world;

    @Inject(method = "render", at = @At("TAIL"))
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

    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getX()D"))
    private double backrooms$terrainCenterX(ClientPlayerEntity player) {
        Vec3d override = PortalRenderState.cameraOverride();
        return override != null ? override.x : player.getX();
    }

    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getY()D"))
    private double backrooms$terrainCenterY(ClientPlayerEntity player) {
        Vec3d override = PortalRenderState.cameraOverride();
        return override != null ? override.y : player.getY();
    }

    @Redirect(method = "setupTerrain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getZ()D"))
    private double backrooms$terrainCenterZ(ClientPlayerEntity player) {
        Vec3d override = PortalRenderState.cameraOverride();
        return override != null ? override.z : player.getZ();
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

    /** Le gestionnaire de particules est celui du monde principal : un monde distant tique ses entities sans particules. */
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;ZZDDDDDD)V", at = @At("HEAD"), cancellable = true)
    private void backrooms$dropRemoteParticles(
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
        if (this.world != this.client.world) {
            ci.cancel();
        }
    }

    /** Une section du monde principal a reconstruire l'est aussi dans sa vue secondaire (portails imbriques). */
    @Inject(method = "scheduleChunkRender", at = @At("HEAD"))
    private void backrooms$mirrorChunkRender(int x, int y, int z, boolean important, CallbackInfo ci) {
        if ((Object) this == this.client.worldRenderer) {
            RemoteWorldRegistry.onMainChunkRenderScheduled(x, y, z, important);
        }
    }
}
