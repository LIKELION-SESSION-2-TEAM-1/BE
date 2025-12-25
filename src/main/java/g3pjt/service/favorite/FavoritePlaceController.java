package g3pjt.service.favorite;

import g3pjt.service.favorite.dto.CreateFavoritePlaceRequest;
import g3pjt.service.favorite.dto.FavoritePlaceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Favorite Places API", description = "여행지 즐겨찾기 API")
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoritePlaceController {

    private final FavoritePlaceService favoritePlaceService;

    @Operation(summary = "즐겨찾기 추가", description = "검색 결과(여행지/가게)를 즐겨찾기에 추가합니다.")
    @PostMapping
    public ResponseEntity<?> addFavorite(
            @RequestBody CreateFavoritePlaceRequest request,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증이 필요합니다.");
        }

        try {
            FavoritePlace saved = favoritePlaceService.addFavorite(authentication.getName(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(FavoritePlaceResponse.from(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @Operation(summary = "내 즐겨찾기 목록", description = "내가 추가한 즐겨찾기 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<?> getFavorites(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증이 필요합니다.");
        }

        List<FavoritePlaceResponse> responses = favoritePlaceService.getFavorites(authentication.getName())
                .stream()
                .map(FavoritePlaceResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "즐겨찾기 삭제", description = "즐겨찾기 id로 삭제합니다.")
    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<?> removeFavorite(
            @PathVariable Long favoriteId,
            Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증이 필요합니다.");
        }

        try {
            favoritePlaceService.removeFavorite(authentication.getName(), favoriteId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
