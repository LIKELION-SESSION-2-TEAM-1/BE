package g3pjt.service.chat;

import g3pjt.service.user.User;
import g3pjt.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRepository chatRepository;
    private final UserService userService;

    public ChatRoom createRoom(ChatRoomRequest request, Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getUserProfile(username);

        Long roomId = System.currentTimeMillis();

        List<Long> members = new ArrayList<>();
        members.add(user.getId());

        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(roomId)
                .name(request.getName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .travelStyle(request.getTravelStyle())
                .createdAt(LocalDateTime.now())
                .memberIds(members)
                .build();

        return chatRoomRepository.save(chatRoom);
    }

    public List<ChatRoom> getMyRooms(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getUserProfile(username);
        return chatRoomRepository.findByMemberIdsContains(user.getId());
    }

    public List<ChatDocument> getChatHistory(Long chatRoomId) {
        return chatRepository.findByChatRoomIdOrderByTimestampAsc(chatRoomId);
    }
}
