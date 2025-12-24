package g3pjt.service.chat.service;

import g3pjt.service.chat.domain.ChatRoom;
import g3pjt.service.chat.dto.ChatRoomRequest;
import g3pjt.service.chat.repository.ChatRepository;
import g3pjt.service.chat.repository.ChatRoomRepository;
import g3pjt.service.user.User;
import g3pjt.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    @Test
    void createRoom_savesTripFieldsAndAddsCreatorAsMember() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ChatRepository chatRepository = mock(ChatRepository.class);
        UserService userService = mock(UserService.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn("alice");

        User user = mock(User.class);
        when(user.getId()).thenReturn(123L);
        when(userService.getUserProfile("alice")).thenReturn(user);

        ChatRoomRequest request = ChatRoomRequest.builder()
                .name("부산 여행")
                .startDate(LocalDate.of(2025, 9, 28))
            .endDate(LocalDate.of(2025, 9, 30))
                .travelStyle("food")
                .build();

        ArgumentCaptor<ChatRoom> roomCaptor = ArgumentCaptor.forClass(ChatRoom.class);
        when(chatRoomRepository.save(roomCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        ChatService service = new ChatService(chatRoomRepository, chatRepository, userService);
        ChatRoom saved = service.createRoom(request, authentication);

        assertThat(saved.getName()).isEqualTo("부산 여행");
        assertThat(saved.getStartDate()).isEqualTo(LocalDate.of(2025, 9, 28));
        assertThat(saved.getEndDate()).isEqualTo(LocalDate.of(2025, 9, 30));
        assertThat(saved.getTravelStyle()).isEqualTo("food");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getMemberIds()).containsExactly(123L);

        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
    }
}
