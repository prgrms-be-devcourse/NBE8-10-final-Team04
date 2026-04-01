package back.domain.aimodel.service;

/**
 * OCI Object Storage 파일 업로드/다운로드 서비스입니다.
 */
public interface OciStorageService {

    /**
     * OCI에서 파일을 바이트 배열로 다운로드합니다.
     *
     * @param objectName OCI 오브젝트 전체 경로
     * @return 파일 바이트 배열
     */
    byte[] download(String objectName);

    /**
     * OCI에서 JSON 파일을 역직렬화해서 반환합니다.
     *
     * @param objectName OCI 오브젝트 전체 경로
     * @param clazz      역직렬화 대상 클래스
     * @return 역직렬화된 객체
     */
    <T> T downloadJson(String objectName, Class<T> clazz);

    /**
     * OCI에 파일을 업로드합니다.
     *
     * @param objectName  OCI 오브젝트 전체 경로
     * @param content     업로드할 바이트 배열
     * @param contentType Content-Type 헤더값
     */
    void upload(String objectName, byte[] content, String contentType);

    /**
     * OCI에 객체를 JSON으로 직렬화해서 업로드합니다.
     *
     * @param objectName OCI 오브젝트 전체 경로
     * @param data       직렬화할 객체
     */
    void uploadJson(String objectName, Object data);

    /**
     * prefix가 포함된 OCI 오브젝트 전체 경로를 반환합니다.
     *
     * @param filename 파일명
     * @return prefix + filename
     */
    String objectName(String filename);
}
