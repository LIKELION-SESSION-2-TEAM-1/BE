package g3pjt.service.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteLinkResponse {
    private Long roomId;
    private String inviteCode;

    /**
     * 프론트에서 그대로 공유할 수 있는 링크 (선택)
     */
    private String inviteUrl;
}
