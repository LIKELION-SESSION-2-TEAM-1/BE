package g3pjt.service.search;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecentSearchRepository extends JpaRepository<RecentSearch, Long> {
    List<RecentSearch> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<RecentSearch> findAllByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<RecentSearch> findByUserIdAndKeyword(Long userId, String keyword);
}
