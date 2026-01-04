package g3pjt.service.config;

import g3pjt.service.user.EmailVerificationRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage() != null ? e.getMessage() : "Bad Request"));
    }

    @ExceptionHandler(EmailVerificationRequiredException.class)
    public ResponseEntity<Map<String, String>> handleEmailVerificationRequired(EmailVerificationRequiredException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", e.getMessage() != null ? e.getMessage() : "이메일 인증이 필요합니다."));
    }
}
