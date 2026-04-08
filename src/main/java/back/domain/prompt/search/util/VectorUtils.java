package back.domain.prompt.search.util;

import back.global.exception.CommonErrorCode;
import back.global.exception.ServiceException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.StringJoiner;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VectorUtils {

    private static final String EMBEDDING_INVALID_VALUE = "벡터 값은 NULL이나 공란일 수 없습니다.";


    // List<Float>를 primitive float 배열로 변환한다.
    public static float[] toFloatArray(List<Float> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Float value = values.get(i);
            if (value == null) {
                throw new ServiceException(
                        CommonErrorCode.INTERNAL_SERVER_ERROR,
                        "[VectorUtils#toFloatArray] null value in embedding vector",
                        EMBEDDING_INVALID_VALUE
                );
            }
            result[i] = value;
        }
        return result;
    }

    // List<Float> 형태의 벡터 값을 PostgreSQL pgvector 형식 문자열("[v1,v2,...]")로 변환한다.
    public static String toPgVector(List<Float> values) {
        if (values == null || values.isEmpty()) {
            throw new ServiceException(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    "[VectorUtils#toPgVector] embedding vector is null or empty",
                    EMBEDDING_INVALID_VALUE
            );
        }

        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (int i = 0; i < values.size(); i++) {
            Float value = values.get(i);
            if (value == null) {
                throw new ServiceException(
                        CommonErrorCode.INTERNAL_SERVER_ERROR,
                        "[VectorUtils#toPgVector] null value in embedding vector",
                        EMBEDDING_INVALID_VALUE
                );
            }
            joiner.add(Float.toString(value));
        }
        return joiner.toString();
    }
}
