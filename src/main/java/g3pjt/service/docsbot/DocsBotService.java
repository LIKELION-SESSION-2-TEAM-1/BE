package g3pjt.service.docsbot;

import g3pjt.service.docsbot.dto.DocsBotChatRequest;
import g3pjt.service.docsbot.dto.DocsBotChatResponse;
import g3pjt.service.docsbot.kb.KbChunk;
import g3pjt.service.docsbot.kb.KbSearchService;
import g3pjt.service.docsbot.kb.KnowledgeBaseService;
import g3pjt.service.docsbot.llm.GeminiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocsBotService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KbSearchService kbSearchService;
    private final GeminiClient geminiClient;

    public DocsBotChatResponse chat(DocsBotChatRequest request) {
        String question = request == null ? null : request.getQuestion();
        if (question == null || question.trim().isEmpty()) {
            return new DocsBotChatResponse("질문을 입력해줘.", List.of());
        }

        List<KbChunk> allChunks = knowledgeBaseService.loadAllChunks();
        List<KbChunk> top = kbSearchService.topK(allChunks, question, 4);

        String systemPrompt = String.join("\n",
            "너는 이 프로젝트(여행/채팅/AI 계획 서비스)의 '도움말/설명 챗봇'이야.",
            "기본적으로 '사용자 입장'에서 기능과 사용 방법을 안내해.",
            "개발자(API/엔드포인트/필드) 설명은 사용자가 명확히 개발자 관점으로 질문했을 때만 제공해.",
            "반드시 아래 제공되는 [지식베이스] 내용만 근거로 답해.",
            "지식베이스에 없는 내용은 추측하지 말고, '문서에 없는 정보'라고 말한 다음 필요한 정보를 1~2개 질문해.",
            "답변은 한국어로, 짧고 단계적으로(1~4단계) 정리해."
        );

        String userPrompt = buildUserPrompt(question, request.getChatHistory(), top);

        String answer = geminiClient.generate(systemPrompt, userPrompt, 0.2);
        if (answer == null || answer.isBlank()) {
            answer = "문서 기반으로 답변 생성에 실패했어. 질문을 조금 더 구체적으로 적어줘.";
        }

        List<DocsBotChatResponse.Source> sources = new ArrayList<>();
        for (KbChunk c : top) {
            sources.add(new DocsBotChatResponse.Source(c.getSource(), c.getTitle()));
        }

        return new DocsBotChatResponse(answer.trim(), sources);
    }

    private String buildUserPrompt(String question, List<String> history, List<KbChunk> top) {
        StringBuilder sb = new StringBuilder();

        if (history != null && !history.isEmpty()) {
            sb.append("[대화 이력]\n");
            for (String h : history) {
                if (h == null || h.isBlank()) continue;
                sb.append(h).append('\n');
            }
            sb.append('\n');
        }

        sb.append("[지식베이스]\n");
        if (top == null || top.isEmpty()) {
            sb.append("(관련 문서 조각을 찾지 못함)\n\n");
        } else {
            for (KbChunk c : top) {
                sb.append("---\n");
                sb.append("source: ").append(c.getSource()).append("\n");
                sb.append("title: ").append(c.getTitle()).append("\n");
                sb.append(c.getContent()).append("\n");
            }
            sb.append("---\n\n");
        }

        sb.append("[질문]\n");
        sb.append(question).append('\n');

        return sb.toString();
    }
}
