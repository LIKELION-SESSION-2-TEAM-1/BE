package g3pjt.service.chat.service;

import g3pjt.service.chat.domain.ChatPollDocument;
import g3pjt.service.chat.domain.ChatRoom;
import g3pjt.service.chat.dto.PollResponse;
import g3pjt.service.chat.dto.VotePollRequest;
import g3pjt.service.chat.repository.ChatPollRepository;
import g3pjt.service.chat.repository.ChatPollVoteRepository;
import g3pjt.service.chat.repository.ChatRepository;
import g3pjt.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatPollAdminTest {

    @Test
    void closePoll_requiresCreator() {
        ChatService chatService = mock(ChatService.class);
        UserService userService = mock(UserService.class);

        ChatRepository chatRepository = mock(ChatRepository.class);
        ChatPollRepository chatPollRepository = mock(ChatPollRepository.class);
        ChatPollVoteRepository chatPollVoteRepository = mock(ChatPollVoteRepository.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);

        Authentication authentication = mock(Authentication.class);

        ChatPollService service = new ChatPollService(
                chatService,
                userService,
                chatRepository,
                chatPollRepository,
                chatPollVoteRepository,
                messagingTemplate
        );

        ChatPollDocument poll = ChatPollDocument.builder()
                .id("poll1")
                .chatRoomId(10L)
                .createdByUserId(2L)
                .createdByName("bob")
                .question("Q")
                .createdAt(Instant.now())
                .build();

        when(chatPollRepository.findById("poll1")).thenReturn(java.util.Optional.of(poll));
        when(chatService.getRequesterUserId(authentication)).thenReturn(1L);
        when(chatService.getRoomOrThrow(10L)).thenReturn(ChatRoom.builder().roomId(10L).memberIds(List.of(1L, 2L)).build());
        doNothing().when(chatService).ensureMember(any(ChatRoom.class), eq(1L));

        assertThatThrownBy(() -> service.closePoll("poll1", authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("생성자");

        verify(chatPollRepository, never()).save(any());
    }

    @Test
    void closePoll_setsClosedAtAndClosedBy() {
        ChatService chatService = mock(ChatService.class);
        UserService userService = mock(UserService.class);

        ChatRepository chatRepository = mock(ChatRepository.class);
        ChatPollRepository chatPollRepository = mock(ChatPollRepository.class);
        ChatPollVoteRepository chatPollVoteRepository = mock(ChatPollVoteRepository.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);

        Authentication authentication = mock(Authentication.class);

        ChatPollService service = new ChatPollService(
                chatService,
                userService,
                chatRepository,
                chatPollRepository,
                chatPollVoteRepository,
                messagingTemplate
        );

        ChatPollDocument poll = ChatPollDocument.builder()
                .id("poll1")
                .chatRoomId(10L)
                .createdByUserId(2L)
                .createdByName("bob")
                .question("Q")
                .options(List.of(ChatPollDocument.PollOption.builder().optionId("o1").text("A").build(),
                        ChatPollDocument.PollOption.builder().optionId("o2").text("B").build()))
                .createdAt(Instant.now())
                .build();

        when(chatPollRepository.findById("poll1")).thenReturn(java.util.Optional.of(poll));
        when(chatService.getRequesterUserId(authentication)).thenReturn(2L);
        when(chatService.getRoomOrThrow(10L)).thenReturn(ChatRoom.builder().roomId(10L).memberIds(List.of(2L)).build());
        doNothing().when(chatService).ensureMember(any(ChatRoom.class), eq(2L));

        when(chatPollVoteRepository.findByPollIdAndUserId(anyString(), anyLong())).thenReturn(java.util.Optional.empty());
        when(chatPollVoteRepository.countByPollIdAndOptionId(anyString(), anyString())).thenReturn(0L);

        ArgumentCaptor<ChatPollDocument> savedCaptor = ArgumentCaptor.forClass(ChatPollDocument.class);
        when(chatPollRepository.save(savedCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        PollResponse response = service.closePoll("poll1", authentication);

        ChatPollDocument saved = savedCaptor.getValue();
        assertThat(saved.getClosedAt()).isNotNull();
        assertThat(saved.getClosedByUserId()).isEqualTo(2L);

        assertThat(response.isClosed()).isTrue();
        assertThat(response.getClosedAt()).isNotNull();
        assertThat(response.getClosedByUserId()).isEqualTo(2L);
    }

    @Test
    void vote_whenClosed_throws() {
        ChatService chatService = mock(ChatService.class);
        UserService userService = mock(UserService.class);

        ChatRepository chatRepository = mock(ChatRepository.class);
        ChatPollRepository chatPollRepository = mock(ChatPollRepository.class);
        ChatPollVoteRepository chatPollVoteRepository = mock(ChatPollVoteRepository.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);

        Authentication authentication = mock(Authentication.class);

        ChatPollService service = new ChatPollService(
                chatService,
                userService,
                chatRepository,
                chatPollRepository,
                chatPollVoteRepository,
                messagingTemplate
        );

        ChatPollDocument poll = ChatPollDocument.builder()
                .id("poll1")
                .chatRoomId(10L)
                .createdByUserId(2L)
                .createdByName("bob")
                .question("Q")
                .options(List.of(ChatPollDocument.PollOption.builder().optionId("o1").text("A").build(),
                        ChatPollDocument.PollOption.builder().optionId("o2").text("B").build()))
                .createdAt(Instant.now())
                .closedAt(Instant.now())
                .closedByUserId(2L)
                .build();

        when(chatPollRepository.findById("poll1")).thenReturn(java.util.Optional.of(poll));

        VotePollRequest req = VotePollRequest.builder().optionId("o1").build();

        assertThatThrownBy(() -> service.vote("poll1", req, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("종료된");

        verify(chatPollVoteRepository, never()).save(any());
    }
}
