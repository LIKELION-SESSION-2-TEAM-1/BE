package g3pjt.service.user.userdto;

import lombok.Data;
import java.util.List;

@Data
public class UserUpdateRequest {
    @io.swagger.v3.oas.annotations.media.Schema(description = "변경할 닉네임 (null이면 변경 안 함)", example = "여행왕")
    private String nickname;

    @io.swagger.v3.oas.annotations.media.Schema(description = "프로필 이미지 URL (null이면 변경 안 함)")
    private String profileImageUrl;

    @io.swagger.v3.oas.annotations.media.Schema(description = "생년월일 (YYYY-MM-DD)", example = "1999-01-01")
    private String birthDate;

    @io.swagger.v3.oas.annotations.media.Schema(description = "이동 페이스 (느림/보통/빠름)", example = "보통")
    private String travelPace;

    @io.swagger.v3.oas.annotations.media.Schema(description = "하루 리듬 (아침형/유연/야행성)", example = "아침형")
    private String dailyRhythm;

    @io.swagger.v3.oas.annotations.media.Schema(description = "선호 음식 리스트", example = "[\"한식\", \"해산물\"]")
    private List<String> foodPreferences;

    @io.swagger.v3.oas.annotations.media.Schema(description = "제외 음식 리스트 (알레르기 등)", example = "[\"오이\", \"땅콩\"]")
    private List<String> foodRestrictions;
}
