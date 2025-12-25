package g3pjt.service.favorite.dto;

import lombok.Data;

@Data
public class CreateFavoritePlaceRequest {
    private String storeName;
    private String category;
    private String address;
    private String rating;
    private String reviewCount;
    private String link;
    private String imageUrl;
}
