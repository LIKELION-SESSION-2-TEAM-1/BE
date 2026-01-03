package g3pjt.service.user.email;

import g3pjt.service.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "email_verification_tokens", indexes = {
        @Index(name = "idx_email_verification_token", columnList = "token", unique = true),
        @Index(name = "idx_email_verification_user", columnList = "user_id")
})
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 128)
    private String token;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant usedAt;

    public EmailVerificationToken(User user, String token, Instant createdAt, Instant expiresAt) {
        this.user = user;
        this.token = token;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    public void markUsed(Instant now) {
        this.usedAt = now;
    }
}
