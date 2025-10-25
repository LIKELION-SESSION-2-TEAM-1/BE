package g3pjt.service.crawling;
import lombok.Data; // Lombok을 사용하면 Getter, Setter 등을 자동으로 만들어줍니다.

@Data
public class StoreDto {
    // 필드 이름이 Python 딕셔너리의 key와 정확히 일치해야 합니다.
    private String storeName;
    private String category;
    private String address;
    private String rating;
    private String reviewCount;
    private String link;
    private String imageUrl;
}