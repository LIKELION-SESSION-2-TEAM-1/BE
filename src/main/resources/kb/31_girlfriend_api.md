# 가상 여자친구(여름이) API

## Base URL
- `/api/girlfriend`

## POST /api/girlfriend/chat
설명: 사용자의 메시지를 보내면 '여름이'가 답변

요청 바디
```json
{ "userMessage": "..." }
```

동작
- system prompt로 페르소나(여름이, 여행 좋아함, 반말/이모지) 고정
- Gemini API 호출

응답 바디
```json
{ "reply": "..." }
```

주의
- API 키: `GEMINI_API_KEY`
