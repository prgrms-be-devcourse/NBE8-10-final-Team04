package back.domain.prompt.search.util;

import java.util.List;
import java.util.StringJoiner;

public final class VectorUtils {

    private VectorUtils() {
    }

    public static float[] toFloatArray(List<Float> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Float value = values.get(i);
            if (value == null) {
                throw new IllegalArgumentException("Vector value must not be null. index=" + i);
            }
            result[i] = value;
        }
        return result;
    }

    public static String toPgVector(List<Float> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("Vector values must not be null or empty.");
        }

        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (int i = 0; i < values.size(); i++) {
            Float value = values.get(i);
            if (value == null) {
                throw new IllegalArgumentException("Vector value must not be null. index=" + i);
            }
            joiner.add(Float.toString(value));
        }
        return joiner.toString();
    }
}
