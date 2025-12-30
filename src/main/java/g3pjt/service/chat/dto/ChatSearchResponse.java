package g3pjt.service.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatSearchResponse {
    private List<ChatRoomSearchResponse> rooms;
    private List<ChatMessageSearchResponse> messages;
}
