/**
 * Representations deterministes du layout et helpers d'extraction partages
 * entre la generation regionale et l'ecriture bloc par chunk.
 *
 * <p>Lecture architecturale :
 * secteur = generation brute ;
 * region = fenetre de contexte suffisante pour evaluer un chunk ;
 * chunk = extraction locale finale prete pour le writer.
 */
package com.petassegang.addons.world.backrooms.level0.layout;
