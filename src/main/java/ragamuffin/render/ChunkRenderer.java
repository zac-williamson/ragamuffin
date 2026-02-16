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

import java.util.ArrayList;
import java.util.List;

/**
 * Renders chunk meshes using LibGDX ModelBatch.
 */
public class ChunkRenderer {

    private final List<ModelInstance> chunkModels;
    private final List<Model> models;

    public ChunkRenderer() {
        this.chunkModels = new ArrayList<>();
        this.models = new ArrayList<>();
    }

    /**
     * Update/rebuild the mesh for a chunk.
     */
    public void updateChunk(Chunk chunk, ChunkMeshBuilder builder) {
        MeshData meshData = builder.build(chunk);

        if (meshData.getFaceCount() == 0) {
            return; // Empty chunk, nothing to render
        }

        // Create LibGDX mesh
        Mesh mesh = new Mesh(true,
            meshData.getVerticesArray().length / 8, // 8 floats per vertex
            meshData.getIndicesArray().length,
            new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
            new VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0")
        );

        mesh.setVertices(meshData.getVerticesArray());
        mesh.setIndices(meshData.getIndicesArray());

        // Create model
        ModelBuilder modelBuilder = new ModelBuilder();
        modelBuilder.begin();

        // Simple colored material for now (Phase 1)
        Material material = new Material(
            ColorAttribute.createDiffuse(new Color(0.4f, 0.8f, 0.3f, 1f))
        );

        modelBuilder.part("chunk", mesh, GL20.GL_TRIANGLES, material);
        Model model = modelBuilder.end();

        ModelInstance instance = new ModelInstance(model);
        chunkModels.add(instance);
        models.add(model);
    }

    /**
     * Render all chunk models.
     */
    public void render(ModelBatch modelBatch, Environment environment) {
        for (ModelInstance instance : chunkModels) {
            modelBatch.render(instance, environment);
        }
    }

    public void dispose() {
        for (Model model : models) {
            model.dispose();
        }
        models.clear();
        chunkModels.clear();
    }
}
