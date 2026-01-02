package g3pjt.service.chat.repository;

import g3pjt.service.chat.domain.ChatPollVoteDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatPollVoteRepository extends MongoRepository<ChatPollVoteDocument, String> {
    Optional<ChatPollVoteDocument> findByPollIdAndUserId(String pollId, Long userId);

    List<ChatPollVoteDocument> findByPollId(String pollId);

    long countByPollIdAndOptionId(String pollId, String optionId);
}
