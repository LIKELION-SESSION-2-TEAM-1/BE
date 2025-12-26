package g3pjt.service.search.dto;

import g3pjt.service.search.RecentSearch;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class RecentSearchResponse {
    private Long id;
    private String keyword;
    private Instant createdAt;

    public static RecentSearchResponse from(RecentSearch recentSearch) {
        return RecentSearchResponse.builder()
                .id(recentSearch.getId())
                .keyword(recentSearch.getKeyword())
                .createdAt(recentSearch.getCreatedAt())
                .build();
    }
}
