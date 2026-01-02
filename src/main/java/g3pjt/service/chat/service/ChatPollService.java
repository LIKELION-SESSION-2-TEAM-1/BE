package g3pjt.service.chat.service;

import g3pjt.service.chat.domain.ChatDocument;
import g3pjt.service.chat.domain.ChatPollDocument;
import g3pjt.service.chat.domain.ChatPollVoteDocument;
import g3pjt.service.chat.domain.ChatRoom;
import g3pjt.service.chat.dto.ChatDto;
import g3pjt.service.chat.dto.CreatePollRequest;
import g3pjt.service.chat.dto.PollResponse;
import g3pjt.service.chat.dto.VotePollRequest;
import g3pjt.service.chat.repository.ChatPollRepository;
import g3pjt.service.chat.repository.ChatPollVoteRepository;
import g3pjt.service.chat.repository.ChatRepository;
import g3pjt.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatPollService {

    private final ChatService chatService;
    private final UserService userService;

    private final ChatRepository chatRepository;
    private final ChatPollRepository chatPollRepository;
    private final ChatPollVoteRepository chatPollVoteRepository;

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 투표를 생성하고, 채팅 타임라인에 POLL 메시지를 저장/브로드캐스트합니다.
     * - 채팅방 멤버만 가능
     * - options는 공백 제외 후 최소 2개
     */
    public PollResponse createPoll(Long roomId, CreatePollRequest request, Authentication authentication) {
        if (request == null) throw new IllegalArgumentException("요청이 비어있습니다.");
        if (!StringUtils.hasText(request.getQuestion())) throw new IllegalArgumentException("question이 비어있습니다.");
        if (request.getOptions() == null || request.getOptions().size() < 2) {
            throw new IllegalArgumentException("options는 최소 2개 이상이어야 합니다.");
        }

        ChatRoom room = chatService.getRoomOrThrow(roomId);
        Long requesterId = chatService.getRequesterUserId(authentication);
        chatService.ensureMember(room, requesterId);

        String displayName = userService.getDisplayNameByUserId(requesterId);

        List<ChatPollDocument.PollOption> options = new ArrayList<>();
        for (String raw : request.getOptions()) {
            if (!StringUtils.hasText(raw)) continue;
            options.add(ChatPollDocument.PollOption.builder()
                    .optionId(UUID.randomUUID().toString())
                    .text(raw.trim())
                    .build());
        }
        if (options.size() < 2) {
            throw new IllegalArgumentException("options는 공백이 아닌 값이 최소 2개 이상이어야 합니다.");
        }

        ChatPollDocument poll = ChatPollDocument.builder()
                .chatRoomId(roomId)
                .createdByUserId(requesterId)
                .createdByName(displayName)
                .question(request.getQuestion().trim())
                .options(options)
                .createdAt(Instant.now())
                .build();

        poll = chatPollRepository.save(poll);

        // 채팅 타임라인에 POLL 메시지로 저장 + 브로드캐스트
        ChatDocument chatDocument = ChatDocument.builder()
                .chatRoomId(roomId)
                .senderUserId(requesterId)
                .senderName(displayName)
                .message(poll.getQuestion())
                .pollId(poll.getId())
                .messageType(ChatDto.MessageType.POLL)
                .timestamp(poll.getCreatedAt())
                .build();
        chatRepository.save(chatDocument);

        ChatDto dto = ChatDto.builder()
                .chatRoomId(roomId)
                .senderUserId(requesterId)
                .senderName(displayName)
                .message(poll.getQuestion())
                .pollId(poll.getId())
                .messageType(ChatDto.MessageType.POLL)
                .timestamp(poll.getCreatedAt())
                .build();
        messagingTemplate.convertAndSend("/sub/chat/" + roomId, dto);

        return toResponse(poll, Optional.empty(), authentication);
    }

    /**
     * 채팅방 투표 목록을 최신순으로 조회합니다. (채팅방 멤버만 가능)
     */
    public List<PollResponse> listPolls(Long roomId, Authentication authentication) {
        ChatRoom room = chatService.getRoomOrThrow(roomId);
        Long requesterId = chatService.getRequesterUserId(authentication);
        chatService.ensureMember(room, requesterId);

        List<ChatPollDocument> polls = chatPollRepository.findByChatRoomIdOrderByCreatedAtDesc(roomId);
        List<PollResponse> responses = new ArrayList<>();
        for (ChatPollDocument poll : polls) {
            responses.add(toResponse(poll, Optional.empty(), authentication));
        }
        return responses;
    }

    /**
     * 투표 상세/결과를 조회합니다. (채팅방 멤버만 가능)
     */
    public PollResponse getPoll(String pollId, Authentication authentication) {
        if (!StringUtils.hasText(pollId)) throw new IllegalArgumentException("pollId가 비어있습니다.");

        ChatPollDocument poll = chatPollRepository.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("투표를 찾을 수 없습니다."));

        ChatRoom room = chatService.getRoomOrThrow(poll.getChatRoomId());
        Long requesterId = chatService.getRequesterUserId(authentication);
        chatService.ensureMember(room, requesterId);

        Optional<ChatPollVoteDocument> myVote = chatPollVoteRepository.findByPollIdAndUserId(pollId, requesterId);
        return toResponse(poll, myVote, authentication);
    }

    /**
     * 투표하기 (1인 1표). 이미 투표했으면 예외를 던집니다. (채팅방 멤버만 가능)
     */
    public PollResponse vote(String pollId, VotePollRequest request, Authentication authentication) {
        if (!StringUtils.hasText(pollId)) throw new IllegalArgumentException("pollId가 비어있습니다.");
        if (request == null || !StringUtils.hasText(request.getOptionId())) {
            throw new IllegalArgumentException("optionId가 비어있습니다.");
        }

        ChatPollDocument poll = chatPollRepository.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("투표를 찾을 수 없습니다."));

        ChatRoom room = chatService.getRoomOrThrow(poll.getChatRoomId());
        Long requesterId = chatService.getRequesterUserId(authentication);
        chatService.ensureMember(room, requesterId);

        boolean optionExists = poll.getOptions().stream().anyMatch(o -> request.getOptionId().equals(o.getOptionId()));
        if (!optionExists) throw new IllegalArgumentException("optionId가 올바르지 않습니다.");

        Optional<ChatPollVoteDocument> existing = chatPollVoteRepository.findByPollIdAndUserId(pollId, requesterId);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("이미 투표하셨습니다.");
        }

        ChatPollVoteDocument vote = ChatPollVoteDocument.builder()
                .id(pollId + ":" + requesterId)
                .pollId(pollId)
                .chatRoomId(poll.getChatRoomId())
                .userId(requesterId)
                .optionId(request.getOptionId())
                .createdAt(Instant.now())
                .build();
        chatPollVoteRepository.save(vote);

        return toResponse(poll, Optional.of(vote), authentication);
    }

    private PollResponse toResponse(ChatPollDocument poll, Optional<ChatPollVoteDocument> myVote, Authentication authentication) {
        Long requesterId = chatService.getRequesterUserId(authentication);
        Optional<ChatPollVoteDocument> voteDoc = myVote;
        if (voteDoc == null || voteDoc.isEmpty()) {
            voteDoc = chatPollVoteRepository.findByPollIdAndUserId(poll.getId(), requesterId);
        }

        List<PollResponse.PollOptionResult> optionResults = new ArrayList<>();
        for (ChatPollDocument.PollOption option : poll.getOptions()) {
            long count = chatPollVoteRepository.countByPollIdAndOptionId(poll.getId(), option.getOptionId());
            optionResults.add(PollResponse.PollOptionResult.builder()
                    .optionId(option.getOptionId())
                    .text(option.getText())
                    .voteCount(count)
                    .build());
        }

        return PollResponse.builder()
                .pollId(poll.getId())
                .chatRoomId(poll.getChatRoomId())
                .createdByUserId(poll.getCreatedByUserId())
                .createdByName(poll.getCreatedByName())
                .question(poll.getQuestion())
                .options(optionResults)
                .myVotedOptionId(voteDoc.map(ChatPollVoteDocument::getOptionId).orElse(null))
                .createdAt(poll.getCreatedAt())
                .build();
    }
}
