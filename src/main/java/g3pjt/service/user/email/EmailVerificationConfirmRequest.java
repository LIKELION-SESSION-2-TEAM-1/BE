package g3pjt.service.user.email;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailVerificationConfirmRequest {
    private String token;
}
