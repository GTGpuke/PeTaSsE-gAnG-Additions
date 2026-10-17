package com.petassegang.addons;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Chaque mixin du mod s'applique a ses classes cibles. Les tests en jeu tournent sur un serveur sans client et
 * ne chargent jamais les classes client : sans ce test, un mixin client casse (cible, methode ou champ introuvable) ne
 * se verrait qu'au lancement du client. Les tests unitaires tournent sous le chargeur Fabric, cote client : charger ici chaque
 * cible applique ses mixins, et l'erreur fait echouer le build.
 */
@DisplayName("Application des mixins a leurs cibles")
class MixinTargetsTest {

    private static final String CONFIG = "petasse_gang_additions.mixins.json";
    private static final String MIXIN_ANNOTATION = "Lorg/spongepowered/asm/mixin/Mixin;";

    /** Mixins declares dans la configuration, communs et client, en noms de classe complets. */
    static Stream<String> mixins() throws IOException {
        ClassLoader loader = MixinTargetsTest.class.getClassLoader();
        try (InputStream in = loader.getResourceAsStream(CONFIG)) {
            assertNotNull(in, "Configuration des mixins sur le classpath.");
            JsonObject config = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            String prefix = config.get("package").getAsString() + ".";
            List<String> names = new ArrayList<>();
            for (String section : new String[] {"mixins", "client"}) {
                JsonArray entries = config.getAsJsonArray(section);
                for (int i = 0; i < entries.size(); i++) {
                    names.add(prefix + entries.get(i).getAsString());
                }
            }
            return names.stream();
        }
    }

    @ParameterizedTest
    @MethodSource("mixins")
    @DisplayName("Charger la cible applique le mixin sans erreur")
    void appliesToItsTargets(String mixin) throws IOException {
        ClassLoader loader = MixinTargetsTest.class.getClassLoader();
        List<String> targets = targets(loader, mixin);
        assertFalse(targets.isEmpty(), mixin + " declare au moins une cible.");
        for (String target : targets) {
            // Charger sans initialiser suffit : c'est au chargement que Mixin transforme la classe.
            assertDoesNotThrow(() -> Class.forName(target, false, loader), mixin + " s'applique a " + target + ".");
        }
    }

    /** Cibles de l'annotation {@code @Mixin}, lues dans le bytecode : une classe mixin ne se charge pas directement. */
    private static List<String> targets(ClassLoader loader, String mixin) throws IOException {
        List<String> targets = new ArrayList<>();
        try (InputStream in = loader.getResourceAsStream(mixin.replace('.', '/') + ".class")) {
            assertNotNull(in, mixin + " est compile.");
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    if (!MIXIN_ANNOTATION.equals(descriptor)) {
                        return null;
                    }
                    return new AnnotationVisitor(Opcodes.ASM9) {
                        @Override
                        public AnnotationVisitor visitArray(String name) {
                            return new AnnotationVisitor(Opcodes.ASM9) {
                                @Override
                                public void visit(String unused, Object value) {
                                    if (value instanceof Type type) {
                                        targets.add(type.getClassName());
                                    } else if (value instanceof String target) {
                                        targets.add(target.replace('/', '.'));
                                    }
                                }
                            };
                        }
                    };
                }
            }, ClassReader.SKIP_CODE);
        }
        return targets;
    }
}
