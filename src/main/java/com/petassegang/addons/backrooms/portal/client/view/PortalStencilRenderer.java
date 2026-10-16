package com.petassegang.addons.backrooms.portal.client.view;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;

import com.petassegang.addons.backrooms.portal.client.world.ClientPortalIndex;
import com.petassegang.addons.backrooms.portal.client.world.RemoteWorld;
import com.petassegang.addons.backrooms.portal.client.world.RemoteWorldRegistry;
import com.petassegang.addons.backrooms.portal.entity.BackroomsPortalEntity;
import com.petassegang.addons.backrooms.portal.transition.PortalRelevance;
import com.petassegang.addons.config.ModConfig;
import com.petassegang.addons.core.ModConstants;
import com.petassegang.addons.mixin.client.CameraAccessor;
import com.petassegang.addons.mixin.client.FrustumAccessor;
import com.petassegang.addons.mixin.client.LightmapTextureManagerAccessor;
import com.petassegang.addons.mixin.client.MinecraftClientAccessor;

/**
 * Rend l'autre cote de chaque portail visible, a la fin du rendu du monde, puis un niveau de portails imbriques.
 *
 * <p>Chaque zone de vue (voir {@link ViewAreaRenderer}) recoit une valeur de stencil distincte la ou elle est
 * visible. Pour chaque portail, la profondeur de la zone est poussee au plan lointain, le fond prend la couleur
 * de brouillard de la vue, puis la vue (ciel, blocs, entities, block entities, nuages) est dessinee depuis la
 * camera transposee, limitee a la zone par le stencil et au plan du portail d'arrivee par la projection oblique.
 * Les portails visibles dans cette vue recoivent a leur tour le bit haut du stencil et leur propre vue, un seul
 * niveau plus loin. Un portail est a sens unique : vu de dos, il n'est pas dessine.
 */
public final class PortalStencilRenderer {

    /** Frustum de culling des portails visibles, reutilise d'une image a l'autre. */
    private static final Frustum CULLING_FRUSTUM = new Frustum(new Matrix4f(), new Matrix4f());

    /** Valeurs de stencil des portails du monde du joueur : 1 a 127 ; le bit haut marque un portail imbrique. */
    private static final int MAX_PORTALS_PER_FRAME = 127;
    private static final int NESTED_BIT = 0x80;
    private static final int OUTER_MASK = 0x7F;
    private static final int FULL_MASK = 0xFF;
    private static final long DEBUG_LOG_INTERVAL_MILLIS = 1000L;

    private static final List<BackroomsPortalEntity> VISIBLE = new ArrayList<>();
    private static final Matrix4f REMOTE_POSITION = new Matrix4f();
    private static final Matrix4f REMOTE_PROJECTION = new Matrix4f();
    private static final Matrix4f NESTED_POSITION = new Matrix4f();
    private static final Matrix4f NESTED_PROJECTION = new Matrix4f();
    private static long nextDebugLogMillis;

    private PortalStencilRenderer() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Point d'entree appele par le mixin de {@code WorldRenderer.render}.
     *
     * @param camera camera du joueur
     * @param positionMatrix matrice de vue du rendu monde
     * @param projectionMatrix matrice de projection du rendu monde
     * @param lightmap lightmap du rendu, recalculee pour chaque monde distant
     * @param tickDelta fraction de tick
     */
    public static void renderAllVisiblePortals(
            Camera camera,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            LightmapTextureManager lightmap,
            float tickDelta) {
        if (!ModConfig.ENABLE_PORTAL_FEATURE || !PortalRenderState.canRenderPortals()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        RemoteWorldRegistry.syncWithMainWorld(client.world);
        collectVisiblePortals(client.world, camera, positionMatrix, projectionMatrix);
        if (VISIBLE.isEmpty()) {
            return;
        }
        PortalRenderState.beginFrame();
        try {
            renderThroughStencil(client, camera, positionMatrix, projectionMatrix, lightmap, tickDelta);
        } finally {
            PortalRenderState.endFrame();
            VISIBLE.clear();
        }
    }

    private static void collectVisiblePortals(ClientWorld world, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix) {
        Vec3d cameraPos = camera.getPos();
        Vec3d look = Vec3d.fromPolar(camera.getPitch(), camera.getYaw());
        double renderDistance = Math.max(1, ModConfig.PORTAL_RENDER_DISTANCE_BLOCKS);
        boolean frustumReady = false;
        List<BackroomsPortalEntity> portals = ClientPortalIndex.portals(world);
        for (int i = 0; i < portals.size(); i++) {
            BackroomsPortalEntity portal = portals.get(i);
            if (!portal.visible() || portal.signedDistance(cameraPos) <= 0.0
                    || !PortalRelevance.isRelevant(cameraPos, look, portal.getPos(), renderDistance)) {
                continue;
            }
            if (!frustumReady) {
                ((FrustumAccessor) CULLING_FRUSTUM).backrooms$init(positionMatrix, projectionMatrix);
                CULLING_FRUSTUM.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
                frustumReady = true;
            }
            if (CULLING_FRUSTUM.isVisible(portal.detectionBox())) {
                VISIBLE.add(portal);
                if (VISIBLE.size() == MAX_PORTALS_PER_FRAME) {
                    return;
                }
            }
        }
    }

    private static void renderThroughStencil(
            MinecraftClient client,
            Camera camera,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            LightmapTextureManager lightmap,
            float tickDelta) {
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        RenderSystem.disableCull();
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, false);
        try {
            markStencils(camera.getPos(), positionMatrix);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            for (int i = 0; i < VISIBLE.size(); i++) {
                GL11.glStencilFunc(GL11.GL_EQUAL, i + 1, FULL_MASK);
                renderPortalContent(client, VISIBLE.get(i), i + 1, camera, positionMatrix, projectionMatrix, lightmap, tickDelta);
            }
            logDebugState(client);
        } finally {
            RenderSystem.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.enableDepthTest();
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            RenderSystem.enableCull();
            // Meme etat qu'a la fin du rendu vanilla : sans brouillard, sinon la main et les objets de l'interface en heritent.
            BackgroundRenderer.clearFog();
        }
    }

    /** Ecrit la valeur {@code i + 1} dans le stencil la ou la zone de vue du portail {@code i} passe le test de profondeur. */
    private static void markStencils(Vec3d cameraPos, Matrix4f positionMatrix) {
        RenderSystem.enableDepthTest();
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);
        try {
            for (int i = 0; i < VISIBLE.size(); i++) {
                GL11.glStencilFunc(GL11.GL_ALWAYS, i + 1, FULL_MASK);
                GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
                ViewAreaRenderer.draw(VISIBLE.get(i), cameraPos, positionMatrix);
            }
        } finally {
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
        }
    }

    private static void renderPortalContent(
            MinecraftClient client,
            BackroomsPortalEntity portal,
            int stencilValue,
            Camera camera,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            LightmapTextureManager lightmap,
            float tickDelta) {
        Vec3d cameraPos = camera.getPos();
        RemoteWorld view = RemoteWorldRegistry.viewOf(portal.destinationDim());
        if (view == null) {
            // Destination pas encore annoncee par le serveur : cadre sombre.
            ViewAreaRenderer.fill(portal, cameraPos, positionMatrix, 0.0f, 0.0f, 0.0f);
            return;
        }
        Vec3d remoteCameraPos = PortalViewTransform.remoteCameraPos(portal, cameraPos);
        Vec2f remoteLook = PortalViewTransform.remoteLook(portal, camera.getYaw(), camera.getPitch());
        Camera remoteCamera = PortalRenderState.remoteCamera();
        aimCamera(remoteCamera, remoteCameraPos, remoteLook, REMOTE_POSITION);
        PortalViewTransform.obliqueProjection(projectionMatrix, REMOTE_POSITION,
                portal.facingDestination().normal, portal.destinationPos(), remoteCameraPos, REMOTE_PROJECTION);

        ViewAreaRenderer.writeDepth(portal, cameraPos, positionMatrix, true);
        if (renderView(client, view, camera, remoteCamera, remoteCameraPos, REMOTE_POSITION, REMOTE_PROJECTION, projectionMatrix, lightmap, tickDelta,
                portal, cameraPos, positionMatrix)) {
            renderNestedPortals(client, stencilValue, view, camera, remoteCameraPos, remoteLook, projectionMatrix, lightmap, tickDelta);
        } else {
            ViewAreaRenderer.fill(portal, cameraPos, positionMatrix, 0.0f, 0.0f, 0.0f);
        }
        // Zone et portails imbriques reprennent la profondeur de la zone, pour ce qui se dessine ensuite devant.
        GL11.glStencilFunc(GL11.GL_EQUAL, stencilValue, OUTER_MASK);
        ViewAreaRenderer.writeDepth(portal, cameraPos, positionMatrix, false);
    }

    /**
     * Un niveau de plus : les portails visibles dans la vue sont dessines depuis la camera distante.
     * Leurs zones se projettent comme la vue qui les contient, dont la profondeur est deja ecrite.
     */
    private static void renderNestedPortals(
            MinecraftClient client,
            int outerValue,
            RemoteWorld view,
            Camera camera,
            Vec3d remoteCameraPos,
            Vec2f remoteLook,
            Matrix4f projectionMatrix,
            LightmapTextureManager lightmap,
            float tickDelta) {
        Frustum frustum = view.viewRenderer().lastFrustum();
        if (frustum == null) {
            return;
        }
        Vec3d look = Vec3d.fromPolar(remoteLook.y, remoteLook.x);
        double renderDistance = Math.max(1, ModConfig.PORTAL_RENDER_DISTANCE_BLOCKS);
        Matrix4f playerProjection = RenderSystem.getProjectionMatrix();
        VertexSorter playerSorter = RenderSystem.getVertexSorting();
        List<BackroomsPortalEntity> portals = ClientPortalIndex.portals(view.world());
        for (int i = 0; i < portals.size(); i++) {
            BackroomsPortalEntity inner = portals.get(i);
            if (!inner.visible() || inner.signedDistance(remoteCameraPos) <= 0.0
                    || !PortalRelevance.isRelevant(remoteCameraPos, look, inner.getPos(), renderDistance)
                    || !frustum.isVisible(inner.detectionBox())) {
                continue;
            }
            RenderSystem.setProjectionMatrix(REMOTE_PROJECTION, VertexSorter.BY_DISTANCE);
            try {
                renderNestedPortal(client, outerValue | NESTED_BIT, inner, camera, remoteCameraPos, remoteLook, projectionMatrix, lightmap, tickDelta);
            } finally {
                RenderSystem.setProjectionMatrix(playerProjection, playerSorter);
            }
        }
    }

    private static void renderNestedPortal(
            MinecraftClient client,
            int nestedValue,
            BackroomsPortalEntity inner,
            Camera camera,
            Vec3d remoteCameraPos,
            Vec2f remoteLook,
            Matrix4f projectionMatrix,
            LightmapTextureManager lightmap,
            float tickDelta) {
        // Le bit imbrique est pose la ou la zone interieure passe le test de profondeur contre la vue.
        GL11.glStencilFunc(GL11.GL_EQUAL, nestedValue, OUTER_MASK);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);
        try {
            ViewAreaRenderer.draw(inner, remoteCameraPos, REMOTE_POSITION);
        } finally {
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        }
        GL11.glStencilFunc(GL11.GL_EQUAL, nestedValue, FULL_MASK);

        RemoteWorld innerView = RemoteWorldRegistry.viewOf(inner.destinationDim());
        Vec3d innerCameraPos = PortalViewTransform.remoteCameraPos(inner, remoteCameraPos);
        if (innerView == null || !hasChunkAt(innerView, inner.destinationPos())) {
            // Pas de donnees pour ce que ce portail montre : cadre sombre, comme un portail trop profond.
            ViewAreaRenderer.fill(inner, remoteCameraPos, REMOTE_POSITION, 0.0f, 0.0f, 0.0f);
            return;
        }
        Vec2f innerLook = PortalViewTransform.remoteLook(inner, remoteLook.x, remoteLook.y);
        Camera nestedCamera = PortalRenderState.nestedCamera();
        aimCamera(nestedCamera, innerCameraPos, innerLook, NESTED_POSITION);
        PortalViewTransform.obliqueProjection(projectionMatrix, NESTED_POSITION,
                inner.facingDestination().normal, inner.destinationPos(), innerCameraPos, NESTED_PROJECTION);

        ViewAreaRenderer.writeDepth(inner, remoteCameraPos, REMOTE_POSITION, true);
        if (!renderView(client, innerView, camera, nestedCamera, innerCameraPos, NESTED_POSITION, NESTED_PROJECTION, projectionMatrix, lightmap, tickDelta,
                inner, remoteCameraPos, REMOTE_POSITION)) {
            ViewAreaRenderer.fill(inner, remoteCameraPos, REMOTE_POSITION, 0.0f, 0.0f, 0.0f);
        }
        // La zone interieure reprend sa profondeur dans la vue : un autre portail imbrique derriere elle n'y sera pas marque.
        ViewAreaRenderer.writeDepth(inner, remoteCameraPos, REMOTE_POSITION, false);
    }

    /** Oriente une camera de vue et en derive la matrice de vue. */
    private static void aimCamera(Camera viewCamera, Vec3d position, Vec2f look, Matrix4f positionMatrix) {
        ((CameraAccessor) viewCamera).backrooms$setPos(position);
        ((CameraAccessor) viewCamera).backrooms$setRotation(look.x, look.y);
        PortalViewTransform.positionMatrix(viewCamera, positionMatrix);
    }

    /** {@code true} si la vue possede le chunk du point d'arrivee ; sinon elle ne montrerait que du ciel. */
    private static boolean hasChunkAt(RemoteWorld view, Vec3d position) {
        return view.world().getChunkManager().isChunkLoaded(
                ChunkSectionPos.getSectionCoord(position.x), ChunkSectionPos.getSectionCoord(position.z));
    }

    /**
     * Dessine une vue avec le monde et le renderer client temporairement remplaces, pour que la lightmap, le
     * brouillard et les lectures implicites de {@code MinecraftClient.world} voient la bonne dimension. La zone
     * est d'abord remplie de la couleur de brouillard de la vue : la ou rien n'est dessine, c'est son horizon.
     *
     * @param camera camera du joueur, rendue aux dispatchers apres le rendu
     * @param viewCamera camera de la vue, deja orientee
     * @param drawProjection projection oblique de la vue, pour le dessin
     * @param cullingProjection projection du joueur, pour le choix des sections
     * @param frame portail dont la zone recoit la vue, avec la camera et la matrice de vue qui la dessinent
     */
    private static boolean renderView(
            MinecraftClient client,
            RemoteWorld view,
            Camera camera,
            Camera viewCamera,
            Vec3d viewCameraPos,
            Matrix4f positionMatrix,
            Matrix4f drawProjection,
            Matrix4f cullingProjection,
            LightmapTextureManager lightmap,
            float tickDelta,
            BackroomsPortalEntity frame,
            Vec3d frameCameraPos,
            Matrix4f framePosition) {
        MinecraftClientAccessor clientAccessor = (MinecraftClientAccessor) client;
        LightmapTextureManagerAccessor lightmapAccessor = (LightmapTextureManagerAccessor) lightmap;
        ClientWorld mainWorld = client.world;
        WorldRenderer mainRenderer = client.worldRenderer;
        try {
            clientAccessor.backrooms$setWorld(view.world());
            clientAccessor.backrooms$setWorldRenderer(view.renderer());
            lightmapAccessor.backrooms$setDirty(true);
            lightmap.update(tickDelta);
            view.viewRenderer().prepareFog(client, viewCamera, tickDelta);
            float[] fog = RenderSystem.getShaderFogColor();
            ViewAreaRenderer.fill(frame, frameCameraPos, framePosition, fog[0], fog[1], fog[2]);
            view.viewRenderer().render(client, viewCamera, viewCameraPos, positionMatrix, cullingProjection, drawProjection, tickDelta);
            return true;
        } catch (RuntimeException exception) {
            PortalRenderState.disableRemoteRendering("exception pendant le rendu distant", exception);
            return false;
        } finally {
            clientAccessor.backrooms$setWorld(mainWorld);
            clientAccessor.backrooms$setWorldRenderer(mainRenderer);
            client.getEntityRenderDispatcher().configure(mainWorld, camera, client.targetedEntity);
            client.getBlockEntityRenderDispatcher().configure(mainWorld, camera, client.crosshairTarget);
            lightmapAccessor.backrooms$setDirty(true);
            lightmap.update(tickDelta);
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private static void logDebugState(MinecraftClient client) {
        if (!ModConfig.DEBUG_PORTAL_RENDERING) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < nextDebugLogMillis) {
            return;
        }
        nextDebugLogMillis = now + DEBUG_LOG_INTERVAL_MILLIS;
        for (int i = 0; i < VISIBLE.size(); i++) {
            BackroomsPortalEntity portal = VISIBLE.get(i);
            RemoteWorld view = RemoteWorldRegistry.viewOf(portal.destinationDim());
            ModConstants.LOGGER.info("[Portal] Portail {} vers {} : {}.",
                    portal.getBlockPos().toShortString(),
                    portal.destinationDim().getValue(),
                    view == null ? "vue absente" : view.debugString());
        }
    }
}
