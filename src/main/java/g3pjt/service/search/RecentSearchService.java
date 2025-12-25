package g3pjt.service.search;

import g3pjt.service.search.dto.CreateRecentSearchRequest;
import g3pjt.service.user.User;
import g3pjt.service.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecentSearchService {

    private static final int MAX_RECENT_SEARCHES = 10;

    private final RecentSearchRepository recentSearchRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addRecentSearch(String username, CreateRecentSearchRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!user.isRecentSearchEnabled()) {
            return; // 자동저장 OFF면 저장하지 않음
        }

        String keyword = safeTrim(request.getKeyword());
        if (keyword == null || keyword.isEmpty()) {
            throw new IllegalArgumentException("keyword는 필수입니다.");
        }

        // 같은 키워드가 이미 있으면 최신으로 갱신 (delete 후 insert)
        recentSearchRepository.findByUserIdAndKeyword(user.getId(), keyword)
                .ifPresent(recentSearchRepository::delete);

        recentSearchRepository.save(new RecentSearch(user, keyword));

        // 개수 제한(10개): 오래된 것부터 삭제
        List<RecentSearch> asc = recentSearchRepository.findAllByUserIdOrderByCreatedAtAsc(user.getId());
        int overflow = asc.size() - MAX_RECENT_SEARCHES;
        for (int i = 0; i < overflow; i++) {
            recentSearchRepository.delete(asc.get(i));
        }
    }

    @Transactional(readOnly = true)
    public List<RecentSearch> getRecentSearches(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return recentSearchRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public void deleteOne(String username, Long recentSearchId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        RecentSearch recent = recentSearchRepository.findById(recentSearchId)
                .orElseThrow(() -> new IllegalArgumentException("최근 검색 항목을 찾을 수 없습니다."));

        if (recent.getUser().getId() != user.getId()) {
            throw new IllegalArgumentException("최근 검색 항목을 삭제할 권한이 없습니다.");
        }

        recentSearchRepository.delete(recent);
    }

    @Transactional
    public void clearAll(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<RecentSearch> all = recentSearchRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());
        recentSearchRepository.deleteAllInBatch(all);
    }

    private String safeTrim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
