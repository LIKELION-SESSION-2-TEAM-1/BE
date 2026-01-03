package g3pjt.service.user;

public class EmailVerificationRequiredException extends RuntimeException {
    public EmailVerificationRequiredException(String message) {
        super(message);
    }
}
