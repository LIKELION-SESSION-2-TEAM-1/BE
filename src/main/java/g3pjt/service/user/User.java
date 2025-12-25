package g3pjt.service.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column
    private String nickname;

    @Column
    private String profileImageUrl;

    @Column
    private String birthDate; // YYYY-MM-DD

    // 여행 스타일
    @Column
    private String travelPace; // 느림, 보통, 빠름

    @Column
    private String dailyRhythm; // 아침형, 유연, 야행성

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_food_preferences", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "preference")
    private java.util.List<String> foodPreferences = new java.util.ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_food_restrictions", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "restriction")
    private java.util.List<String> foodRestrictions = new java.util.ArrayList<>();

    // 최근 검색 자동 저장 설정
    @Column(name = "recent_search_enabled")
    private Boolean recentSearchEnabled = true;

    public boolean isRecentSearchEnabled() {
        return recentSearchEnabled == null ? true : recentSearchEnabled;
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateProfile(String nickname, String profileImageUrl, String birthDate,
                              String travelPace, String dailyRhythm,
                              java.util.List<String> foodPreferences, java.util.List<String> foodRestrictions,
                              Boolean recentSearchEnabled) {
        if (nickname != null) this.nickname = nickname;
        if (profileImageUrl != null) this.profileImageUrl = profileImageUrl;
        if (birthDate != null) this.birthDate = birthDate;
        if (travelPace != null) this.travelPace = travelPace;
        if (dailyRhythm != null) this.dailyRhythm = dailyRhythm;

        if (recentSearchEnabled != null) {
            this.recentSearchEnabled = recentSearchEnabled;
        }
        
        if (foodPreferences != null) {
            this.foodPreferences.clear();
            this.foodPreferences.addAll(foodPreferences);
        }
        if (foodRestrictions != null) {
            this.foodRestrictions.clear();
            this.foodRestrictions.addAll(foodRestrictions);
        }
    }
}
