package com.gtgpuke.gtgsbackrooms.core.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.loot.LootTables;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import com.gtgpuke.gtgsbackrooms.MinecraftTestBootstrap;
import com.gtgpuke.gtgsbackrooms.backrooms.BackroomsConstants;
import com.gtgpuke.gtgsbackrooms.backrooms.level0.generation.LevelZeroChunkGenerator;
import com.gtgpuke.gtgsbackrooms.core.ModConstants;

@DisplayName("Registres du mod")
class ModRegistriesTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.ensure();
    }

    private static List<Identifier> modIds(Iterable<Identifier> ids) {
        List<Identifier> result = new ArrayList<>();
        for (Identifier id : ids) {
            if (ModConstants.MOD_ID.equals(id.getNamespace())) {
                result.add(id);
            }
        }
        return result;
    }

    @Test
    @DisplayName("Les blocs et items du mod sont enregistres sous les bons identifiants")
    void blocksAndItemsAreRegistered() {
        assertEquals(Identifier.of(ModConstants.MOD_ID, "level_zero_wallpaper_adaptive"),
                Registries.BLOCK.getId(ModBlocks.LEVEL_ZERO_WALLPAPER_ADAPTIVE), "Mur adaptatif.");
        assertEquals(8, modIds(Registries.BLOCK.getIds()).size(), "Huit blocs enregistres : les sept du Level 0 et le bloc portail.");
        assertEquals(8, modIds(Registries.ITEM.getIds()).size(), "Huit items enregistres : la baguette et l'item de chaque bloc du Level 0.");
        assertEquals(Identifier.of(ModConstants.MOD_ID, "item.portal_wand.place"), Registries.SOUND_EVENT.getId(ModSounds.PORTAL_WAND_PLACE),
                "Son d'un portail qui s'ouvre.");
        assertEquals(Identifier.of(ModConstants.MOD_ID, "portal_wand"), Registries.ITEM.getId(ModItems.PORTAL_WAND), "Baguette de portail.");
        assertEquals(Identifier.of(ModConstants.MOD_ID, "backrooms_portal"),
                Registries.BLOCK_ENTITY_TYPE.getId(ModBlockEntities.BACKROOMS_PORTAL), "Block entity du bloc portail.");
        assertEquals(Identifier.of(ModConstants.MOD_ID, "portal_settings"),
                Registries.DATA_COMPONENT_TYPE.getId(ModDataComponents.PORTAL_SETTINGS), "Composant des reglages de portail.");
        assertEquals(Identifier.of(ModConstants.MOD_ID, "backrooms_portal"), Registries.ENTITY_TYPE.getId(ModEntities.BACKROOMS_PORTAL), "Entity portail.");
        assertSame(LevelZeroChunkGenerator.CODEC,
                Registries.CHUNK_GENERATOR.get(Identifier.of(ModConstants.MOD_ID, BackroomsConstants.LEVEL_ZERO_CHUNK_GENERATOR)),
                "Codec du generateur du Level 0.");
        assertEquals(Identifier.of(ModConstants.MOD_ID, "main"), Registries.ITEM_GROUP.getId(ModCreativeTab.TAB), "Onglet creatif du mod.");
    }

    @Test
    @DisplayName("Chaque bloc a un item du meme identifiant, sauf le bloc portail, que pose la baguette")
    void blockItemsMatchBlocks() {
        assertEquals(Items.AIR, ModBlocks.BACKROOMS_PORTAL.asItem(), "Le bloc portail n'a pas d'item : la baguette le pose.");
        for (Identifier id : modIds(Registries.BLOCK.getIds())) {
            Block block = Registries.BLOCK.get(id);
            if (block == ModBlocks.BACKROOMS_PORTAL) {
                continue;
            }
            Item item = Registries.ITEM.get(id);
            assertTrue(item instanceof BlockItem blockItem && blockItem.getBlock() == block,
                    "Le bloc " + id + " doit avoir un BlockItem du meme identifiant.");
        }
    }

    @Test
    @DisplayName("Tous les items du mod sont dans l'onglet creatif, une seule fois")
    void creativeTabListsEveryItemOnce() {
        List<Item> entries = ModCreativeTab.ENTRIES;
        assertEquals(entries.size(), Set.copyOf(entries).size(), "Pas de doublon dans l'onglet.");
        for (Identifier id : modIds(Registries.ITEM.getIds())) {
            Item item = Registries.ITEM.get(id);
            assertTrue(entries.contains(item), "L'item " + id + " est dans l'onglet.");
        }
    }

    @Test
    @DisplayName("Chaque bloc a un blockstate, un modele et une loot table s'il rend quelque chose ; chaque item a un modele")
    void resourcesExistForEveryBlockAndItem() {
        for (Identifier id : modIds(Registries.BLOCK.getIds())) {
            assertResource("/assets/" + id.getNamespace() + "/blockstates/" + id.getPath() + ".json");
            assertResource("/assets/" + id.getNamespace() + "/models/block/" + id.getPath() + ".json");
            if (Registries.BLOCK.get(id).getLootTableKey() != LootTables.EMPTY) {
                assertResource("/data/" + id.getNamespace() + "/loot_table/blocks/" + id.getPath() + ".json");
            }
        }
        for (Identifier id : modIds(Registries.ITEM.getIds())) {
            assertResource("/assets/" + id.getNamespace() + "/models/item/" + id.getPath() + ".json");
        }
    }

    @Test
    @DisplayName("Les traductions EN et FR couvrent les memes cles et tout le contenu enregistre")
    void languageFilesAreComplete() throws IOException {
        JsonObject english = readJson("/assets/" + ModConstants.MOD_ID + "/lang/en_us.json");
        JsonObject french = readJson("/assets/" + ModConstants.MOD_ID + "/lang/fr_fr.json");
        assertEquals(english.keySet(), french.keySet(), "EN et FR doivent avoir exactement les memes cles.");
        for (Identifier id : modIds(Registries.BLOCK.getIds())) {
            assertTrue(english.has(Registries.BLOCK.get(id).getTranslationKey()), "Traduction du bloc " + id + ".");
        }
        for (Identifier id : modIds(Registries.ITEM.getIds())) {
            assertTrue(english.has(Registries.ITEM.get(id).getTranslationKey()), "Traduction de l'item " + id + ".");
        }
        assertTrue(english.has(ModEntities.BACKROOMS_PORTAL.getTranslationKey()), "Traduction de l'entity portail.");
        assertTrue(english.has("itemGroup.gtgs_backrooms.main"), "Traduction de l'onglet du mod.");
        // Chaque son du mod est declare dans sounds.json sous son chemin, avec un sous-titre traduit.
        JsonObject sounds = readJson("/assets/" + ModConstants.MOD_ID + "/sounds.json");
        List<Identifier> soundIds = modIds(Registries.SOUND_EVENT.getIds());
        assertEquals(soundIds.size(), sounds.size(), "Autant de sons declares que de sons enregistres.");
        for (Identifier id : soundIds) {
            assertTrue(sounds.has(id.getPath()), "Le son " + id + " est declare.");
            assertTrue(english.has(sounds.getAsJsonObject(id.getPath()).get("subtitle").getAsString()), "Sous-titre du son " + id + ".");
        }
        // Un item de bloc lit les cles de son bloc : une cle item.* a son nom ne servirait a rien.
        for (Identifier id : modIds(Registries.ITEM.getIds())) {
            if (Registries.ITEM.get(id) instanceof BlockItem) {
                String itemKey = "item." + id.getNamespace() + "." + id.getPath();
                assertTrue(english.keySet().stream().noneMatch(key -> key.equals(itemKey) || key.startsWith(itemKey + ".")),
                        "Aucune cle " + itemKey + " pour un item de bloc : les siennes sont sous block.*.");
            }
        }
    }

    private static void assertResource(String path) {
        try (InputStream stream = ModRegistriesTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "La ressource " + path + " doit exister.");
        } catch (IOException exception) {
            throw new AssertionError("Lecture impossible de " + path + ".", exception);
        }
    }

    private static JsonObject readJson(String path) throws IOException {
        try (InputStream stream = ModRegistriesTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "La ressource " + path + " doit exister.");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
