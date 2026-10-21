package com.gtgpuke.gtgsbackrooms.core.portal.client.world;

import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormatElement;
import net.minecraft.util.math.Vec3d;

/**
 * Tampons qui coupent au plan d'un portail, sur le processeur, ce qu'on y dessine : chaque quadrilatere est rogne du cote
 * garde avant d'etre ecrit dans les tampons cibles. Pour les shaders d'un pack, qui ignorent la distance de decoupe que
 * les shaders vanilla recoivent ({@code ShaderClipInjector}). Les couches d'autres primitives que des quadrilateres
 * passent telles quelles.
 */
final class ClippedVertexConsumers implements VertexConsumerProvider {

    private final VertexConsumerProvider target;
    /** Un rogneur par couche, cree a son premier usage puis garde : il y en a autant que de couches d'entities dessinees. */
    private final Reference2ObjectOpenHashMap<RenderLayer, QuadClipper> clippers = new Reference2ObjectOpenHashMap<>();
    /** Plan garde, normale puis distance, dans le repere des sommets : relatif a la camera du monde dessine. */
    private final float[] plane = new float[4];

    ClippedVertexConsumers(VertexConsumerProvider target) {
        this.target = target;
    }

    /**
     * Garde ce qui est du cote ou pointe la normale du plan.
     *
     * @param normal normale du plan, dans le monde dessine
     * @param point point du plan, dans le monde dessine
     * @param cameraPos camera dont les sommets sont relatifs
     */
    void setPlane(Vec3d normal, Vec3d point, Vec3d cameraPos) {
        this.plane[0] = (float) normal.x;
        this.plane[1] = (float) normal.y;
        this.plane[2] = (float) normal.z;
        this.plane[3] = (float) (normal.x * (cameraPos.x - point.x) + normal.y * (cameraPos.y - point.y) + normal.z * (cameraPos.z - point.z));
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer layer) {
        VertexConsumer buffer = this.target.getBuffer(layer);
        if (layer.getDrawMode() != VertexFormat.DrawMode.QUADS) {
            return buffer;
        }
        QuadClipper clipper = this.clippers.get(layer);
        if (clipper == null) {
            clipper = new QuadClipper(layer.getVertexFormat(), this.plane);
            this.clippers.put(layer, clipper);
        }
        return clipper.wrap(buffer);
    }

    /**
     * Rogne chaque quadrilatere au plan, puis l'ecrit dans sa cible : entier s'il est du cote garde, rien s'il est de
     * l'autre, sinon le polygone coupe (trois a cinq sommets) en un ou deux quadrilateres, le dernier sommet repete pour
     * un triangle. Un format dont il ne connait pas tous les attributs passe tel quel.
     */
    static final class QuadClipper implements VertexConsumer {

        private static final int COLOR = 1;
        private static final int UV0 = 2;
        private static final int UV1 = 4;
        private static final int UV2 = 8;
        private static final int NORMAL = 16;
        /** Par sommet : position (trois), couleur (quatre), texture, overlay et lumiere (deux chacun), normale (trois). */
        private static final int STRIDE = 16;
        private static final int RED = 3;
        private static final int TEXTURE = 7;
        private static final int OVERLAY = 9;
        private static final int LIGHT = 11;
        private static final int NORMAL_X = 13;
        private static final int CORNERS = 4;
        /** Un quadrilatere coupe par un plan garde cinq sommets au plus. */
        private static final int MAX_CLIPPED = 5;

        private final float[] plane;
        private final int required;
        private final boolean clips;
        private final float[] in = new float[CORNERS * STRIDE];
        private final float[] out = new float[MAX_CLIPPED * STRIDE];
        private final float[] distances = new float[CORNERS];
        private VertexConsumer target;
        private int count;
        private int set;

        QuadClipper(VertexFormat format, float[] plane) {
            this.plane = plane;
            int mask = 0;
            boolean known = format.has(VertexFormatElement.POSITION);
            for (int i = 0; i < format.getElements().size(); i++) {
                VertexFormatElement element = format.getElements().get(i);
                if (element == VertexFormatElement.COLOR) {
                    mask |= COLOR;
                } else if (element == VertexFormatElement.UV_0) {
                    mask |= UV0;
                } else if (element == VertexFormatElement.UV_1) {
                    mask |= UV1;
                } else if (element == VertexFormatElement.UV_2) {
                    mask |= UV2;
                } else if (element == VertexFormatElement.NORMAL) {
                    mask |= NORMAL;
                } else if (element != VertexFormatElement.POSITION) {
                    known = false;
                }
            }
            this.required = mask;
            this.clips = known;
        }

        /** Ecrit desormais dans {@code buffer} : la couche vient d'etre demandee, le quadrilatere en cours repart de zero. */
        QuadClipper wrap(VertexConsumer buffer) {
            this.target = buffer;
            this.count = 0;
            this.set = 0;
            return this;
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            if (!this.clips) {
                this.target.vertex(x, y, z);
                return this;
            }
            int base = this.count * STRIDE;
            this.in[base] = x;
            this.in[base + 1] = y;
            this.in[base + 2] = z;
            this.count++;
            this.set = 0;
            completeQuad();
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            if (!this.clips) {
                this.target.color(red, green, blue, alpha);
                return this;
            }
            int base = (this.count - 1) * STRIDE + RED;
            this.in[base] = red;
            this.in[base + 1] = green;
            this.in[base + 2] = blue;
            this.in[base + 3] = alpha;
            return attributeSet(COLOR);
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            if (!this.clips) {
                this.target.texture(u, v);
                return this;
            }
            return pair(TEXTURE, u, v, UV0);
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            if (!this.clips) {
                this.target.overlay(u, v);
                return this;
            }
            return pair(OVERLAY, u, v, UV1);
        }

        @Override
        public VertexConsumer light(int u, int v) {
            if (!this.clips) {
                this.target.light(u, v);
                return this;
            }
            return pair(LIGHT, u, v, UV2);
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            if (!this.clips) {
                this.target.normal(x, y, z);
                return this;
            }
            int base = (this.count - 1) * STRIDE + NORMAL_X;
            this.in[base] = x;
            this.in[base + 1] = y;
            this.in[base + 2] = z;
            return attributeSet(NORMAL);
        }

        private VertexConsumer pair(int offset, float u, float v, int attribute) {
            int base = (this.count - 1) * STRIDE + offset;
            this.in[base] = u;
            this.in[base + 1] = v;
            return attributeSet(attribute);
        }

        private VertexConsumer attributeSet(int attribute) {
            this.set |= attribute;
            completeQuad();
            return this;
        }

        /** Le quatrieme sommet a tous les attributs de son format : le quadrilatere est rogne et ecrit. */
        private void completeQuad() {
            if (this.count == CORNERS && this.set == this.required) {
                this.count = 0;
                emit();
            }
        }

        private void emit() {
            int kept = 0;
            for (int i = 0; i < CORNERS; i++) {
                int base = i * STRIDE;
                this.distances[i] = this.plane[0] * this.in[base] + this.plane[1] * this.in[base + 1] + this.plane[2] * this.in[base + 2] + this.plane[3];
                if (this.distances[i] >= 0.0f) {
                    kept++;
                }
            }
            if (kept == CORNERS) {
                for (int i = 0; i < CORNERS; i++) {
                    write(this.in, i);
                }
                return;
            }
            if (kept == 0) {
                return;
            }
            int clipped = 0;
            for (int i = 0; i < CORNERS; i++) {
                int next = (i + 1) % CORNERS;
                boolean inside = this.distances[i] >= 0.0f;
                if (inside) {
                    System.arraycopy(this.in, i * STRIDE, this.out, clipped * STRIDE, STRIDE);
                    clipped++;
                }
                if (inside != this.distances[next] >= 0.0f) {
                    interpolate(i, next, this.distances[i] / (this.distances[i] - this.distances[next]), clipped);
                    clipped++;
                }
            }
            write(this.out, 0);
            write(this.out, 1);
            write(this.out, 2);
            write(this.out, Math.min(clipped, CORNERS) - 1);
            if (clipped == MAX_CLIPPED) {
                write(this.out, 0);
                write(this.out, 3);
                write(this.out, 4);
                write(this.out, 4);
            }
        }

        private void interpolate(int from, int to, float t, int slot) {
            int a = from * STRIDE;
            int b = to * STRIDE;
            int o = slot * STRIDE;
            for (int k = 0; k < STRIDE; k++) {
                this.out[o + k] = this.in[a + k] + (this.in[b + k] - this.in[a + k]) * t;
            }
        }

        private void write(float[] vertices, int index) {
            int base = index * STRIDE;
            this.target.vertex(vertices[base], vertices[base + 1], vertices[base + 2]);
            if ((this.required & COLOR) != 0) {
                this.target.color(Math.round(vertices[base + RED]), Math.round(vertices[base + RED + 1]),
                        Math.round(vertices[base + RED + 2]), Math.round(vertices[base + RED + 3]));
            }
            if ((this.required & UV0) != 0) {
                this.target.texture(vertices[base + TEXTURE], vertices[base + TEXTURE + 1]);
            }
            if ((this.required & UV1) != 0) {
                this.target.overlay(Math.round(vertices[base + OVERLAY]), Math.round(vertices[base + OVERLAY + 1]));
            }
            if ((this.required & UV2) != 0) {
                this.target.light(Math.round(vertices[base + LIGHT]), Math.round(vertices[base + LIGHT + 1]));
            }
            if ((this.required & NORMAL) != 0) {
                this.target.normal(vertices[base + NORMAL_X], vertices[base + NORMAL_X + 1], vertices[base + NORMAL_X + 2]);
            }
        }
    }
}
