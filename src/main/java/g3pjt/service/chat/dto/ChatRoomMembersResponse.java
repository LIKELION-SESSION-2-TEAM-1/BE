package g3pjt.service.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomMembersResponse {
    private Long roomId;
    private int memberCount;
    private List<ChatMemberResponse> members;
}
