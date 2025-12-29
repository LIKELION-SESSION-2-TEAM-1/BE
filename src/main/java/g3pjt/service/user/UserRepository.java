package g3pjt.service.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByNickname(String nickname);

    // MongoDB에서 가져온 ID 리스트로 PostgreSQL 유저들을 한 번에 조회하는 메서드
    List<User> findByIdIn(List<Long> ids);
}