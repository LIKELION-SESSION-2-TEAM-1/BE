package g3pjt.service.favorite;

import g3pjt.service.favorite.dto.CreateFavoritePlaceRequest;
import g3pjt.service.user.User;
import g3pjt.service.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FavoritePlaceService {

    private final FavoritePlaceRepository favoritePlaceRepository;
    private final UserRepository userRepository;

    @Transactional
    public FavoritePlace addFavorite(String username, CreateFavoritePlaceRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        String storeName = safeTrim(request.getStoreName());
        if (storeName == null || storeName.isEmpty()) {
            throw new IllegalArgumentException("storeName은 필수입니다.");
        }

        String address = safeTrim(request.getAddress());
        if (address == null) {
            address = "";
        }

        boolean exists = favoritePlaceRepository.existsByUserIdAndStoreNameAndAddress(user.getId(), storeName, address);
        if (exists) {
            throw new IllegalArgumentException("이미 즐겨찾기에 추가된 여행지입니다.");
        }

        FavoritePlace favoritePlace = new FavoritePlace(
                user,
                storeName,
                safeTrim(request.getCategory()),
                address,
                safeTrim(request.getRating()),
                safeTrim(request.getReviewCount()),
                safeTrim(request.getLink()),
                safeTrim(request.getImageUrl())
        );

        return favoritePlaceRepository.save(favoritePlace);
    }

    @Transactional(readOnly = true)
    public List<FavoritePlace> getFavorites(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return favoritePlaceRepository.findAllByUserIdOrderByIdDesc(user.getId());
    }

    @Transactional
    public void removeFavorite(String username, Long favoriteId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        FavoritePlace favorite = favoritePlaceRepository.findById(favoriteId)
                .orElseThrow(() -> new IllegalArgumentException("즐겨찾기를 찾을 수 없습니다."));

        if (favorite.getUser().getId() != user.getId()) {
            throw new IllegalArgumentException("즐겨찾기를 삭제할 권한이 없습니다.");
        }

        favoritePlaceRepository.delete(favorite);
    }

    private String safeTrim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
