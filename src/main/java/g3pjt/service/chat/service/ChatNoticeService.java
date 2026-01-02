package g3pjt.service.chat.service;

import g3pjt.service.chat.domain.ChatNoticeDocument;
import g3pjt.service.chat.domain.ChatPollDocument;
import g3pjt.service.chat.domain.ChatRoom;
import g3pjt.service.chat.dto.CreatePollNoticeRequest;
import g3pjt.service.chat.dto.NoticeResponse;
import g3pjt.service.chat.repository.ChatNoticeRepository;
import g3pjt.service.chat.repository.ChatPollRepository;
import g3pjt.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatNoticeService {

    private final ChatService chatService;
    private final UserService userService;

    private final ChatPollRepository chatPollRepository;
    private final ChatNoticeRepository chatNoticeRepository;

    /**
     * 공지에 투표를 등록합니다.
     * - 채팅방 멤버면 누구나 가능
     */
    public NoticeResponse addPollNotice(Long roomId, CreatePollNoticeRequest request, Authentication authentication) {
        if (request == null || !StringUtils.hasText(request.getPollId())) {
            throw new IllegalArgumentException("pollId가 비어있습니다.");
        }

        ChatRoom room = chatService.getRoomOrThrow(roomId);
        Long requesterId = chatService.getRequesterUserId(authentication);
        chatService.ensureMember(room, requesterId);

        ChatPollDocument poll = chatPollRepository.findById(request.getPollId())
                .orElseThrow(() -> new IllegalArgumentException("투표를 찾을 수 없습니다."));

        if (poll.getChatRoomId() == null || !poll.getChatRoomId().equals(roomId)) {
            throw new IllegalArgumentException("해당 채팅방의 투표가 아닙니다.");
        }

        String displayName = userService.getDisplayNameByUserId(requesterId);

        ChatNoticeDocument notice = ChatNoticeDocument.builder()
                .chatRoomId(roomId)
                .type(ChatNoticeDocument.NoticeType.POLL)
                .createdByUserId(requesterId)
                .createdByName(displayName)
                .pollId(poll.getId())
                .message(StringUtils.hasText(request.getMessage()) ? request.getMessage().trim() : poll.getQuestion())
                .createdAt(Instant.now())
                .build();

        notice = chatNoticeRepository.save(notice);
        return toResponse(notice);
    }

    /**
     * 채팅방 공지 목록을 최신순으로 조회합니다. (채팅방 멤버만 가능)
     */
    public List<NoticeResponse> listNotices(Long roomId, Authentication authentication) {
        ChatRoom room = chatService.getRoomOrThrow(roomId);
        Long requesterId = chatService.getRequesterUserId(authentication);
        chatService.ensureMember(room, requesterId);

        List<ChatNoticeDocument> notices = chatNoticeRepository.findByChatRoomIdOrderByCreatedAtDesc(roomId);
        List<NoticeResponse> responses = new ArrayList<>();
        for (ChatNoticeDocument n : notices) {
            responses.add(toResponse(n));
        }
        return responses;
    }

    private NoticeResponse toResponse(ChatNoticeDocument n) {
        return NoticeResponse.builder()
                .noticeId(n.getId())
                .chatRoomId(n.getChatRoomId())
                .type(n.getType() == null ? null : n.getType().name())
                .createdByUserId(n.getCreatedByUserId())
                .createdByName(n.getCreatedByName())
                .pollId(n.getPollId())
                .message(n.getMessage())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
