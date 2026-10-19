package com.gtgpuke.gtgsbackrooms.mixin.client;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;

/**
 * Positionne et oriente une camera du mod (camera distante du rendu de portail, camera des eclaboussures de pluie d'une
 * vue) ; hauteur des yeux lissee de la camera du joueur, d'ou part sa camera a la troisieme personne. Sous un pack de
 * shaders, la camera du joueur prend la place de celle d'une vue le temps de son rendu complet (position, rotation,
 * monde lu), et la camera de la vue suit le joueur a la troisieme personne ; sans pack, le temps du dessin des nuages
 * d'une vue (position et monde lu).
 */
@Mixin(Camera.class)
public interface CameraAccessor {

    @Invoker("setPos")
    void backrooms$setPos(Vec3d pos);

    @Invoker("setRotation")
    void backrooms$setRotation(float yaw, float pitch);

    @Accessor("cameraY")
    float backrooms$getCameraY();

    @Accessor("lastCameraY")
    float backrooms$getLastCameraY();

    @Accessor("area")
    BlockView backrooms$getArea();

    @Accessor("area")
    void backrooms$setArea(BlockView area);

    @Accessor("focusedEntity")
    void backrooms$setFocusedEntity(@Nullable Entity entity);

    @Accessor("thirdPerson")
    void backrooms$setThirdPerson(boolean thirdPerson);
}
