package g3pjt.service.chat.service;

import g3pjt.service.chat.domain.ChatDocument;
import g3pjt.service.chat.domain.ChatRoom;
import g3pjt.service.chat.domain.ChatRoomReadState;
import g3pjt.service.chat.dto.ChatMemberResponse;
import g3pjt.service.chat.dto.ChatRoomRequest;
import g3pjt.service.chat.dto.ChatRoomMembersResponse;
import g3pjt.service.chat.dto.ChatMessageSearchResponse;
import g3pjt.service.chat.dto.ChatRoomSearchResponse;
import g3pjt.service.chat.dto.ChatSearchResponse;
import g3pjt.service.chat.dto.ChatRoomSummaryResponse;
import g3pjt.service.chat.dto.InviteLinkResponse;
import g3pjt.service.chat.repository.ChatRepository;
import g3pjt.service.chat.repository.ChatRoomRepository;
import g3pjt.service.chat.repository.ChatRoomReadStateRepository;
import g3pjt.service.storage.SupabaseStorageService;
import g3pjt.service.user.User;
import g3pjt.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRepository chatRepository;
    private final ChatRoomReadStateRepository chatRoomReadStateRepository;
    private final UserService userService;
    private final SupabaseStorageService supabaseStorageService;

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

    public ChatRoom createRoomWithImage(ChatRoomRequest request, MultipartFile file, Authentication authentication) {
        ChatRoom room = createRoom(request, authentication);

        if (file == null || file.isEmpty()) {
            return room;
        }

        String username = authentication.getName();
        String imageUrl = supabaseStorageService.uploadChatRoomImage(room.getRoomId(), username, file);
        room.setImageUrl(imageUrl);
        return chatRoomRepository.save(room);
    }

    public List<ChatRoom> getMyRooms(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getUserProfile(username);
        return chatRoomRepository.findByMemberIdsContains(user.getId());
    }

    public ChatSearchResponse search(Authentication authentication, String keyword, Integer roomLimit, Integer messageLimit) {
        String safeKeyword = safeKeyword(keyword);
        int safeRoomLimit = clamp(roomLimit != null ? roomLimit : 3, 0, 20);
        int safeMessageLimit = clamp(messageLimit != null ? messageLimit : 3, 0, 20);

        List<ChatRoomSearchResponse> rooms = searchRooms(authentication, safeKeyword, safeRoomLimit);
        List<ChatMessageSearchResponse> messages = searchMessages(
                authentication,
                safeKeyword,
                PageRequest.of(0, safeMessageLimit, Sort.by(Sort.Direction.DESC, "timestamp"))
        );

        return ChatSearchResponse.builder()
                .rooms(rooms)
                .messages(messages)
                .build();
    }

    public List<ChatRoomSearchResponse> searchRooms(Authentication authentication, String keyword, int limit) {
        String safeKeyword = safeKeyword(keyword);
        int safeLimit = clamp(limit, 0, 100);

        Long userId = getRequesterUserId(authentication);
        List<ChatRoom> rooms = chatRoomRepository.findByMemberIdsContains(userId);
        String normalized = safeKeyword.toLowerCase(Locale.ROOT);

        return rooms.stream()
                .filter(r -> r.getName() != null && r.getName().toLowerCase(Locale.ROOT).contains(normalized))
                .limit(safeLimit)
                .map(r -> {
                    ChatDocument last = chatRepository.findFirstByChatRoomIdOrderByTimestampDesc(r.getRoomId()).orElse(null);
                    return ChatRoomSearchResponse.builder()
                            .roomId(r.getRoomId())
                            .name(r.getName())
                            .lastMessage(last != null ? last.getMessage() : null)
                            .lastMessageAt(last != null ? last.getTimestamp() : null)
                            .build();
                })
                .toList();
    }

    public List<ChatMessageSearchResponse> searchMessages(Authentication authentication, String keyword, Pageable pageable) {
        String safeKeyword = safeKeyword(keyword);

        Long userId = getRequesterUserId(authentication);
        List<ChatRoom> rooms = chatRoomRepository.findByMemberIdsContains(userId);
        if (rooms.isEmpty()) {
            return List.of();
        }

        List<Long> roomIds = rooms.stream().map(ChatRoom::getRoomId).toList();

        Map<Long, String> roomNameById = new HashMap<>();
        for (ChatRoom r : rooms) {
            if (r.getRoomId() != null) {
                roomNameById.put(r.getRoomId(), r.getName());
            }
        }

        String regex = ".*" + Pattern.quote(safeKeyword) + ".*";
        List<ChatDocument> docs = chatRepository.searchByRoomIdsAndMessageRegex(roomIds, regex, pageable);

        return docs.stream()
                .map(doc -> ChatMessageSearchResponse.builder()
                        .messageId(doc.getId())
                        .roomId(doc.getChatRoomId())
                        .roomName(roomNameById.get(doc.getChatRoomId()))
                        .senderUserId(doc.getSenderUserId())
                        .senderName(doc.getSenderName())
                        .message(doc.getMessage())
                        .timestamp(doc.getTimestamp())
                        .build())
                .toList();
    }

    private String safeKeyword(String keyword) {
        if (keyword == null) {
            throw new IllegalArgumentException("keyword가 비어있습니다.");
        }
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("keyword가 비어있습니다.");
        }
        return trimmed;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

        public List<ChatRoomSummaryResponse> getMyRoomSummaries(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.getUserProfile(username);
        Long userId = user.getId();

        List<ChatRoom> rooms = chatRoomRepository.findByMemberIdsContains(userId);
        Instant now = Instant.now();

        return rooms.stream()
            .map(room -> {
                ChatRoomReadState readState = chatRoomReadStateRepository.findByRoomIdAndUserId(room.getRoomId(), userId)
                    .orElseGet(() -> initializeReadState(room.getRoomId(), userId, now));

                Instant lastReadAt = readState.getLastReadAt() == null ? now : readState.getLastReadAt();
                long unreadCount = chatRepository.countByChatRoomIdAndTimestampAfterAndSenderUserIdNot(
                    room.getRoomId(),
                    lastReadAt,
                    userId
                );

                return ChatRoomSummaryResponse.builder()
                    .roomId(room.getRoomId())
                    .name(room.getName())
                    .imageUrl(room.getImageUrl())
                    .startDate(room.getStartDate())
                    .endDate(room.getEndDate())
                    .travelStyle(room.getTravelStyle())
                    .createdAt(room.getCreatedAt())
                    .ownerUserId(resolveOwnerUserId(room))
                    .unreadCount(unreadCount)
                    .build();
            })
            .toList();
        }

        public void markRoomAsRead(Long roomId, Authentication authentication) {
        ChatRoom room = getRoomOrThrow(roomId);

        Long requesterId = getRequesterUserId(authentication);
        ensureMember(room, requesterId);

        Instant now = Instant.now();
        ChatRoomReadState state = chatRoomReadStateRepository.findByRoomIdAndUserId(roomId, requesterId)
            .orElse(ChatRoomReadState.builder()
                .roomId(roomId)
                .userId(requesterId)
                .build());

        state.setLastReadAt(now);
        state.setUpdatedAt(now);
        chatRoomReadStateRepository.save(state);
        }

    public List<ChatDocument> getChatHistory(Long chatRoomId) {
        return chatRepository.findByChatRoomIdOrderByTimestampAsc(chatRoomId);
    }

    public String uploadChatImage(Long roomId, MultipartFile file, Authentication authentication) {
        ChatRoom room = getRoomOrThrow(roomId);

        Long requesterId = getRequesterUserId(authentication);
        ensureMember(room, requesterId);

        String username = authentication.getName();
        return supabaseStorageService.uploadChatImage(roomId, username, file);
    }

    public ChatRoomMembersResponse getRoomMembers(Long roomId, Authentication authentication) {
        ChatRoom room = getRoomOrThrow(roomId);

        Long requesterId = getRequesterUserId(authentication);
        ensureMember(room, requesterId);

        List<ChatMemberResponse> members = room.getMemberIds().stream()
                .map(memberId -> ChatMemberResponse.builder()
                        .userId(memberId)
                        .displayName(userService.getDisplayNameByUserId(memberId))
                .profileImageUrl(userService.getProfileImageUrlByUserId(memberId))
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

    public ChatRoom leaveRoom(Long roomId, Authentication authentication) {
        ChatRoom room = getRoomOrThrow(roomId);

        Long requesterId = getRequesterUserId(authentication);
        ensureMember(room, requesterId);

        List<Long> updatedMembers = new ArrayList<>(room.getMemberIds());
        updatedMembers.removeIf(memberId -> memberId != null && memberId.equals(requesterId));
        room.setMemberIds(updatedMembers);

        Long ownerId = resolveOwnerUserId(room);
        if (ownerId != null && ownerId.equals(requesterId)) {
            if (!updatedMembers.isEmpty()) {
                room.setOwnerUserId(updatedMembers.get(0));
            } else {
                room.setOwnerUserId(null);
            }
        }

        return chatRoomRepository.save(room);
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

    private ChatRoomReadState initializeReadState(Long roomId, Long userId, Instant now) {
        ChatRoomReadState state = ChatRoomReadState.builder()
                .roomId(roomId)
                .userId(userId)
                // 최초 도입 시 기존 메시지 전부 unread로 뜨는 걸 방지하기 위해, 첫 조회 시점으로 초기화
                .lastReadAt(now)
                .updatedAt(now)
                .build();
        return chatRoomReadStateRepository.save(state);
    }

    ChatRoom getRoomOrThrow(Long roomId) {
        ChatRoom room = chatRoomRepository.findByRoomId(roomId);
        if (room == null) {
            throw new IllegalArgumentException("채팅방을 찾을 수 없습니다.");
        }
        return room;
    }

    Long getRequesterUserId(Authentication authentication) {
        String username = authentication.getName();
        return userService.getUserProfile(username).getId();
    }

    void ensureMember(ChatRoom room, Long userId) {
        if (userId == null || !room.getMemberIds().contains(userId)) {
            throw new IllegalArgumentException("채팅방 멤버만 수행할 수 있습니다.");
        }
    }

    Long resolveOwnerUserId(ChatRoom room) {
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
