package com.invasion.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.invasion.InvasionMod;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A textured quad mesh converted from the artist's Blender/FBX model (assets/invasion/meshes/*.json):
 * model units are 1/16 block, y up, feet at 0, facing -Z. Each quad belongs to a bone.
 */
public final class MeshModel {
    public record Quad(int texture, int bone, float[][] v, float[] normal) {}

    public enum BoneRole { BODY, LEG, HEAD }

    public final List<ResourceLocation> textures = new ArrayList<>();
    public final List<float[]> pivots = new ArrayList<>();
    public final List<BoneRole> roles = new ArrayList<>();
    public final List<Quad> quads = new ArrayList<>();
    public final int[] legIndex;
    public float height;

    private static final Map<String, MeshModel> CACHE = new HashMap<>();

    public static MeshModel get(String name) {
        return CACHE.computeIfAbsent(name, MeshModel::load);
    }

    public static void clearCache() {
        CACHE.clear();
    }

    private static MeshModel load(String name) {
        try (Reader r = new InputStreamReader(Minecraft.getInstance().getResourceManager()
                .getResourceOrThrow(InvasionMod.id("meshes/" + name + ".json")).open(), StandardCharsets.UTF_8)) {
            return new MeshModel(JsonParser.parseReader(r).getAsJsonObject());
        } catch (Exception e) {
            InvasionMod.LOGGER.error("Could not load mesh {}", name, e);
            return new MeshModel(new JsonObject());
        }
    }

    private MeshModel(JsonObject json) {
        if (json.has("textures")) for (JsonElement t : json.getAsJsonArray("textures"))
            textures.add(InvasionMod.id("textures/entity/flesh/" + t.getAsString() + ".png"));
        if (json.has("bones")) for (JsonElement b : json.getAsJsonArray("bones")) {
            JsonArray p = b.getAsJsonObject().getAsJsonArray("pivot");
            pivots.add(new float[]{p.get(0).getAsFloat(), p.get(1).getAsFloat(), p.get(2).getAsFloat()});
        }
        if (json.has("quads")) for (JsonElement e : json.getAsJsonArray("quads")) {
            JsonObject q = e.getAsJsonObject();
            JsonArray vs = q.getAsJsonArray("v");
            float[][] v = new float[4][5];
            for (int i = 0; i < 4; i++) {
                JsonArray a = vs.get(i).getAsJsonArray();
                for (int k = 0; k < 5; k++) v[i][k] = a.get(k).getAsFloat();
                height = Math.max(height, v[i][1]);
            }
            quads.add(new Quad(q.get("t").getAsInt(), q.get("b").getAsInt(), v, normal(v)));
        }
        // Classify bones: low ones are legs, the busiest upper bone is the body, other upper bones are heads.
        int n = pivots.size();
        double[] avgY = new double[n];
        int[] count = new int[n];
        for (Quad q : quads) for (float[] p : q.v) { avgY[q.bone] += p[1]; count[q.bone]++; }
        int body = -1;
        for (int i = 0; i < n; i++) {
            avgY[i] = count[i] > 0 ? avgY[i] / count[i] : 0;
            boolean leg = count[i] > 0 && avgY[i] < height * 0.38;
            roles.add(leg ? BoneRole.LEG : BoneRole.HEAD);
            if (!leg && (body < 0 || count[i] > count[body])) body = i;
        }
        if (body >= 0) roles.set(body, BoneRole.BODY);
        legIndex = new int[n];
        int legs = 0;
        for (int i = 0; i < n; i++) legIndex[i] = roles.get(i) == BoneRole.LEG ? legs++ : -1;
        // Legs swing from their top: use the highest point of each leg's own geometry as the hinge height.
        for (int i = 0; i < n; i++) {
            if (roles.get(i) != BoneRole.LEG) continue;
            float top = -1e9F, cx = 0, cz = 0; int c = 0;
            for (Quad q : quads) if (q.bone == i) for (float[] p : q.v) { top = Math.max(top, p[1]); cx += p[0]; cz += p[2]; c++; }
            if (c > 0) pivots.set(i, new float[]{cx / c, top, cz / c});
        }
    }

    private static float[] normal(float[][] v) {
        float ax = v[1][0] - v[0][0], ay = v[1][1] - v[0][1], az = v[1][2] - v[0][2];
        float bx = v[2][0] - v[0][0], by = v[2][1] - v[0][1], bz = v[2][2] - v[0][2];
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        return len < 1e-6 ? new float[]{0, 1, 0} : new float[]{nx / len, ny / len, nz / len};
    }
}
