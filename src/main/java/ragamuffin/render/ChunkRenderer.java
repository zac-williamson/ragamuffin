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
import ragamuffin.world.Chunk;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders chunk meshes using LibGDX ModelBatch.
 */
public class ChunkRenderer {

    private final Map<String, ChunkModel> chunkModels;

    public ChunkRenderer() {
        this.chunkModels = new HashMap<>();
    }

    private static class ChunkModel {
        Model model;
        ModelInstance instance;

        ChunkModel(Model model, ModelInstance instance) {
            this.model = model;
            this.instance = instance;
        }

        void dispose() {
            model.dispose();
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

        // Create LibGDX mesh with color support
        Mesh mesh = new Mesh(true,
            meshData.getVerticesArray().length / 12, // 12 floats per vertex (pos+normal+uv+color)
            meshData.getIndicesArray().length,
            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"),
            new VertexAttribute(VertexAttributes.Usage.ColorUnpacked, 4, "a_color")
        );

        mesh.setVertices(meshData.getVerticesArray());
        mesh.setIndices(meshData.getIndicesArray());

        // Create model
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        // Material that uses vertex colors
        Material material = new Material(
            ColorAttribute.createDiffuse(Color.WHITE) // Base white - vertex colors will override
        );

        modelBuilder.part("chunk", mesh, GL20.GL_TRIANGLES, material);
        Model model = modelBuilder.end();

        ModelInstance instance = new ModelInstance(model);

        // Position the model instance at the chunk's world position
        float worldX = chunk.getChunkX() * Chunk.SIZE;
        float worldY = chunk.getChunkY() * Chunk.HEIGHT;
        float worldZ = chunk.getChunkZ() * Chunk.SIZE;
        instance.transform.setToTranslation(worldX, worldY, worldZ);

        chunkModels.put(key, new ChunkModel(model, instance));
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
     * Render all chunk models.
     */
    public void render(ModelBatch modelBatch, Environment environment) {
        for (ChunkModel chunkModel : chunkModels.values()) {
            modelBatch.render(chunkModel.instance, environment);
        }
    }

    public void dispose() {
        for (ChunkModel chunkModel : chunkModels.values()) {
            chunkModel.dispose();
        }
        chunkModels.clear();
    }
}
