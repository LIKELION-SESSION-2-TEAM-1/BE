package g3pjt.service.user;

import g3pjt.service.user.jwt.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LogoutIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtUtil jwtUtil;

    @Test
    void logout_blacklistsToken() throws Exception {
        String token = jwtUtil.createToken("testuser");

        mockMvc.perform(post("/api/user/logout")
                        .header(JwtUtil.AUTHORIZATION_HEADER, token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/user/logout")
                        .header(JwtUtil.AUTHORIZATION_HEADER, token))
                .andExpect(status().isUnauthorized());
    }
}
