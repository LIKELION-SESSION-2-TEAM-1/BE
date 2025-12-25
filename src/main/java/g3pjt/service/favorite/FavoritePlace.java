package g3pjt.service.favorite;

import g3pjt.service.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "favorite_places",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_favorite_places_user_store_address", columnNames = {"user_id", "store_name", "address"})
        }
)
public class FavoritePlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column
    private String category;

    @Column
    private String address;

    @Column
    private String rating;

    @Column
    private String reviewCount;

    @Column(length = 1024)
    private String link;

    @Column(length = 2048)
    private String imageUrl;

    public FavoritePlace(User user,
                         String storeName,
                         String category,
                         String address,
                         String rating,
                         String reviewCount,
                         String link,
                         String imageUrl) {
        this.user = user;
        this.storeName = storeName;
        this.category = category;
        this.address = address;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.link = link;
        this.imageUrl = imageUrl;
    }
}
