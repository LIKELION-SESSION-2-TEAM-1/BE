package g3pjt.service.docsbot.kb;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class KnowledgeBaseService {

    private static final Pattern HEADING = Pattern.compile("^(#{1,6})\\s+(.+)$");

    public List<KbChunk> loadAllChunks() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath*:kb/*.md");

            List<KbChunk> chunks = new ArrayList<>();
            for (Resource resource : resources) {
                String sourceName = resource.getFilename() == null ? "kb/unknown.md" : ("kb/" + resource.getFilename());
                String text = readAll(resource);
                chunks.addAll(splitByHeading(sourceName, text));
            }

            if (chunks.isEmpty()) {
                log.warn("No knowledge base chunks found under classpath:kb/*.md");
            }

            return chunks;
        } catch (Exception e) {
            log.error("Failed to load knowledge base", e);
            return List.of();
        }
    }

    private String readAll(Resource resource) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        return sb.toString();
    }

    private List<KbChunk> splitByHeading(String source, String text) {
        List<KbChunk> chunks = new ArrayList<>();

        String currentTitle = "문서";
        StringBuilder current = new StringBuilder();

        String[] lines = text.split("\\R");
        for (String line : lines) {
            Matcher m = HEADING.matcher(line);
            if (m.matches()) {
                // flush
                flushChunk(chunks, source, currentTitle, current);

                currentTitle = m.group(2).trim();
                current = new StringBuilder();
            } else {
                current.append(line).append('\n');
            }
        }

        flushChunk(chunks, source, currentTitle, current);
        return chunks;
    }

    private void flushChunk(List<KbChunk> out, String source, String title, StringBuilder content) {
        String c = content.toString().trim();
        if (c.isEmpty()) return;

        // 너무 길면 잘라서(간단 RAG) 모델 프롬프트 폭주 방지
        int maxChars = 1800;
        if (c.length() > maxChars) {
            c = c.substring(0, maxChars) + "\n...";
        }

        out.add(new KbChunk(source, title, c));
    }
}
