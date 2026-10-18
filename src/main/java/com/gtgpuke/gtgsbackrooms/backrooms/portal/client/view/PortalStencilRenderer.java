package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.FogShape;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.transition.EntityHandover;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.world.ClientPortalIndex;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.world.PortalSightReporter;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.world.RemoteWorld;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.world.RemoteWorldRegistry;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.client.world.StraddlingEntityRenderer;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.entity.PortalFacing;
import com.gtgpuke.gtgsbackrooms.backrooms.portal.transition.PortalRelevance;
import com.gtgpuke.gtgsbackrooms.config.ModConfig;
import com.gtgpuke.gtgsbackrooms.core.ModConstants;
import com.gtgpuke.gtgsbackrooms.mixin.client.BackgroundRendererAccessor;
import com.gtgpuke.gtgsbackrooms.mixin.client.CameraAccessor;
import com.gtgpuke.gtgsbackrooms.mixin.client.FrustumAccessor;
import com.gtgpuke.gtgsbackrooms.mixin.client.LightmapTextureManagerAccessor;
import com.gtgpuke.gtgsbackrooms.mixin.client.MinecraftClientAccessor;

/**
 * Rend l'autre cote de chaque portail visible, puis un niveau de portails imbriques, au milieu du rendu du monde : apres
 * ses blocs opaques et ses entities, avant ce qui est translucide. L'eau ou le verre du monde du joueur devant un
 * cadre se dessinent ensuite par-dessus la vue, comme par-dessus n'importe quel decor.
 *
 * <p>La valeur du stencil est le niveau d'imbrication : 0 pour le monde du joueur, 1 dans
 * la vue d'un portail, 2 dans celle d'un portail vu a travers lui. Les portails se dessinent un par un, du plus proche
 * au plus lointain. Pour chacun : sa zone de vue (voir {@link FrameAreaRenderer}) passe le stencil du niveau courant au
 * suivant la ou elle n'est cachee par rien, sa profondeur est poussee au plan lointain, le fond prend la couleur de
 * brouillard de la vue, puis la vue (ciel, blocs, entities, block entities, particules, nuages, pluie et neige) est dessinee depuis
 * la camera transposee avec la projection du joueur, limitee a la zone par le stencil et au plan du portail d'arrivee
 * par la decoupe des shaders ({@link PortalClipping}). Enfin la zone reprend sa vraie profondeur et son stencil revient
 * au niveau courant : le portail suivant, s'il est derriere, est cache par celui-ci, et deux portails l'un derriere
 * l'autre ne se melangent pas. Un portail est a sens unique : vu de dos, il n'est pas dessine.
 *
 * <p>A la troisieme personne, quand la camera est passee derriere un portail que le joueur vient de franchir
 * ({@link ThirdPersonPortal}), l'image entiere est la vue de l'autre cote, depuis l'image de la camera : tout l'ecran
 * passe au niveau 1, la vue s'y dessine avec ses portails imbriques, et le reste du rendu du monde du joueur est refuse
 * par le stencil jusqu'a la fin de l'image.
 */
public final class PortalStencilRenderer {

    /** Frustums de culling des portails visibles (depuis le joueur, puis depuis la camera d'une vue), reutilises d'une image a l'autre. */
    private static final Frustum CULLING_FRUSTUM = new Frustum(new Matrix4f(), new Matrix4f());
    private static final Frustum NESTED_FRUSTUM = new Frustum(new Matrix4f(), new Matrix4f());
    /** Volume visible a travers le cadre d'une vue : seuls les portails qui le touchent sont imbriques. */
    private static final ViewCone NESTED_CONE = new ViewCone();

    /** Garde-fou de performance : au-dela, les portails les plus lointains de l'image ne sont pas dessines. */
    private static final int MAX_PORTALS_PER_FRAME = 32;
    /** Niveau d'imbrication du monde du joueur ; une vue est au niveau suivant, une vue imbriquee encore un au-dela. */
    private static final int WORLD_LEVEL = 0;
    private static final int FULL_MASK = 0xFF;
    /** Distance de la camera a la troisieme personne, comme vanilla, avant l'echelle du joueur. */
    private static final float THIRD_PERSON_DISTANCE = 4.0f;
    /** Ecart des rayons qui eloignent la camera des blocs, comme vanilla : huit coins d'un petit cube autour d'elle. */
    private static final double CAMERA_CLEARANCE = 0.1;
    private static final int CAMERA_CORNERS = 8;
    private static final long DEBUG_LOG_INTERVAL_MILLIS = 1000L;

    private static final List<BackroomsPortalEntity> VISIBLE = new ArrayList<>();
    private static final Matrix4f REMOTE_POSITION = new Matrix4f();
    private static final Matrix4f NESTED_POSITION = new Matrix4f();
    /** Brouillard du monde du joueur (couleur, portee, forme), rendu apres les vues, qui posent chacune le leur. */
    private static final float[] MAIN_FOG = new float[4];
    private static float mainFogStart;
    private static float mainFogEnd;
    private static FogShape mainFogShape = FogShape.SPHERE;
    private static long nextDebugLogMillis;
    /** La vue couvre l'ecran : le stencil refuse la suite du rendu du monde du joueur jusqu'a la fin de l'image. */
    private static boolean coveringScreen;
    /** Oeil et camera voulue a la troisieme personne, calcules en cherchant le portail franchi et repris pour le dessiner. */
    private static Vec3d behindEye = Vec3d.ZERO;
    private static Vec3d behindWanted = Vec3d.ZERO;

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
        PortalSightReporter.beginFrame();
        if (!ModConfig.ENABLE_PORTAL_FEATURE || !PortalRenderState.canRenderPortals()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        RemoteWorldRegistry.syncWithMainWorld(client.world);
        if (ClientPortalIndex.allPortals(client.world).isEmpty()) {
            // Aucun portail dans ce monde, mais une entity arrivee par un portail aller simple s'y dessine encore en
            // parties : vanilla ne la dessine pas.
            if (EntityHandover.arrivalCount() > 0) {
                StraddlingEntityRenderer.renderMainWorldParts(client, camera, positionMatrix, projectionMatrix, tickDelta);
            }
            return;
        }
        BackroomsPortalEntity behind = camera.isThirdPerson() ? portalBehindCamera(client.world, camera, tickDelta) : null;
        if (behind != null) {
            PortalSightReporter.noteLookedThrough(behind);
        } else {
            collectVisiblePortals(client.world, camera, positionMatrix, projectionMatrix);
            PortalSightReporter.noteDrawn(VISIBLE);
            if (VISIBLE.isEmpty()) {
                // Aucun cadre a l'ecran, mais une entity peut etre coupee par un portail vu de cote : vanilla ne la dessine pas.
                StraddlingEntityRenderer.renderMainWorldParts(client, camera, positionMatrix, projectionMatrix, tickDelta);
                return;
            }
        }
        PortalRenderState.beginFrame();
        // Vanilla a empile sa matrice de vue pour ses entities : les zones et les vues portent chacune la leur.
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();
        try {
            if (behind != null) {
                renderFromBehind(client, behind, camera, projectionMatrix, lightmap, tickDelta);
            } else {
                renderThroughStencil(client, camera, positionMatrix, projectionMatrix, lightmap, tickDelta);
            }
        } finally {
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
            PortalRenderState.endFrame();
            VISIBLE.clear();
        }
    }

    /** Fin du rendu du monde du joueur : si la vue couvrait l'ecran, le stencil laisse de nouveau tout passer (main, interface). */
    public static void endWorldRender() {
        if (coveringScreen) {
            coveringScreen = false;
            GL11.glStencilFunc(GL11.GL_ALWAYS, WORLD_LEVEL, FULL_MASK);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
        }
    }

    /** Oeil du joueur suivi par la camera, comme {@code Camera.update} le place avant de reculer la camera. */
    private static Vec3d eyePosition(Camera camera, Entity focused, float tickDelta) {
        CameraAccessor accessor = (CameraAccessor) camera;
        return new Vec3d(
                MathHelper.lerp(tickDelta, focused.prevX, focused.getX()),
                MathHelper.lerp(tickDelta, focused.prevY, focused.getY())
                        + MathHelper.lerp(tickDelta, accessor.backrooms$getLastCameraY(), accessor.backrooms$getCameraY()),
                MathHelper.lerp(tickDelta, focused.prevZ, focused.getZ()));
    }

    /** Portail derriere lequel est passee la camera a la troisieme personne, si elle a pu y aller, ou {@code null}. */
    private static @Nullable BackroomsPortalEntity portalBehindCamera(ClientWorld world, Camera camera, float tickDelta) {
        Entity focused = camera.getFocusedEntity();
        if (focused == null) {
            return null;
        }
        Vec3d eye = eyePosition(camera, focused, tickDelta);
        Vec3d wanted = wantedCameraPos(camera, focused, eye);
        BackroomsPortalEntity portal = ThirdPersonPortal.crossedBy(ClientPortalIndex.portals(world), eye, wanted);
        if (portal == null || !portal.visible() || RemoteWorldRegistry.viewOf(portal.destinationDim()) == null) {
            return null;
        }
        // Un bloc de ce cote, entre l'oeil et le plan, a deja retenu la camera vanilla : elle ne passe pas le portail.
        double reached = camera.getPos().distanceTo(eye);
        double plane = ThirdPersonPortal.crossingFraction(portal, eye, wanted) * eye.distanceTo(wanted);
        if (reached + CAMERA_CLEARANCE < plane) {
            return null;
        }
        behindEye = eye;
        behindWanted = wanted;
        return portal;
    }

    /** Camera a sa pleine distance derriere l'oeil, avant que les blocs ne la rapprochent. */
    private static Vec3d wantedCameraPos(Camera camera, Entity focused, Vec3d eye) {
        float distance = THIRD_PERSON_DISTANCE * (focused instanceof LivingEntity living ? living.getScale() : 1.0f);
        Vector3f forward = camera.getHorizontalPlane();
        return eye.add(-forward.x() * distance, -forward.y() * distance, -forward.z() * distance);
    }

    /**
     * L'image entiere vue de l'autre cote : la camera passe le portail au point ou son segment le traverse et recule de
     * la distance qui lui reste, arretee par les blocs de l'autre monde comme vanilla l'arrete par ceux du sien.
     */
    private static void renderFromBehind(
            MinecraftClient client,
            BackroomsPortalEntity portal,
            Camera camera,
            Matrix4f projectionMatrix,
            LightmapTextureManager lightmap,
            float tickDelta) {
        RemoteWorld view = RemoteWorldRegistry.viewOf(portal.destinationDim());
        Entity focused = camera.getFocusedEntity();
        Vec3d eye = behindEye;
        Vec3d wanted = behindWanted;
        double fraction = ThirdPersonPortal.crossingFraction(portal, eye, wanted);
        Vec3d start = portal.toDestination(eye.lerp(wanted, fraction));
        Vec3d end = portal.toDestination(wanted);
        Vec3d remoteCameraPos = clipToSpace(view.world(), focused, start, end);
        Vec2f remoteLook = PortalViewTransform.remoteLook(portal, camera.getYaw(), camera.getPitch());
        Camera remoteCamera = PortalRenderState.remoteCamera();
        aimCamera(remoteCamera, remoteCameraPos, remoteLook, REMOTE_POSITION);

        saveMainFog();
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glClearStencil(WORLD_LEVEL + 1);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, false);
        GL11.glClearStencil(WORLD_LEVEL);
        GL11.glStencilFunc(GL11.GL_EQUAL, WORLD_LEVEL + 1, FULL_MASK);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        coveringScreen = true;
        try {
            if (renderView(client, view, camera, remoteCamera, remoteCameraPos, REMOTE_POSITION, projectionMatrix, lightmap, tickDelta,
                    portal, null, null)) {
                renderNestedPortals(client, null, view, camera, remoteCameraPos, remoteLook, projectionMatrix, lightmap, tickDelta);
            }
        } finally {
            // Le reste du rendu du monde du joueur (translucide, particules, nuages, meteo) est derriere la vue : refuse partout.
            GL11.glStencilFunc(GL11.GL_EQUAL, WORLD_LEVEL, FULL_MASK);
            restoreMainState(client);
        }
    }

    /**
     * Etat de rendu du monde du joueur, rendu apres les vues sur tous les chemins, exception comprise : la suite du rendu
     * vanilla (couche translucide, particules, nuages, meteo) reprend ses tests, son brouillard, sa couleur de fond et
     * l'eclairage de ses entities, que chaque vue avait remplaces par les siens.
     */
    private static void restoreMainState(MinecraftClient client) {
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        restoreMainFog();
        RenderSystem.clearColor(MAIN_FOG[0], MAIN_FOG[1], MAIN_FOG[2], 0.0f);
        applyEntityLighting(client.world);
    }

    /** Garde le brouillard du monde du joueur avant que les vues ne posent le leur. */
    private static void saveMainFog() {
        System.arraycopy(RenderSystem.getShaderFogColor(), 0, MAIN_FOG, 0, MAIN_FOG.length);
        mainFogStart = RenderSystem.getShaderFogStart();
        mainFogEnd = RenderSystem.getShaderFogEnd();
        mainFogShape = RenderSystem.getShaderFogShape();
    }

    /**
     * Rend au reste du rendu vanilla le brouillard du monde du joueur, aux shaders comme a {@code BackgroundRenderer} : les
     * nuages et la meteo y relisent la couleur, qui serait sinon celle du dernier monde vu (le noir du Level 0 sur les
     * nuages de l'Overworld).
     */
    private static void restoreMainFog() {
        RenderSystem.setShaderFogStart(mainFogStart);
        RenderSystem.setShaderFogEnd(mainFogEnd);
        RenderSystem.setShaderFogShape(mainFogShape);
        RenderSystem.setShaderFogColor(MAIN_FOG[0], MAIN_FOG[1], MAIN_FOG[2], MAIN_FOG[3]);
        BackgroundRendererAccessor.backrooms$setRed(MAIN_FOG[0]);
        BackgroundRendererAccessor.backrooms$setGreen(MAIN_FOG[1]);
        BackgroundRendererAccessor.backrooms$setBlue(MAIN_FOG[2]);
    }

    /** Recul de la camera dans l'autre monde, arrete par ses blocs : les huit rayons de {@code Camera.clipToSpace}. */
    private static Vec3d clipToSpace(ClientWorld world, Entity focused, Vec3d start, Vec3d end) {
        double length = start.distanceTo(end);
        double fraction = 1.0;
        for (int corner = 0; corner < CAMERA_CORNERS; corner++) {
            double dx = ((corner & 1) * 2 - 1) * CAMERA_CLEARANCE;
            double dy = ((corner >> 1 & 1) * 2 - 1) * CAMERA_CLEARANCE;
            double dz = ((corner >> 2 & 1) * 2 - 1) * CAMERA_CLEARANCE;
            Vec3d from = start.add(dx, dy, dz);
            HitResult hit = world.raycast(new RaycastContext(from, end.add(dx, dy, dz),
                    RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, focused));
            if (hit.getType() != HitResult.Type.MISS && length > 0.0) {
                fraction = Math.min(fraction, hit.getPos().distanceTo(from) / length);
            }
        }
        return start.lerp(end, fraction);
    }

    private static void collectVisiblePortals(ClientWorld world, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix) {
        Vec3d cameraPos = camera.getPos();
        double renderDistance = Math.max(1, ModConfig.PORTAL_RENDER_DISTANCE_BLOCKS);
        boolean frustumReady = false;
        // Sorties fugaces cachees au joueur comprises : dans le champ, il peut les voir de nouveau.
        List<BackroomsPortalEntity> portals = ClientPortalIndex.allPortals(world);
        for (int i = 0; i < portals.size(); i++) {
            BackroomsPortalEntity portal = portals.get(i);
            if (!portal.visible() || portal.signedDistance(cameraPos) <= 0.0
                    || !PortalRelevance.isRelevant(cameraPos, camera.getPitch(), camera.getYaw(), portal.getPos(), renderDistance)) {
                continue;
            }
            if (!frustumReady) {
                ((FrustumAccessor) CULLING_FRUSTUM).backrooms$init(positionMatrix, projectionMatrix);
                CULLING_FRUSTUM.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);
                frustumReady = true;
            }
            if (CULLING_FRUSTUM.isVisible(portal.detectionBox())
                    && (!portal.hiddenFromPlayer() || PortalSightReporter.revealed(world, cameraPos, portal))) {
                VISIBLE.add(portal);
            }
        }
        sortByDistance(cameraPos);
        while (VISIBLE.size() > MAX_PORTALS_PER_FRAME) {
            VISIBLE.remove(VISIBLE.size() - 1);
        }
    }

    /**
     * Du plus proche au plus lointain : la premiere vue d'un renderer dans l'image prepare son terrain, et c'est le
     * portail le plus proche, celui qu'on va traverser, qui doit le faire. Tri par insertion, sans allocation.
     */
    private static void sortByDistance(Vec3d cameraPos) {
        for (int i = 1; i < VISIBLE.size(); i++) {
            BackroomsPortalEntity portal = VISIBLE.get(i);
            double distance = portal.squaredDistanceTo(cameraPos);
            int j = i - 1;
            while (j >= 0 && VISIBLE.get(j).squaredDistanceTo(cameraPos) > distance) {
                VISIBLE.set(j + 1, VISIBLE.get(j));
                j--;
            }
            VISIBLE.set(j + 1, portal);
        }
    }

    private static void renderThroughStencil(
            MinecraftClient client,
            Camera camera,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            LightmapTextureManager lightmap,
            float tickDelta) {
        saveMainFog();
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        RenderSystem.clear(GL11.GL_STENCIL_BUFFER_BIT, false);
        try {
            Vec3d cameraPos = camera.getPos();
            for (int i = 0; i < VISIBLE.size(); i++) {
                renderPortal(client, VISIBLE.get(i), camera, cameraPos, positionMatrix, projectionMatrix, lightmap, tickDelta);
            }
            // Ce qui traverse un portail : chaque moitie de ce cote, dans le brouillard et l'eclairage du monde du joueur.
            restoreMainFog();
            applyEntityLighting(client.world);
            StraddlingEntityRenderer.renderMainWorldParts(client, camera, positionMatrix, projectionMatrix, tickDelta);
            logDebugState(client);
        } finally {
            GL11.glStencilFunc(GL11.GL_ALWAYS, WORLD_LEVEL, FULL_MASK);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            restoreMainState(client);
        }
    }

    /** Eclairage des entities propre a une dimension, comme vanilla le pose avant de les dessiner. */
    public static void applyEntityLighting(ClientWorld world) {
        if (world.getDimensionEffects().isDarkened()) {
            DiffuseLighting.enableForLevel();
        } else {
            DiffuseLighting.disableForLevel();
        }
    }

    /** Un portail du monde du joueur : zone marquee au niveau 1, vue, portails imbriques, puis zone rendue au niveau 0. */
    private static void renderPortal(
            MinecraftClient client,
            BackroomsPortalEntity portal,
            Camera camera,
            Vec3d cameraPos,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            LightmapTextureManager lightmap,
            float tickDelta) {
        markArea(portal, cameraPos, positionMatrix, WORLD_LEVEL);
        try {
            renderPortalContent(client, portal, camera, cameraPos, positionMatrix, projectionMatrix, lightmap, tickDelta);
        } finally {
            restoreArea(portal, cameraPos, positionMatrix, WORLD_LEVEL);
        }
    }

    /**
     * Fait passer la zone de vue du niveau {@code level} au suivant, la ou elle passe le test de profondeur ; une
     * meme zone ne compte qu'une fois par pixel, le stencil ne valant plus {@code level} apres la premiere face.
     */
    private static void markArea(BackroomsPortalEntity portal, Vec3d cameraPos, Matrix4f positionMatrix, int level) {
        GL11.glStencilFunc(GL11.GL_EQUAL, level, FULL_MASK);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_INCR);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(false);
        try {
            FrameAreaRenderer.draw(portal, cameraPos, positionMatrix);
        } finally {
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.depthMask(true);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
        }
        GL11.glStencilFunc(GL11.GL_EQUAL, level + 1, FULL_MASK);
    }

    /**
     * La zone marquee reprend sa vraie profondeur (la face la plus proche) et redescend au niveau {@code level} : ce qui
     * se dessine ensuite a ce niveau la voit comme une surface opaque, et un portail plus lointain y reste cache.
     */
    private static void restoreArea(BackroomsPortalEntity portal, Vec3d cameraPos, Matrix4f positionMatrix, int level) {
        GL11.glStencilFunc(GL11.GL_EQUAL, level + 1, FULL_MASK);
        FrameAreaRenderer.writeDepth(portal, cameraPos, positionMatrix, true);
        FrameAreaRenderer.writeDepth(portal, cameraPos, positionMatrix, false);
        // Sans test de profondeur, chaque pixel de la zone redescend une fois : il ne vaut plus level + 1 ensuite.
        RenderSystem.disableDepthTest();
        RenderSystem.colorMask(false, false, false, false);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_DECR);
        try {
            FrameAreaRenderer.draw(portal, cameraPos, positionMatrix);
        } finally {
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            RenderSystem.colorMask(true, true, true, true);
            RenderSystem.enableDepthTest();
        }
        GL11.glStencilFunc(GL11.GL_EQUAL, level, FULL_MASK);
    }

    private static void renderPortalContent(
            MinecraftClient client,
            BackroomsPortalEntity portal,
            Camera camera,
            Vec3d cameraPos,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            LightmapTextureManager lightmap,
            float tickDelta) {
        RemoteWorld view = RemoteWorldRegistry.viewOf(portal.destinationDim());
        if (view == null) {
            // Destination pas encore annoncee par le serveur : cadre sombre.
            FrameAreaRenderer.fill(portal, cameraPos, positionMatrix, 0.0f, 0.0f, 0.0f);
            return;
        }
        Vec3d remoteCameraPos = portal.toDestination(cameraPos);
        Vec2f remoteLook = PortalViewTransform.remoteLook(portal, camera.getYaw(), camera.getPitch());
        Camera remoteCamera = PortalRenderState.remoteCamera();
        aimCamera(remoteCamera, remoteCameraPos, remoteLook, REMOTE_POSITION);

        FrameAreaRenderer.writeDepth(portal, cameraPos, positionMatrix, true);
        if (renderView(client, view, camera, remoteCamera, remoteCameraPos, REMOTE_POSITION, projectionMatrix, lightmap, tickDelta,
                portal, cameraPos, positionMatrix)) {
            renderNestedPortals(client, portal, view, camera, remoteCameraPos, remoteLook, projectionMatrix, lightmap, tickDelta);
        } else {
            FrameAreaRenderer.fill(portal, cameraPos, positionMatrix, 0.0f, 0.0f, 0.0f);
        }
    }

    /**
     * Un niveau de plus : les portails visibles dans la vue, depuis sa camera et a travers son cadre, sont dessines a
     * leur tour, sous le plan de decoupe de la vue qui les contient, dont la profondeur est deja ecrite. Sans portail
     * exterieur ({@code outer} nul), la vue couvre l'ecran : tous les portails qu'y voit sa camera comptent.
     */
    private static void renderNestedPortals(
            MinecraftClient client,
            @Nullable BackroomsPortalEntity outer,
            RemoteWorld view,
            Camera camera,
            Vec3d remoteCameraPos,
            Vec2f remoteLook,
            Matrix4f projectionMatrix,
            LightmapTextureManager lightmap,
            float tickDelta) {
        ((FrustumAccessor) NESTED_FRUSTUM).backrooms$init(REMOTE_POSITION, projectionMatrix);
        NESTED_FRUSTUM.setPosition(remoteCameraPos.x, remoteCameraPos.y, remoteCameraPos.z);
        if (outer != null) {
            PortalFacing arrival = outer.facingDestination();
            NESTED_CONE.set(remoteCameraPos, outer.destinationPos(), arrival.axisW, arrival.axisH, arrival.normal,
                    outer.width() / 2.0, outer.height() / 2.0);
            PortalClipping.push(arrival.normal, outer.destinationPos(), remoteCameraPos, REMOTE_POSITION);
        } else {
            NESTED_CONE.unbounded();
        }
        double renderDistance = Math.max(1, ModConfig.PORTAL_RENDER_DISTANCE_BLOCKS);
        List<BackroomsPortalEntity> portals = ClientPortalIndex.portals(view.world());
        int level = WORLD_LEVEL + 1;
        try {
            for (int i = 0; i < portals.size(); i++) {
                BackroomsPortalEntity inner = portals.get(i);
                // Les faces du portail d'arrivee lui-meme sont dans le plan de decoupe : celle qui regarde la camera de la
                // vue (la face arriere d'un portail double face) recouvrirait toute la vue par l'autre cote.
                if (outer != null && inner.centeredAt(outer.destinationPos())) {
                    continue;
                }
                if (!inner.visible() || inner.signedDistance(remoteCameraPos) <= 0.0
                        || !PortalRelevance.isRelevant(remoteCameraPos, remoteLook.y, remoteLook.x, inner.getPos(), renderDistance)
                        || !NESTED_FRUSTUM.isVisible(inner.detectionBox())
                        || !NESTED_CONE.intersects(inner.detectionBox())) {
                    continue;
                }
                markArea(inner, remoteCameraPos, REMOTE_POSITION, level);
                try {
                    renderNestedPortal(client, inner, camera, remoteCameraPos, remoteLook, projectionMatrix, lightmap, tickDelta);
                } finally {
                    restoreArea(inner, remoteCameraPos, REMOTE_POSITION, level);
                }
            }
        } finally {
            if (outer != null) {
                PortalClipping.pop();
            }
        }
    }

    private static void renderNestedPortal(
            MinecraftClient client,
            BackroomsPortalEntity inner,
            Camera camera,
            Vec3d remoteCameraPos,
            Vec2f remoteLook,
            Matrix4f projectionMatrix,
            LightmapTextureManager lightmap,
            float tickDelta) {
        RemoteWorld innerView = RemoteWorldRegistry.viewOf(inner.destinationDim());
        if (innerView == null || !hasChunkAt(innerView, inner.destinationPos())) {
            // Pas de donnees pour ce que ce portail montre : cadre sombre, comme un portail trop profond.
            FrameAreaRenderer.fill(inner, remoteCameraPos, REMOTE_POSITION, 0.0f, 0.0f, 0.0f);
            return;
        }
        Vec3d innerCameraPos = inner.toDestination(remoteCameraPos);
        Vec2f innerLook = PortalViewTransform.remoteLook(inner, remoteLook.x, remoteLook.y);
        Camera nestedCamera = PortalRenderState.nestedCamera();
        aimCamera(nestedCamera, innerCameraPos, innerLook, NESTED_POSITION);

        FrameAreaRenderer.writeDepth(inner, remoteCameraPos, REMOTE_POSITION, true);
        if (!renderView(client, innerView, camera, nestedCamera, innerCameraPos, NESTED_POSITION, projectionMatrix, lightmap, tickDelta,
                inner, remoteCameraPos, REMOTE_POSITION)) {
            FrameAreaRenderer.fill(inner, remoteCameraPos, REMOTE_POSITION, 0.0f, 0.0f, 0.0f);
        }
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
     * @param client client, dont le monde et le renderer sont substitues le temps du dessin
     * @param view monde de la vue
     * @param camera camera du joueur, rendue aux dispatchers apres le rendu
     * @param viewCamera camera de la vue, deja orientee
     * @param viewCameraPos position de la camera de la vue
     * @param positionMatrix matrice de vue de la camera de la vue
     * @param projection projection du joueur, la seule : le plan du portail d'arrivee est coupe par les shaders
     * @param lightmap lightmap du rendu, recalculee pour le monde de la vue
     * @param tickDelta fraction de tick de l'image
     * @param frame portail dont c'est la vue
     * @param frameCameraPos camera qui dessine la zone du cadre ; {@code null} quand la vue couvre l'ecran
     * @param framePosition matrice de vue de cette camera ; {@code null} quand la vue couvre l'ecran
     * @return {@code true} si la vue a ete dessinee ; {@code false} si elle a echoue et que le rendu distant est coupe
     */
    private static boolean renderView(
            MinecraftClient client,
            RemoteWorld view,
            Camera camera,
            Camera viewCamera,
            Vec3d viewCameraPos,
            Matrix4f positionMatrix,
            Matrix4f projection,
            LightmapTextureManager lightmap,
            float tickDelta,
            BackroomsPortalEntity frame,
            @Nullable Vec3d frameCameraPos,
            @Nullable Matrix4f framePosition) {
        MinecraftClientAccessor clientAccessor = (MinecraftClientAccessor) client;
        LightmapTextureManagerAccessor lightmapAccessor = (LightmapTextureManagerAccessor) lightmap;
        ClientWorld mainWorld = client.world;
        WorldRenderer mainRenderer = client.worldRenderer;
        try {
            client.world = view.world();
            clientAccessor.backrooms$setWorldRenderer(view.renderer());
            lightmapAccessor.backrooms$setDirty(true);
            lightmap.update(tickDelta);
            view.viewRenderer().prepareFog(client, viewCamera, tickDelta);
            boolean throughFrame = frameCameraPos != null && framePosition != null;
            if (throughFrame) {
                float[] fog = RenderSystem.getShaderFogColor();
                FrameAreaRenderer.fill(frame, frameCameraPos, framePosition, fog[0], fog[1], fog[2]);
            } else {
                // La vue couvre l'ecran : couleur de son brouillard (posee par prepareFog) et profondeur remises a zero partout.
                RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT, MinecraftClient.IS_SYSTEM_MAC);
            }
            view.viewRenderer().render(client, viewCamera, viewCameraPos, positionMatrix, projection, frame, frameCameraPos, lightmap, tickDelta);
            return true;
        } catch (RuntimeException exception) {
            PortalRenderState.disableRemoteRendering("exception pendant le rendu distant", exception);
            return false;
        } finally {
            client.world = mainWorld;
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
