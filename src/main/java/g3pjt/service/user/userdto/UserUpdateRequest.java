package g3pjt.service.user.userdto;

import lombok.Data;
import java.util.List;

@Data
public class UserUpdateRequest {
    private String nickname;
    private String profileImageUrl;
    private String birthDate;
    private String travelPace;
    private String dailyRhythm;
    private List<String> foodPreferences;
    private List<String> foodRestrictions;
}
