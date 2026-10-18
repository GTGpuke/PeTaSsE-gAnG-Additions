package com.gtgpuke.gtgsbackrooms.backrooms.portal.client.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Plan de decoupe injecte dans les vertex shaders")
class ShaderClipInjectorTest {

    private static final String CORE = "assets/minecraft/shaders/core/";
    private static final String DECLARATION = "uniform vec4 backrooms_ClipPlane;";
    private static final String MAIN = "void main() {";
    private static final Pattern CLIP_LINE = Pattern.compile(
            "gl_ClipDistance\\[0\\] = dot\\(backrooms_ClipPlane, ModelViewMat \\* vec4\\((Position|Position \\+ ChunkOffset), 1\\.0\\)\\);");

    private static final String TERRAIN = """
            #version 150

            in vec3 Position;
            in vec4 Color;

            uniform mat4 ModelViewMat;
            uniform mat4 ProjMat;
            uniform vec3 ChunkOffset;

            out vec4 vertexColor;

            void main() {
                vec3 pos = Position + ChunkOffset;
                gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
                vertexColor = Color;
            }
            """;

    @Test
    @DisplayName("Tous les vertex shaders vanilla qui placent leurs sommets par ModelViewMat recoivent le plan, une fois, en tete de main")
    void vanillaVertexShadersReceiveThePlane() throws IOException {
        Map<String, String> shaders = vanillaCoreShaders();
        assertTrue(shaders.size() > 100, "Les shaders de base de vanilla sont la : " + shaders.size() + ".");
        int injected = 0;
        for (Map.Entry<String, String> entry : shaders.entrySet()) {
            String name = entry.getKey();
            String source = entry.getValue();
            String result = ShaderClipInjector.inject(source);
            if (name.endsWith(".fsh")) {
                assertSame(source, result, name + " : un fragment shader reste intact.");
                continue;
            }
            if (name.equals("blit_screen.vsh")) {
                assertFalse(ShaderClipInjector.applies(source), name + " : pas de ModelViewMat, pas de plan.");
                assertSame(source, result, name + " reste intact.");
                continue;
            }
            assertTrue(ShaderClipInjector.applies(source), name + " : tout autre vertex shader vanilla recoit le plan.");
            injected++;
            assertEquals(1, count(result, DECLARATION), name + " : l'uniforme est declare une fois.");
            Matcher clip = CLIP_LINE.matcher(result);
            assertTrue(clip.find(), name + " : la distance de decoupe est ecrite.");
            assertFalse(clip.find(), name + " : une seule fois.");
            int main = result.indexOf(MAIN);
            assertTrue(main >= 0, name + " : main inchange.");
            assertEquals(main + MAIN.length(), result.indexOf("\n    gl_ClipDistance[0]"), name + " : premiere instruction de main.");
            assertEquals(source.contains("uniform vec3 ChunkOffset;"), result.contains("vec4(Position + ChunkOffset, 1.0)"),
                    name + " : le decalage de section est pris en compte si et seulement si le shader en a un.");
            assertSame(result, ShaderClipInjector.inject(result), name + " : deja transforme, rien de plus.");
        }
        assertEquals(54, injected, "Cinquante-quatre vertex shaders de base sur cinquante-cinq.");
    }

    @Test
    @DisplayName("Un shader de terrain recoit le plan avec le decalage de section, avant toute autre instruction")
    void terrainShaderReceivesChunkOffset() {
        String result = ShaderClipInjector.inject(TERRAIN);
        assertTrue(result.startsWith("#version 150\n"), "La directive de version reste en tete.");
        String expected = DECLARATION + "\nvoid main() {\n"
                + "    gl_ClipDistance[0] = dot(backrooms_ClipPlane, ModelViewMat * vec4(Position + ChunkOffset, 1.0));\n    vec3 pos";
        assertTrue(result.contains(expected), "Uniforme juste avant main, distance en premiere instruction : " + result.replace('\n', ' ') + ".");
    }

    @Test
    @DisplayName("Sans ModelViewMat, sans Position ou sans main, la source repart telle quelle")
    void unrelatedSourcesAreUntouched() {
        String noModelView = TERRAIN.replace("uniform mat4 ModelViewMat;", "");
        String noPosition = TERRAIN.replace("in vec3 Position;", "in vec4 Position;");
        String noMain = TERRAIN.replace("void main() {", "void helper() {");
        String fragment = TERRAIN.replace("gl_Position", "fragColor");
        assertSame(noModelView, ShaderClipInjector.inject(noModelView), "Pas de ModelViewMat.");
        assertSame(noPosition, ShaderClipInjector.inject(noPosition), "Position en vec4 : un shader de post-traitement.");
        assertSame(noMain, ShaderClipInjector.inject(noMain), "Pas de main.");
        assertSame(fragment, ShaderClipInjector.inject(fragment), "Pas de gl_Position : pas un vertex shader.");
    }

    private static int count(String text, String needle) {
        int count = 0;
        for (int at = text.indexOf(needle); at >= 0; at = text.indexOf(needle, at + needle.length())) {
            count++;
        }
        return count;
    }

    /** Les shaders de base de vanilla, par nom de fichier, lus dans le jar du jeu (ou son dossier extrait). */
    private static Map<String, String> vanillaCoreShaders() throws IOException {
        URL url = ShaderClipInjector.class.getClassLoader().getResource(CORE + "position.vsh");
        assertNotNull(url, "Les shaders vanilla sont sur le classpath de test.");
        Map<String, String> shaders = new TreeMap<>();
        if ("jar".equals(url.getProtocol())) {
            // Adresse de la forme jar:file:/C:/.../minecraft-merged.jar!/assets/... : le jar est la partie avant le separateur.
            String path = url.getPath();
            Path jarPath = Path.of(URI.create(path.substring(0, path.indexOf("!/"))));
            try (JarFile jar = new JarFile(jarPath.toFile())) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(CORE) && (name.endsWith(".vsh") || name.endsWith(".fsh"))) {
                        try (InputStream in = jar.getInputStream(entry)) {
                            shaders.put(name.substring(CORE.length()), new String(in.readAllBytes(), StandardCharsets.UTF_8));
                        }
                    }
                }
            }
        } else {
            Path directory = Path.of(URI.create(url.toString())).getParent();
            try (Stream<Path> files = Files.list(directory)) {
                for (Path file : (Iterable<Path>) files::iterator) {
                    String name = file.getFileName().toString();
                    if (name.endsWith(".vsh") || name.endsWith(".fsh")) {
                        shaders.put(name, Files.readString(file));
                    }
                }
            }
        }
        return shaders;
    }
}
