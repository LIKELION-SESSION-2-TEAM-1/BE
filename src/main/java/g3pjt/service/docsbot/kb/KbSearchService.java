package g3pjt.service.docsbot.kb;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class KbSearchService {

    // 한글/영문/숫자만 토큰으로 취급 (구두점/특수문자 제거)
    private static final Pattern SPLIT = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHangul}]+");

    public List<KbChunk> topK(List<KbChunk> chunks, String query, int k) {
        if (chunks == null || chunks.isEmpty()) return List.of();
        if (query == null || query.isBlank()) return List.of();

        List<String> qTokens = tokenize(query);
        if (qTokens.isEmpty()) return List.of();

        List<Scored> scored = new ArrayList<>(chunks.size());
        for (KbChunk chunk : chunks) {
            double score = score(chunk, qTokens);
            if (score > 0) {
                scored.add(new Scored(chunk, score));
            }
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));
        int end = Math.min(k, scored.size());
        List<KbChunk> result = new ArrayList<>(end);
        for (int i = 0; i < end; i++) {
            result.add(scored.get(i).chunk);
        }
        return result;
    }

    private double score(KbChunk chunk, List<String> qTokens) {
        String title = safeLower(chunk.getTitle());
        String content = safeLower(chunk.getContent());

        Map<String, Integer> tf = new HashMap<>();
        for (String t : tokenize(title + " " + content)) {
            tf.merge(t, 1, Integer::sum);
        }

        double s = 0;
        for (String qt : qTokens) {
            int f = tf.getOrDefault(qt, 0);
            if (f > 0) {
                s += Math.min(3, f); // 과도한 반복 가중치 제한
                if (title.contains(qt)) {
                    s += 2.0; // 제목 매칭 보너스
                }
            }
        }
        return s;
    }

    private List<String> tokenize(String s) {
        if (s == null) return List.of();
        String[] raw = SPLIT.split(s.toLowerCase(Locale.ROOT));
        List<String> out = new ArrayList<>(raw.length);
        for (String t : raw) {
            if (t == null) continue;
            String tt = t.trim();
            if (tt.length() < 2) continue;
            out.add(tt);
        }
        return out;
    }

    private String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    private static class Scored {
        final KbChunk chunk;
        final double score;

        private Scored(KbChunk chunk, double score) {
            this.chunk = chunk;
            this.score = score;
        }
    }
}
