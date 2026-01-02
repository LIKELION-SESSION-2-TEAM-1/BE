package g3pjt.service.chat.repository;

import g3pjt.service.chat.domain.ChatDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChatRepository extends MongoRepository<ChatDocument, String> {
    List<ChatDocument> findByChatRoomId(Long chatRoomId);

    List<ChatDocument> findByChatRoomIdOrderByTimestampAsc(Long chatRoomId);

    Optional<ChatDocument> findFirstByChatRoomIdOrderByTimestampDesc(Long chatRoomId);

    @Query(value = "{ 'chatRoomId': { $in: ?0 }, 'message': { $regex: ?1, $options: 'i' } }")
    List<ChatDocument> searchByRoomIdsAndMessageRegex(List<Long> chatRoomIds, String regex, Pageable pageable);

    long countByChatRoomIdAndTimestampAfterAndSenderUserIdNot(Long chatRoomId, Instant timestamp, Long senderUserId);

    void deleteByChatRoomId(Long chatRoomId);
}
