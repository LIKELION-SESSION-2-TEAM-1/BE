# 앱 설명 챗봇(Docs Bot)

## Base URL
- `/api/help`

## POST /api/help/chat
설명: 프로젝트 지식베이스(md)에서 관련 문단을 찾아 그 근거로만 답변합니다.

요청
```json
{
  "question": "질문",
  "chatHistory": ["사용자: ...", "봇: ..."]
}
```

응답
```json
{
  "answer": "...",
  "sources": [
    {"source": "kb/10_user_api.md", "title": "사용자(User) / 인증"}
  ]
}
```

설정
- `GEMINI_API_KEY`
- (선택) `DOCSBOT_GEMINI_MODEL` 또는 `docsbot.gemini.model`

동작 요약
- classpath `kb/*.md` 로드
- 헤딩 단위로 split
- 질문과 유사한 문단 Top K(기본 4)를 프롬프트에 포함
- 문서에 없는 내용은 추측하지 말고 "문서에 없는 정보"라고 말하도록 system prompt로 강제
