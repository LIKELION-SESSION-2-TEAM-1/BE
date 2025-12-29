package g3pjt.service.storage;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    private final SupabaseProperties properties;
    private final RestClient restClient;

    public SupabaseStorageService(SupabaseProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    public String uploadProfileImage(String username, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 비어 있습니다.");
        }

        String contentTypeValue = file.getContentType();
        MediaType contentType = (contentTypeValue != null ? MediaType.parseMediaType(contentTypeValue) : MediaType.APPLICATION_OCTET_STREAM);

        // 최소한의 안전장치: 이미지 타입만 허용
        if (!(MediaType.IMAGE_JPEG.includes(contentType)
                || MediaType.IMAGE_PNG.includes(contentType)
                || MediaType.valueOf("image/webp").includes(contentType))) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다. (jpg/png/webp)");
        }

        String baseUrl = requireValue(properties.getUrl(), "SUPABASE_URL");
        String serviceRoleKey = requireValue(properties.getServiceRoleKey(), "SUPABASE_SERVICE_ROLE_KEY");
        String bucket = requireValue(properties.getStorage().getBucket(), "SUPABASE_STORAGE_BUCKET");

        String extension = guessExtension(file, contentType);
        String objectPath = "profiles/" + safePathSegment(username) + "/" + UUID.randomUUID() + extension;

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("파일을 읽을 수 없습니다.", e);
        }

        URI uploadUri = buildObjectUploadUri(baseUrl, bucket, objectPath);

        try {
            restClient
                    .post()
                    .uri(uploadUri)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .header("x-upsert", "true")
                    .contentType(contentType)
                    .body(bytes)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            String message = "Supabase 업로드 실패 (HTTP " + e.getStatusCode().value() + ")";
            if (StringUtils.hasText(body)) {
                message += ": " + body;
            }
            throw new IllegalArgumentException(message, e);
        }

        if (!properties.getStorage().isPublicBucket()) {
            // private bucket이면 이 URL로는 접근이 안 됩니다. (signed url이 필요)
            return null;
        }

        return buildPublicObjectUrl(baseUrl, bucket, objectPath);
    }

    public String uploadChatImage(Long roomId, String username, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 비어 있습니다.");
        }

        String contentTypeValue = file.getContentType();
        MediaType contentType = (contentTypeValue != null ? MediaType.parseMediaType(contentTypeValue) : MediaType.APPLICATION_OCTET_STREAM);

        // 최소한의 안전장치: 이미지 타입만 허용
        if (!(MediaType.IMAGE_JPEG.includes(contentType)
                || MediaType.IMAGE_PNG.includes(contentType)
                || MediaType.valueOf("image/webp").includes(contentType))) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다. (jpg/png/webp)");
        }

        String baseUrl = requireValue(properties.getUrl(), "SUPABASE_URL");
        String serviceRoleKey = requireValue(properties.getServiceRoleKey(), "SUPABASE_SERVICE_ROLE_KEY");
        String bucket = requireValue(properties.getStorage().getBucket(), "SUPABASE_STORAGE_BUCKET");

        String extension = guessExtension(file, contentType);
        String safeUser = safePathSegment(username);
        String safeRoom = (roomId == null ? "unknown" : String.valueOf(roomId));
        String objectPath = "chats/" + safeRoom + "/" + safeUser + "/" + UUID.randomUUID() + extension;

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("파일을 읽을 수 없습니다.", e);
        }

        URI uploadUri = buildObjectUploadUri(baseUrl, bucket, objectPath);

        try {
            restClient
                    .post()
                    .uri(uploadUri)
                    .header("Authorization", "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .header("x-upsert", "true")
                    .contentType(contentType)
                    .body(bytes)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            String message = "Supabase 업로드 실패 (HTTP " + e.getStatusCode().value() + ")";
            if (StringUtils.hasText(body)) {
                message += ": " + body;
            }
            throw new IllegalArgumentException(message, e);
        }

        if (!properties.getStorage().isPublicBucket()) {
            return null;
        }

        return buildPublicObjectUrl(baseUrl, bucket, objectPath);
    }

    private static String requireValue(String value, String envName) {
        if (!StringUtils.hasText(value)) {
            // 컨트롤러의 IllegalArgumentException 핸들러로 400 응답을 내려주기 위함
            throw new IllegalArgumentException(envName + " 값이 설정되지 않았습니다.");
        }
        return value.trim();
    }

    private static String safePathSegment(String segment) {
        if (!StringUtils.hasText(segment)) {
            return "unknown";
        }
        // Supabase object key에 넣을 최소한의 정리
        return segment.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String guessExtension(MultipartFile file, MediaType contentType) {
        String original = file.getOriginalFilename();
        if (StringUtils.hasText(original) && original.contains(".")) {
            String ext = original.substring(original.lastIndexOf('.')).toLowerCase(Locale.ROOT);
            // 흔한 확장자만 허용
            if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".webp")) {
                return ext;
            }
        }

        if (MediaType.IMAGE_PNG.includes(contentType)) return ".png";
        if (MediaType.IMAGE_JPEG.includes(contentType)) return ".jpg";
        if (MediaType.valueOf("image/webp").includes(contentType)) return ".webp";

        return "";
    }

    private static URI buildObjectUploadUri(String baseUrl, String bucket, String objectPath) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                .pathSegment("storage", "v1", "object", bucket);

        for (String segment : objectPath.split("/")) {
            if (StringUtils.hasText(segment)) {
                builder = builder.pathSegment(segment);
            }
        }

        return builder.build(true).toUri();
    }

    private static String buildPublicObjectUrl(String baseUrl, String bucket, String objectPath) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                .pathSegment("storage", "v1", "object", "public", bucket);

        for (String segment : objectPath.split("/")) {
            if (StringUtils.hasText(segment)) {
                builder = builder.pathSegment(segment);
            }
        }

        return builder.build(true).toUriString();
    }
}
