package g3pjt.service.search;

import g3pjt.service.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "recent_searches",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_recent_searches_user_keyword", columnNames = {"user_id", "keyword"})
        }
)
public class RecentSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 200)
    private String keyword;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public RecentSearch(User user, String keyword) {
        this.user = user;
        this.keyword = keyword;
        this.createdAt = Instant.now();
    }
}
