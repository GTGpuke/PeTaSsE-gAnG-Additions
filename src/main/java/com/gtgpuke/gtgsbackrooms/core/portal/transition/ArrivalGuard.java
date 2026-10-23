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
 * fermee, le temps d'en descendre. Un cadre au sol s'ouvre sous qui s'y tient ({@link PortalCollision#tunnel}) : retombee
 * sur son cadre d'arrivee, elle repasserait aussitot le portail de retour et repartirait d'ou elle vient. Tant que la garde
 * tient, les portails de ce cadre ne la font ni traverser ni tomber, et un sol mince ferme leur ouverture sous elle
 * ({@link PortalCollision#closedOpening}) : elle s'y pose, meme au-dessus du vide.
 *
 * <p>La garde tombe au tick suivant des que l'entity ne se tient plus au-dessus de l'ouverture (elle en est descendue, ou en a ete
 * poussee), qu'elle s'accroupit (on reprend ainsi une arrivee au sol, volontairement), qu'elle change de monde ou
 * qu'elle disparait, et {@value #GRACE_TICKS} ticks apres s'etre posee sur le cadre : qui y reste, ou y marche sans y
 * prendre garde, y retombe. Le repit court de la pose, pas de la sortie : qui en jaillit avec son elan retombe dessus
 * avant qu'il ne commence. Chaque cote tient les siennes : le serveur pour toutes ses entities, le client pour son joueur,
 * qui decide de ses deplacements ; chacun la pose a la traversee qu'il voit.
 */
public final class ArrivalGuard {

    /** Ticks pendant lesquels un cadre d'arrivee au sol porte l'entity qui s'y est posee, avant de s'ouvrir sous elle. */
    public static final int GRACE_TICKS = 20;
    /** Hauteur, au-dessus du plan, ou des pieds se tiennent encore sur un cadre au sol : la sortie, a peine devant le plan. */
    private static final double ON_FRAME = 0.1;
    /** Heure de pose d'une garde dont l'entity n'a pas encore touche le cadre (elle jaillit de l'arrivee). */
    private static final long NOT_LANDED = -1L;

    /**
     * Cadre d'arrivee garde.
     *
     * @param world monde du cadre
     * @param center centre du cadre
     * @param facing orientation du cadre, normale vers la sortie
     * @param width largeur du cadre, en blocs
     * @param height hauteur du cadre, en blocs
     * @param landedAt heure du monde ou l'entity s'est posee sur le cadre ; {@link #NOT_LANDED} tant qu'elle ne l'a pas touche
     */
    record Guard(World world, Vec3d center, PortalFacing facing, float width, float height, long landedAt) {

        /** Meme garde, posee a cette heure. */
        Guard landed(long time) {
            return new Guard(this.world, this.center, this.facing, this.width, this.height, time);
        }
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
            guard(side, entity, new Guard(entity.getWorld(), crossed.destinationPos(), crossed.facingDestination(), crossed.width(), crossed.height(),
                    NOT_LANDED));
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
                        portal.height(), NOT_LANDED));
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
     * pas dedans. Une garde qui ne tient plus (accroupie, ailleurs, plus au-dessus de l'ouverture, posee depuis
     * {@value #GRACE_TICKS} ticks) tombe au {@link #tick} suivant.
     */
    public static boolean guards(Entity entity, BackroomsPortalEntity portal) {
        Guard guard = PortalCollision.side(entity.getWorld()).guards.get(entity);
        return guard != null && portal.getWorld() == guard.world() && portal.centeredAt(guard.center());
    }

    /**
     * Leve les gardes qui ne tiennent plus, d'un cote, a chaque tick : meme sans aucun portail a portee. Une entity gardee
     * qui touche le sol pour la premiere fois s'est posee sur le cadre : son repit commence.
     */
    public static void tick(PortalCollision.Side side) {
        List<Entity> entities = side.guardedEntities;
        for (int i = entities.size() - 1; i >= 0; i--) {
            Entity entity = entities.get(i);
            Guard guard = side.guards.get(entity);
            if (released(entity, guard)) {
                side.guards.remove(entity);
                entities.remove(i);
            } else if (guard.landedAt() == NOT_LANDED && entity.isOnGround()) {
                // Une garde neuve, une fois par arrivee.
                side.guards.put(entity, guard.landed(entity.getWorld().getTime()));
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
                || guard.landedAt() != NOT_LANDED && entity.getWorld().getTime() - guard.landedAt() >= GRACE_TICKS
                || !BackroomsPortalEntity.facesOpening(guard.facing(), guard.center(), guard.width(), guard.height(),
                        entity.getX(), entity.getY(), entity.getZ());
    }
}
