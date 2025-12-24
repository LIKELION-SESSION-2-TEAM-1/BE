package g3pjt.service.chat.service;

import g3pjt.service.chat.domain.ChatDocument;
import g3pjt.service.chat.domain.ChatRoom;
import g3pjt.service.chat.dto.ChatMemberResponse;
import g3pjt.service.chat.dto.ChatRoomRequest;
import g3pjt.service.chat.dto.ChatRoomMembersResponse;
import g3pjt.service.chat.dto.InviteLinkResponse;
import g3pjt.service.chat.repository.ChatRepository;
import g3pjt.service.chat.repository.ChatRoomRepository;
import g3pjt.service.user.User;
import g3pjt.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
            .ownerUserId(user.getId())
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

    public ChatRoomMembersResponse getRoomMembers(Long roomId, Authentication authentication) {
        ChatRoom room = getRoomOrThrow(roomId);

        Long requesterId = getRequesterUserId(authentication);
        ensureMember(room, requesterId);

        List<ChatMemberResponse> members = room.getMemberIds().stream()
                .map(memberId -> ChatMemberResponse.builder()
                        .userId(memberId)
                        .displayName(userService.getDisplayNameByUserId(memberId))
                        .build())
                .collect(Collectors.toList());

        return ChatRoomMembersResponse.builder()
                .roomId(roomId)
                .memberCount(members.size())
                .members(members)
                .build();
    }

    public void deleteRoom(Long roomId, Authentication authentication) {
        ChatRoom room = getRoomOrThrow(roomId);

        Long requesterId = getRequesterUserId(authentication);
        ensureMember(room, requesterId);

        Long ownerId = resolveOwnerUserId(room);
        if (ownerId == null || !ownerId.equals(requesterId)) {
            throw new IllegalArgumentException("방장만 방을 삭제할 수 있습니다.");
        }

        chatRepository.deleteByChatRoomId(roomId);
        chatRoomRepository.delete(room);
    }

    public ChatRoom addMemberByIdentifier(Long roomId, String identifier, Authentication authentication) {
        ChatRoom room = getRoomOrThrow(roomId);

        Long requesterId = getRequesterUserId(authentication);
        ensureMember(room, requesterId);

        Long targetUserId = userService.findByUsernameOrNickname(identifier).getId();
        if (!room.getMemberIds().contains(targetUserId)) {
            room.getMemberIds().add(targetUserId);
            room = chatRoomRepository.save(room);
        }
        return room;
    }

    public InviteLinkResponse generateInviteLink(Long roomId, Authentication authentication, String inviteBaseUrl) {
        ChatRoom room = getRoomOrThrow(roomId);

        Long requesterId = getRequesterUserId(authentication);
        ensureMember(room, requesterId);

        String code = UUID.randomUUID().toString().replace("-", "");
        room.setInviteCode(code);
        room.setInviteCodeCreatedAt(LocalDateTime.now());
        chatRoomRepository.save(room);

        String inviteUrl = null;
        if (inviteBaseUrl != null && !inviteBaseUrl.trim().isEmpty()) {
            String base = inviteBaseUrl.trim();
            inviteUrl = base + "/chat/join?roomId=" + roomId + "&code=" + code;
        }

        return InviteLinkResponse.builder()
                .roomId(roomId)
                .inviteCode(code)
                .inviteUrl(inviteUrl)
                .build();
    }

    public ChatRoom joinByInviteCode(Long roomId, String inviteCode, Authentication authentication) {
        if (inviteCode == null || inviteCode.trim().isEmpty()) {
            throw new IllegalArgumentException("inviteCode가 비어있습니다.");
        }
        ChatRoom room = getRoomOrThrow(roomId);
        if (room.getInviteCode() == null || !room.getInviteCode().equals(inviteCode.trim())) {
            throw new IllegalArgumentException("초대 코드가 올바르지 않습니다.");
        }

        Long requesterId = getRequesterUserId(authentication);
        if (!room.getMemberIds().contains(requesterId)) {
            room.getMemberIds().add(requesterId);
            room = chatRoomRepository.save(room);
        }
        return room;
    }

    private ChatRoom getRoomOrThrow(Long roomId) {
        ChatRoom room = chatRoomRepository.findByRoomId(roomId);
        if (room == null) {
            throw new IllegalArgumentException("채팅방을 찾을 수 없습니다.");
        }
        return room;
    }

    private Long getRequesterUserId(Authentication authentication) {
        String username = authentication.getName();
        return userService.getUserProfile(username).getId();
    }

    private void ensureMember(ChatRoom room, Long userId) {
        if (userId == null || !room.getMemberIds().contains(userId)) {
            throw new IllegalArgumentException("채팅방 멤버만 수행할 수 있습니다.");
        }
    }

    private Long resolveOwnerUserId(ChatRoom room) {
        if (room.getOwnerUserId() != null) {
            return room.getOwnerUserId();
        }
        // legacy fallback: creator was added first
        if (room.getMemberIds() != null && !room.getMemberIds().isEmpty()) {
            return room.getMemberIds().get(0);
        }
        return null;
    }
}
