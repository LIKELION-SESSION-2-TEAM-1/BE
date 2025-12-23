package g3pjt.service.user;

// ... (다른 import)
import g3pjt.service.user.jwt.JwtUtil; // ⭐️ JwtUtil 임포트
import g3pjt.service.user.userdto.LoginRequestDto;
import g3pjt.service.user.userdto.SignupRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동으로 만들어줍니다.
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 회원가입
     */
    public void signup(SignupRequestDto requestDto) {
        String username = requestDto.getUsername();
        String password = requestDto.getPassword();

        // 1. 아이디 중복 확인
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 아이디입니다.");
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(password);

        // 3. 유저 생성 및 저장
        User user = new User(username, encodedPassword);
        userRepository.save(user);
    }

    /**
     * 로그인
     */
    public String login(LoginRequestDto requestDto) {
        String username = requestDto.getUsername();
        String password = requestDto.getPassword();

        // 1. 유저 확인
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.")
        );

        // 2. 비밀번호 대조
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        // 3. 로그인 성공 및 JWT 발급
        return jwtUtil.createToken(user.getUsername());
    }
    public String getDisplayNameByUserId(Long userId){
        if (userId == null) return "unknown";
        return userRepository.findById(userId)
                .map(user -> {
                    // 닉네임이 있으면 닉네임 반환, 없으면 유저네임 반환
                    if (user.getNickname() != null && !user.getNickname().isEmpty()) {
                        return user.getNickname();
                    }
                    return user.getUsername();
                }).orElse("unknown");
    }

    /**
     * 프로필 수정
     */
    @org.springframework.transaction.annotation.Transactional
    public void updateProfile(String username, g3pjt.service.user.userdto.UserUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        user.updateProfile(
                request.getNickname(),
                request.getProfileImageUrl(),
                request.getBirthDate(),
                request.getTravelPace(),
                request.getDailyRhythm(),
                request.getFoodPreferences(),
                request.getFoodRestrictions()
        );
        // Transactional 어노테이션 덕분에 save 호출 없이도 더티 체킹으로 업데이트됨
    }

    /**
     * 프로필 조회
     */
    public User getUserProfile(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}