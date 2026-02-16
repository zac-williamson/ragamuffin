package ragamuffin.render;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds mesh data for a chunk (vertices, indices, etc.)
 */
public class MeshData {
    private final List<Float> vertices;
    private final List<Short> indices;
    private int faceCount;

    public MeshData() {
        this.vertices = new ArrayList<>();
        this.indices = new ArrayList<>();
        this.faceCount = 0;
    }

    public void addQuad(float[] quadVertices, short[] quadIndices, short baseIndex) {
        for (float v : quadVertices) {
            vertices.add(v);
        }
        for (short i : quadIndices) {
            indices.add((short)(i + baseIndex));
        }
        faceCount++;
    }

    public int getFaceCount() {
        return faceCount;
    }

    public List<Float> getVertices() {
        return vertices;
    }

    public List<Short> getIndices() {
        return indices;
    }

    public float[] getVerticesArray() {
        float[] arr = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            arr[i] = vertices.get(i);
        }
        return arr;
    }

    public short[] getIndicesArray() {
        short[] arr = new short[indices.size()];
        for (int i = 0; i < indices.size(); i++) {
            arr[i] = indices.get(i);
        }
        return arr;
    }
}
