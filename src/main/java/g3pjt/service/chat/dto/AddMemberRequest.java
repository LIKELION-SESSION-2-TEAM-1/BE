package g3pjt.service.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddMemberRequest {
    /**
     * 닉네임 또는 이메일(=username) 또는 아이디(username)
     */
    private String identifier;
}
