package com.gtgpuke.gtgsbackrooms.core.portal.transition;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import com.gtgpuke.gtgsbackrooms.core.portal.entity.BackroomsPortalEntity;
import com.gtgpuke.gtgsbackrooms.core.portal.entity.PortalFacing;

/**
 * Garde d'une arrivee au sol : le cadre couche d'ou une entity vient de sortir vers le haut reste, pour elle, une trappe
 * fermee. Un cadre au sol s'ouvre sous qui s'y tient ({@link PortalCollision#tunnel}) : retombee sur son cadre d'arrivee,
 * elle repasserait aussitot le portail de retour et repartirait d'ou elle vient, sans fin. Tant que la garde tient, les
 * portails de ce cadre ne la font ni traverser ni tomber, et un sol mince ferme leur ouverture sous elle
 * ({@link PortalCollision#closedOpening}) : elle s'y pose, meme au-dessus du vide.
 *
 * <p>La garde tombe au tick suivant des que l'entity ne se tient plus au-dessus de l'ouverture (elle en est descendue, ou en a ete
 * poussee), qu'elle s'accroupit (on reprend ainsi une arrivee au sol, volontairement), qu'elle change de monde ou
 * qu'elle disparait. Chaque cote tient les siennes : le serveur pour toutes ses entities, le client pour son joueur, qui
 * decide de ses deplacements ; chacun la pose a la traversee qu'il voit.
 */
public final class ArrivalGuard {

    /** Hauteur, au-dessus du plan, ou des pieds se tiennent encore sur un cadre au sol : la sortie, a peine devant le plan. */
    private static final double ON_FRAME = 0.1;

    /**
     * Cadre d'arrivee garde.
     *
     * @param world monde du cadre
     * @param center centre du cadre
     * @param facing orientation du cadre, normale vers la sortie
     * @param width largeur du cadre, en blocs
     * @param height hauteur du cadre, en blocs
     */
    record Guard(World world, Vec3d center, PortalFacing facing, float width, float height) {
    }

    private ArrivalGuard() {
        throw new UnsupportedOperationException("Classe utilitaire.");
    }

    /**
     * Une entity vient de passer un portail et se trouve de l'autre cote : sortie d'un cadre au sol, elle en est gardee ;
     * toute autre arrivee leve sa garde precedente.
     *
     * @param entity entity arrivee, dans son monde d'arrivee
     * @param crossed portail passe
     */
    public static void arrive(Entity entity, BackroomsPortalEntity crossed) {
        PortalCollision.Side side = PortalCollision.side(entity.getWorld());
        if (crossed.facingDestination().normal.y > 0.0) {
            guard(side, entity, new Guard(entity.getWorld(), crossed.destinationPos(), crossed.facingDestination(), crossed.width(), crossed.height()));
        } else if (side.guards.remove(entity) != null) {
            PortalCollision.unfollow(side.guardedEntities, entity);
        }
    }

    /**
     * Cote client, apres une bascule menee par le serveur, sans portail connu : le cadre au sol sur lequel le joueur vient
     * d'etre pose, s'il y en a un, le garde comme une traversee predite.
     *
     * @param entity joueur pose a l'arrivee
     * @param portals portails de son monde
     */
    public static void arriveOnFrame(Entity entity, List<BackroomsPortalEntity> portals) {
        for (int i = 0; i < portals.size(); i++) {
            BackroomsPortalEntity portal = portals.get(i);
            double above = portal.signedDistance(entity.getX(), entity.getY(), entity.getZ());
            if (portal.getWorld() == entity.getWorld() && portal.facingSource().normal.y > 0.0 && above >= 0.0 && above <= ON_FRAME
                    && portal.withinFrameWidth(entity.getX(), entity.getY(), entity.getZ())) {
                guard(PortalCollision.side(entity.getWorld()), entity, new Guard(entity.getWorld(), portal.getPos(), portal.facingSource(), portal.width(),
                        portal.height()));
                return;
            }
        }
    }

    /** Garde cette entity, a la place de sa garde precedente s'il y en a une. */
    private static void guard(PortalCollision.Side side, Entity entity, Guard guard) {
        if (side.guards.put(entity, guard) == null) {
            side.guardedEntities.add(entity);
        }
    }

    /**
     * {@code true} si ce portail appartient au cadre d'arrivee garde pour cette entity : elle ne le traverse pas et ne tombe
     * pas dedans. Une garde qui ne tient plus (accroupie, ailleurs, plus au-dessus de l'ouverture) tombe au {@link #tick}
     * suivant.
     */
    public static boolean guards(Entity entity, BackroomsPortalEntity portal) {
        Guard guard = PortalCollision.side(entity.getWorld()).guards.get(entity);
        return guard != null && portal.getWorld() == guard.world() && portal.centeredAt(guard.center());
    }

    /** Leve les gardes qui ne tiennent plus, d'un cote, a chaque tick : meme sans aucun portail a portee. */
    public static void tick(PortalCollision.Side side) {
        List<Entity> entities = side.guardedEntities;
        for (int i = entities.size() - 1; i >= 0; i--) {
            Entity entity = entities.get(i);
            if (released(entity, side.guards.get(entity))) {
                side.guards.remove(entity);
                entities.remove(i);
            }
        }
    }

    /** Oublie toutes les gardes d'un cote (arret du serveur, deconnexion du client). */
    public static void clear(PortalCollision.Side side) {
        side.guards.clear();
        side.guardedEntities.clear();
    }

    /** Centre du cadre garde pour cette entity, ou {@code null}. */
    public static @Nullable Vec3d guardedCenter(Entity entity) {
        Guard guard = PortalCollision.side(entity.getWorld()).guards.get(entity);
        return guard == null ? null : guard.center();
    }

    private static boolean released(Entity entity, Guard guard) {
        return entity.isRemoved() || entity.getWorld() != guard.world() || entity.isSneaking()
                || !BackroomsPortalEntity.facesOpening(guard.facing(), guard.center(), guard.width(), guard.height(),
                        entity.getX(), entity.getY(), entity.getZ());
    }
}
