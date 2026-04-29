/**
 * Point d'entree de la generation du Level 0 des Backrooms.
 *
 * <p>Lecture conseillee pour comprendre l'architecture globale :
 *
 * <ol>
 *   <li>{@code LevelZeroChunkGenerator} : boucle sur les layers, appelle le layout,
 *   puis delegue l'ecriture au writer.</li>
 *   <li>{@code LevelZeroLayout} : facade historique stable qui construit l'etat
 *   logique local d'un chunk sans poser de blocs.</li>
 *   <li>{@code layout/sector} : generation brute issue de la grammaire historique
 *   du projet et du script de reference.</li>
 *   <li>{@code layout/LevelZeroRegionGrid} : transforme cette base sectorielle en
 *   vue regionale deterministe, suffisante pour extraire un chunk coherent.</li>
 *   <li>{@code stage/LevelZeroLegacyLayoutPipeline} : derive biome, topologie,
 *   micro-geometrie, motifs et lumiere a partir de la walkability regionale.</li>
 *   <li>{@code write/LevelZeroBlockWriter} : traduit l'etat logique final en blocs
 *   poses dans le chunk.</li>
 * </ol>
 *
 * <p>Le mot {@code Legacy} dans ce package ne veut pas dire {@code code mort} ni
 * {@code compatibilite temporaire}. Ici, {@code Legacy} designe le coeur stable
 * de la generation historique du Level 0 : la grammaire de layout, ses salles,
 * sa topologie et ses invariants visuels. Ce coeur ne doit pas bouger
 * silencieusement ; les evolutions autour de lui doivent au contraire le
 * preserver et l'expliciter.
 */
package com.petassegang.addons.backrooms.level.level0;
