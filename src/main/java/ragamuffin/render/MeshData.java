package ragamuffin.render;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds mesh data for a chunk (vertices, indices, etc.)
 * Automatically splits into sub-meshes when vertex count approaches
 * the unsigned short index limit (65535).
 * Uses primitive arrays internally to avoid boxing overhead.
 *
 * Transparent faces (e.g. glass) are stored separately so they can be
 * rendered after all opaque geometry with alpha blending enabled.
 */
public class MeshData {
    private static final int MAX_VERTICES_PER_MESH = 65532; // Leave room for a full quad (4 verts)
    private static final int FLOATS_PER_VERTEX = 12;
    private static final int INITIAL_CAPACITY = 4096; // quads

    private List<float[]> vertexBatches;
    private List<short[]> indexBatches;

    // Current batch — primitive arrays with manual length tracking
    private float[] currentVertices;
    private short[] currentIndices;
    private int vertexFloatCount;  // number of floats written
    private int indexCount;        // number of indices written
    private int currentVertexCount; // number of vertices (floats / 12)
    private int totalFaceCount;

    // Separate storage for transparent faces (rendered with alpha blending)
    private final MeshData transparentData;
    private final boolean isTransparentStore;

    public MeshData() {
        this.vertexBatches = new ArrayList<>();
        this.indexBatches = new ArrayList<>();
        this.currentVertices = new float[INITIAL_CAPACITY * 4 * FLOATS_PER_VERTEX];
        this.currentIndices = new short[INITIAL_CAPACITY * 6];
        this.vertexFloatCount = 0;
        this.indexCount = 0;
        this.currentVertexCount = 0;
        this.totalFaceCount = 0;
        this.isTransparentStore = false;
        this.transparentData = new MeshData(true);
    }

    /** Private constructor for the transparent sub-store (avoids infinite recursion). */
    private MeshData(boolean isTransparentStore) {
        this.vertexBatches = new ArrayList<>();
        this.indexBatches = new ArrayList<>();
        this.currentVertices = new float[256 * 4 * FLOATS_PER_VERTEX]; // smaller initial capacity for transparent
        this.currentIndices = new short[256 * 6];
        this.vertexFloatCount = 0;
        this.indexCount = 0;
        this.currentVertexCount = 0;
        this.totalFaceCount = 0;
        this.isTransparentStore = true;
        this.transparentData = null;
    }

    /**
     * Returns the transparent sub-mesh data (for GLASS and other alpha-blended blocks).
     * Faces added via {@link #addQuadTransparent} are stored here.
     */
    public MeshData getTransparentMeshData() {
        return transparentData;
    }

    /**
     * Add a transparent quad (e.g. a glass face) to the separate transparent sub-mesh.
     * These faces will be rendered after all opaque geometry with alpha blending.
     */
    public void addQuadTransparent(float[] quadVertices, short[] quadIndices, int baseIndex) {
        if (transparentData != null) {
            transparentData.addQuad(quadVertices, quadIndices, baseIndex);
        }
    }

    public void addQuad(float[] quadVertices, short[] quadIndices, int baseIndex) {
        // If adding this quad would exceed the limit, flush current batch and start new one
        if (currentVertexCount + 4 > MAX_VERTICES_PER_MESH) {
            flushCurrentBatch();
            baseIndex = 0;
        }

        // Ensure capacity for vertices
        int neededFloats = vertexFloatCount + quadVertices.length;
        if (neededFloats > currentVertices.length) {
            int newLen = Math.max(currentVertices.length * 2, neededFloats);
            float[] bigger = new float[newLen];
            System.arraycopy(currentVertices, 0, bigger, 0, vertexFloatCount);
            currentVertices = bigger;
        }

        // Ensure capacity for indices
        int neededIndices = indexCount + quadIndices.length;
        if (neededIndices > currentIndices.length) {
            int newLen = Math.max(currentIndices.length * 2, neededIndices);
            short[] bigger = new short[newLen];
            System.arraycopy(currentIndices, 0, bigger, 0, indexCount);
            currentIndices = bigger;
        }

        // Copy vertex data
        System.arraycopy(quadVertices, 0, currentVertices, vertexFloatCount, quadVertices.length);
        vertexFloatCount += quadVertices.length;

        // Copy index data with local base offset
        int localBase = currentVertexCount;
        for (int i = 0; i < quadIndices.length; i++) {
            currentIndices[indexCount++] = (short)(quadIndices[i] + localBase);
        }
        currentVertexCount += 4;
        totalFaceCount++;
    }

    private void flushCurrentBatch() {
        if (currentVertexCount > 0) {
            float[] verts = new float[vertexFloatCount];
            System.arraycopy(currentVertices, 0, verts, 0, vertexFloatCount);
            short[] inds = new short[indexCount];
            System.arraycopy(currentIndices, 0, inds, 0, indexCount);
            vertexBatches.add(verts);
            indexBatches.add(inds);
        }
        // Reset for next batch — reuse arrays
        vertexFloatCount = 0;
        indexCount = 0;
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
