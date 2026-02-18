package ragamuffin.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import ragamuffin.world.Chunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders chunk meshes using LibGDX ModelBatch.
 * Supports multiple sub-meshes per chunk to handle large vertex counts.
 * Uses frustum culling to skip chunks outside the camera view.
 */
public class ChunkRenderer {

    private final Map<String, ChunkModel> chunkModels;

    public ChunkRenderer() {
        this.chunkModels = new HashMap<>();
    }

    private static class ChunkModel {
        List<Model> models;
        List<ModelInstance> instances;
        BoundingBox bounds;

        ChunkModel(float worldX, float worldY, float worldZ) {
            this.models = new ArrayList<>();
            this.instances = new ArrayList<>();
            // Bounding box for frustum culling
            this.bounds = new BoundingBox(
                new Vector3(worldX, worldY, worldZ),
                new Vector3(worldX + Chunk.SIZE, worldY + Chunk.HEIGHT, worldZ + Chunk.SIZE)
            );
        }

        void add(Model model, ModelInstance instance) {
            models.add(model);
            instances.add(instance);
        }

        void dispose() {
            for (Model m : models) {
                m.dispose();
            }
            models.clear();
            instances.clear();
        }
    }

    /**
     * Update/rebuild the mesh for a chunk.
     */
    public void updateChunk(Chunk chunk, ChunkMeshBuilder builder) {
        String key = getChunkKey(chunk);

        // Remove old model if it exists
        ChunkModel oldModel = chunkModels.remove(key);
        if (oldModel != null) {
            oldModel.dispose();
        }

        MeshData meshData = builder.build(chunk);

        if (meshData.getFaceCount() == 0) {
            return; // Empty chunk, nothing to render
        }

        float worldX = chunk.getChunkX() * Chunk.SIZE;
        float worldY = chunk.getChunkY() * Chunk.HEIGHT;
        float worldZ = chunk.getChunkZ() * Chunk.SIZE;

        ChunkModel chunkModel = new ChunkModel(worldX, worldY, worldZ);
        int meshCount = meshData.getMeshCount();

        for (int batch = 0; batch < meshCount; batch++) {
            float[] verts = meshData.getVerticesArray(batch);
            short[] inds = meshData.getIndicesArray(batch);

            if (verts.length == 0 || inds.length == 0) continue;

            Mesh mesh = new Mesh(true,
                verts.length / 12,
                inds.length,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
                new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"),
                new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "a_color")
            );

            mesh.setVertices(verts);
            mesh.setIndices(inds);

            ModelBuilder modelBuilder = new ModelBuilder();
            modelBuilder.begin();

            Material material = new Material(
                ColorAttribute.createDiffuse(Color.WHITE)
            );

            modelBuilder.part("chunk_" + batch, mesh, GL20.GL_TRIANGLES, material);
            Model model = modelBuilder.end();

            ModelInstance instance = new ModelInstance(model);
            instance.transform.setToTranslation(worldX, worldY, worldZ);

            chunkModel.add(model, instance);
        }

        if (!chunkModel.instances.isEmpty()) {
            chunkModels.put(key, chunkModel);
        }
    }

    /**
     * Remove a chunk from rendering.
     */
    public void removeChunk(Chunk chunk) {
        String key = getChunkKey(chunk);
        ChunkModel model = chunkModels.remove(key);
        if (model != null) {
            model.dispose();
        }
    }

    private String getChunkKey(Chunk chunk) {
        return chunk.getChunkX() + "," + chunk.getChunkY() + "," + chunk.getChunkZ();
    }

    /**
     * Render all chunk models with frustum culling.
     */
    public void render(ModelBatch modelBatch, Environment environment) {
        for (ChunkModel chunkModel : chunkModels.values()) {
            // Frustum culling: skip chunks outside the camera's view
            if (modelBatch.getCamera() != null &&
                !modelBatch.getCamera().frustum.boundsInFrustum(chunkModel.bounds)) {
                continue;
            }
            for (ModelInstance instance : chunkModel.instances) {
                modelBatch.render(instance, environment);
            }
        }
    }

    public void dispose() {
        for (ChunkModel chunkModel : chunkModels.values()) {
            chunkModel.dispose();
        }
        chunkModels.clear();
    }
}
