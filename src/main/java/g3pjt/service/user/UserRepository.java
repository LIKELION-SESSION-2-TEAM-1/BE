package g3pjt.service.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // username으로 사용자를 찾기 위한 메서드 (중복 검사 및 로그인 시 사용)
    Optional<User> findByUsername(String username);
}
