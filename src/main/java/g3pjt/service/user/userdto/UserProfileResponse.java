package g3pjt.service.user.userdto;

import g3pjt.service.user.User;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
public class UserProfileResponse {
    private String username;
    private String nickname;
    private String profileImageUrl;
    private String birthDate;
    private String travelPace;
    private String dailyRhythm;
    private List<String> foodPreferences;
    private List<String> foodRestrictions;

    public UserProfileResponse(User user) {
        this.username = user.getUsername();
        this.nickname = user.getNickname();
        this.profileImageUrl = user.getProfileImageUrl();
        this.birthDate = user.getBirthDate();
        this.travelPace = user.getTravelPace();
        this.dailyRhythm = user.getDailyRhythm();
        this.foodPreferences = user.getFoodPreferences() != null ? user.getFoodPreferences() : new ArrayList<>();
        this.foodRestrictions = user.getFoodRestrictions() != null ? user.getFoodRestrictions() : new ArrayList<>();
    }
}
