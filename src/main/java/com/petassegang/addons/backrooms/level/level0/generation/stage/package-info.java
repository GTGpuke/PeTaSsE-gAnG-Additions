/**
 * Interfaces et orchestration centrale de la pipeline du Level 0.
 *
 * <p>Ce package racine conserve volontairement les types transversaux
 * (`context`, `evaluation`, interfaces de stage, pipeline legacy) qui relient
 * plusieurs sous-familles metier sans appartenir a une seule d'entre elles.
 *
 * <p>Le terme {@code legacy} dans ces classes ne designe pas du code a jeter.
 * Il designe le coeur historique et stable de generation du Level 0 : une base
 * encore active, preservee pour garder le ressenti valide du projet.
 */
package com.petassegang.addons.backrooms.level.level0.generation.stage;
