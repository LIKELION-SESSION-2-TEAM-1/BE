package g3pjt.service.favorite.dto;

import g3pjt.service.favorite.FavoritePlace;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FavoritePlaceResponse {
    private Long id;
    private String storeName;
    private String category;
    private String address;
    private String rating;
    private String reviewCount;
    private String link;
    private String imageUrl;

    public static FavoritePlaceResponse from(FavoritePlace favoritePlace) {
        return FavoritePlaceResponse.builder()
                .id(favoritePlace.getId())
                .storeName(favoritePlace.getStoreName())
                .category(favoritePlace.getCategory())
                .address(favoritePlace.getAddress())
                .rating(favoritePlace.getRating())
                .reviewCount(favoritePlace.getReviewCount())
                .link(favoritePlace.getLink())
                .imageUrl(favoritePlace.getImageUrl())
                .build();
    }
}
