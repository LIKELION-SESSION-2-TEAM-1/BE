package g3pjt.service.chat.service;

import g3pjt.service.chat.domain.ChatRoom;
import g3pjt.service.chat.repository.ChatRepository;
import g3pjt.service.chat.repository.ChatRoomRepository;
import g3pjt.service.chat.repository.ChatRoomReadStateRepository;
import g3pjt.service.storage.SupabaseStorageService;
import g3pjt.service.user.User;
import g3pjt.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ChatRoomAdminTest {

    @Test
    void deleteRoom_requiresOwner() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ChatRepository chatRepository = mock(ChatRepository.class);
        UserService userService = mock(UserService.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn("alice");

        User alice = mock(User.class);
        when(alice.getId()).thenReturn(1L);
        when(userService.getUserProfile("alice")).thenReturn(alice);

        ChatRoom room = ChatRoom.builder()
                .roomId(10L)
                .name("ccc")
                .ownerUserId(999L)
                .memberIds(List.of(1L, 2L))
                .build();

        when(chatRoomRepository.findByRoomId(10L)).thenReturn(room);

        SupabaseStorageService supabaseStorageService = mock(SupabaseStorageService.class);
        ChatRoomReadStateRepository chatRoomReadStateRepository = mock(ChatRoomReadStateRepository.class);

        ChatService service = new ChatService(chatRoomRepository, chatRepository, chatRoomReadStateRepository, userService, supabaseStorageService);

        assertThatThrownBy(() -> service.deleteRoom(10L, authentication))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("방장");

        verify(chatRepository, never()).deleteByChatRoomId(anyLong());
        verify(chatRoomRepository, never()).delete(any());
    }

    @Test
    void leaveRoom_removesOnlyMembership_keepsRoomAndChats() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ChatRepository chatRepository = mock(ChatRepository.class);
        UserService userService = mock(UserService.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn("alice");

        User alice = mock(User.class);
        when(alice.getId()).thenReturn(1L);
        when(userService.getUserProfile("alice")).thenReturn(alice);

        ChatRoom room = ChatRoom.builder()
                .roomId(10L)
                .name("ccc")
                .ownerUserId(999L)
                .memberIds(List.of(1L, 2L))
                .build();

        when(chatRoomRepository.findByRoomId(10L)).thenReturn(room);

        ArgumentCaptor<ChatRoom> savedCaptor = ArgumentCaptor.forClass(ChatRoom.class);
        when(chatRoomRepository.save(savedCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        SupabaseStorageService supabaseStorageService = mock(SupabaseStorageService.class);
        ChatRoomReadStateRepository chatRoomReadStateRepository = mock(ChatRoomReadStateRepository.class);
        ChatService service = new ChatService(chatRoomRepository, chatRepository, chatRoomReadStateRepository, userService, supabaseStorageService);

        ChatRoom updated = service.leaveRoom(10L, authentication);

        assertThat(updated.getMemberIds()).containsExactly(2L);
        assertThat(updated.getOwnerUserId()).isEqualTo(999L);

        verify(chatRepository, never()).deleteByChatRoomId(anyLong());
        verify(chatRoomRepository, never()).delete(any());
        verify(chatRoomRepository, times(1)).save(any(ChatRoom.class));
    }

    @Test
    void leaveRoom_whenOwnerLeaves_transfersOwnershipToRemainingMember() {
        ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
        ChatRepository chatRepository = mock(ChatRepository.class);
        UserService userService = mock(UserService.class);
        Authentication authentication = mock(Authentication.class);

        when(authentication.getName()).thenReturn("alice");

        User alice = mock(User.class);
        when(alice.getId()).thenReturn(1L);
        when(userService.getUserProfile("alice")).thenReturn(alice);

        ChatRoom room = ChatRoom.builder()
                .roomId(10L)
                .name("ccc")
                .ownerUserId(1L)
                .memberIds(List.of(1L, 2L, 3L))
                .build();

        when(chatRoomRepository.findByRoomId(10L)).thenReturn(room);
        when(chatRoomRepository.save(any(ChatRoom.class))).thenAnswer(inv -> inv.getArgument(0));

        SupabaseStorageService supabaseStorageService = mock(SupabaseStorageService.class);
        ChatRoomReadStateRepository chatRoomReadStateRepository = mock(ChatRoomReadStateRepository.class);
        ChatService service = new ChatService(chatRoomRepository, chatRepository, chatRoomReadStateRepository, userService, supabaseStorageService);

        ChatRoom updated = service.leaveRoom(10L, authentication);

        assertThat(updated.getMemberIds()).containsExactly(2L, 3L);
        assertThat(updated.getOwnerUserId()).isEqualTo(2L);

        verify(chatRepository, never()).deleteByChatRoomId(anyLong());
        verify(chatRoomRepository, never()).delete(any());
    }
}
