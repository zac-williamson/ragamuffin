package ragamuffin.building;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * A crafting recipe that transforms input materials into output materials.
 */
public class Recipe {
    private final Map<Material, Integer> inputs;
    private final Map<Material, Integer> outputs;

    public Recipe(Map<Material, Integer> inputs, Map<Material, Integer> outputs) {
        this.inputs = Collections.unmodifiableMap(inputs);
        this.outputs = Collections.unmodifiableMap(outputs);
    }

    public Map<Material, Integer> getInputs() {
        return inputs;
    }

    public Map<Material, Integer> getOutputs() {
        return outputs;
    }

    /**
     * Get a display name for this recipe.
     * Format: "4 WOOD -> 8 PLANKS"
     */
    public String getDisplayName() {
        StringBuilder sb = new StringBuilder();

        // Inputs
        boolean first = true;
        for (Map.Entry<Material, Integer> entry : inputs.entrySet()) {
            if (!first) sb.append(" + ");
            sb.append(entry.getValue()).append(" ").append(entry.getKey().getDisplayName());
            first = false;
        }

        sb.append(" -> ");

        // Outputs
        first = true;
        for (Map.Entry<Material, Integer> entry : outputs.entrySet()) {
            if (!first) sb.append(" + ");
            sb.append(entry.getValue()).append(" ").append(entry.getKey().getDisplayName());
            first = false;
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Recipe recipe = (Recipe) o;
        return Objects.equals(inputs, recipe.inputs) &&
               Objects.equals(outputs, recipe.outputs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputs, outputs);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
