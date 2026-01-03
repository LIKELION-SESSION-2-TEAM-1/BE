package g3pjt.service.favorite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoritePlaceRepository extends JpaRepository<FavoritePlace, Long> {
    List<FavoritePlace> findAllByUserIdOrderByIdDesc(Long userId);

    boolean existsByUserIdAndStoreNameAndAddress(Long userId, String storeName, String address);

    void deleteAllByUserId(Long userId);
}
