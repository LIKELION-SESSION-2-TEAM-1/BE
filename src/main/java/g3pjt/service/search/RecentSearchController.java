package g3pjt.service.search;

import g3pjt.service.search.dto.CreateRecentSearchRequest;
import g3pjt.service.search.dto.RecentSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Recent Searches API", description = "최근 검색(검색어 자동 저장) API")
@RestController
@RequestMapping("/api/searches/recent")
@RequiredArgsConstructor
public class RecentSearchController {

    private final RecentSearchService recentSearchService;

    @Operation(summary = "최근 검색 목록", description = "내 최근 검색(검색어) 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<?> getRecent(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증이 필요합니다.");
        }

        List<RecentSearchResponse> responses = recentSearchService.getRecentSearches(authentication.getName())
                .stream()
                .map(RecentSearchResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "최근 검색 추가", description = "검색어를 최근 검색에 추가합니다. (자동저장 OFF면 저장되지 않음)")
    @PostMapping
    public ResponseEntity<?> addRecent(
            @RequestBody CreateRecentSearchRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증이 필요합니다.");
        }

        try {
            recentSearchService.addRecentSearch(authentication.getName(), request);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "최근 검색 개별 삭제", description = "recentSearchId로 최근 검색 항목 1개를 삭제합니다.")
    @DeleteMapping("/{recentSearchId}")
    public ResponseEntity<?> deleteOne(
            @PathVariable Long recentSearchId,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증이 필요합니다.");
        }

        try {
            recentSearchService.deleteOne(authentication.getName(), recentSearchId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "최근 검색 전체 삭제", description = "내 최근 검색 항목을 전체 삭제합니다.")
    @DeleteMapping
    public ResponseEntity<?> clearAll(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증이 필요합니다.");
        }

        recentSearchService.clearAll(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
