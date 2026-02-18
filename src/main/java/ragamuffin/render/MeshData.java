package ragamuffin.render;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds mesh data for a chunk (vertices, indices, etc.)
 * Automatically splits into sub-meshes when vertex count approaches
 * the unsigned short index limit (65535).
 */
public class MeshData {
    private static final int MAX_VERTICES_PER_MESH = 65532; // Leave room for a full quad (4 verts)
    private static final int FLOATS_PER_VERTEX = 12;

    private List<float[]> vertexBatches;
    private List<short[]> indexBatches;

    private List<Float> currentVertices;
    private List<Short> currentIndices;
    private int currentVertexCount;
    private int totalFaceCount;

    public MeshData() {
        this.vertexBatches = new ArrayList<>();
        this.indexBatches = new ArrayList<>();
        this.currentVertices = new ArrayList<>();
        this.currentIndices = new ArrayList<>();
        this.currentVertexCount = 0;
        this.totalFaceCount = 0;
    }

    public void addQuad(float[] quadVertices, short[] quadIndices, int baseIndex) {
        // If adding this quad would exceed the limit, flush current batch and start new one
        if (currentVertexCount + 4 > MAX_VERTICES_PER_MESH) {
            flushCurrentBatch();
            // Reset baseIndex for the new batch — caller tracks global index,
            // but we need local indices within this batch
            baseIndex = 0;
        }

        // Use local vertex index within current batch
        int localBase = currentVertexCount;
        for (float v : quadVertices) {
            currentVertices.add(v);
        }
        for (short i : quadIndices) {
            currentIndices.add((short)(i + localBase));
        }
        currentVertexCount += 4;
        totalFaceCount++;
    }

    private void flushCurrentBatch() {
        if (currentVertexCount > 0) {
            float[] verts = new float[currentVertices.size()];
            for (int i = 0; i < currentVertices.size(); i++) {
                verts[i] = currentVertices.get(i);
            }
            short[] inds = new short[currentIndices.size()];
            for (int i = 0; i < currentIndices.size(); i++) {
                inds[i] = currentIndices.get(i);
            }
            vertexBatches.add(verts);
            indexBatches.add(inds);
        }
        currentVertices = new ArrayList<>();
        currentIndices = new ArrayList<>();
        currentVertexCount = 0;
    }

    /**
     * Get the number of sub-meshes needed to render this chunk.
     */
    public int getMeshCount() {
        finalize_();
        return vertexBatches.size();
    }

    /**
     * Ensure all data is flushed to batches.
     */
    private boolean finalized = false;
    private void finalize_() {
        if (!finalized) {
            flushCurrentBatch();
            finalized = true;
        }
    }

    public int getFaceCount() {
        return totalFaceCount;
    }

    // Legacy single-mesh accessors (for backwards compatibility with tests)
    public float[] getVerticesArray() {
        finalize_();
        if (vertexBatches.isEmpty()) return new float[0];
        return vertexBatches.get(0);
    }

    public short[] getIndicesArray() {
        finalize_();
        if (indexBatches.isEmpty()) return new short[0];
        return indexBatches.get(0);
    }

    /**
     * Get vertices for a specific sub-mesh batch.
     */
    public float[] getVerticesArray(int batchIndex) {
        finalize_();
        return vertexBatches.get(batchIndex);
    }

    /**
     * Get indices for a specific sub-mesh batch.
     */
    public short[] getIndicesArray(int batchIndex) {
        finalize_();
        return indexBatches.get(batchIndex);
    }
}
