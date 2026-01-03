package g3pjt.service.user.userdto;

import g3pjt.service.user.User;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
public class UserProfileResponse {
    private Long id;
    private String username;
    private String nickname;
    private String profileImageUrl;
    private boolean emailVerified;
    private String birthDate;
    private String travelPace;
    private String dailyRhythm;
    private List<String> foodPreferences;
    private List<String> foodRestrictions;
    private boolean recentSearchEnabled;

    public UserProfileResponse(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.nickname = user.getNickname();
        this.profileImageUrl = user.getProfileImageUrl();
        this.emailVerified = user.isEmailVerified();
        this.birthDate = user.getBirthDate();
        this.travelPace = user.getTravelPace();
        this.dailyRhythm = user.getDailyRhythm();
        this.foodPreferences = user.getFoodPreferences() != null ? user.getFoodPreferences() : new ArrayList<>();
        this.foodRestrictions = user.getFoodRestrictions() != null ? user.getFoodRestrictions() : new ArrayList<>();
        this.recentSearchEnabled = user.isRecentSearchEnabled();
    }
}
