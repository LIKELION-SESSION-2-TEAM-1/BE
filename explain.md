
# 앱 설명 챗봇(Docs Bot) 안내

이 문서는 "이 앱이 뭐 하는 서비스인지", "어떤 API가 있는지" 등을 설명해주는 챗봇이 참고하도록 만든 요약 문서입니다.

## 1) 챗봇이 참고하는 지식베이스 위치

- 지식베이스 파일: `src/main/resources/kb/*.md`
- 기본 제공: `src/main/resources/kb/tokplan_kb.md`

챗봇은 질문이 오면, 위 md 파일들에서 관련 문단을 간단히 검색해(키워드 기반) 그 내용만 근거로 답변합니다.

## 2) 챗봇 API

- `POST /api/help/chat`

요청 예시

```json
{
	"question": "내 채팅방 목록은 어떤 API로 조회해?",
	"chatHistory": ["사용자: ...", "봇: ..."]
}
```

응답 예시(요약)

```json
{
	"answer": "내 채팅방 목록은 GET /api/chats/rooms 로 조회합니다(인증 필요).",
	"sources": [
		{"source": "kb/tokplan_kb.md", "title": "채팅(Chat) API 요약"}
	]
}
```

## 3) 필요한 API Key(기존에 있는 키 사용)

현재 챗봇은 프로젝트에 이미 설정돼 있는 Gemini 키를 사용합니다.

- 환경변수: `GEMINI_API_KEY`
- (선택) 모델 변경: `DOCSBOT_GEMINI_MODEL` (기본값: `gemini-2.0-flash`)

`ServiceApplication`에서 `.env`를 읽어 System Property로 주입하므로,
로컬에서는 `.env`에 `GEMINI_API_KEY=...`를 넣는 방식도 가능합니다.

## 4) 지식베이스 확장 방법(추천)

추가로 문서를 넣고 싶으면 아래만 하면 됩니다.

1. `src/main/resources/kb/` 아래에 `xxx.md` 파일 추가
2. 제목(##/### 헤딩)으로 섹션을 나눠서 작성
3. 서버 재시작

챗봇은 md의 헤딩 단위로 잘라서(top 4) 질문과 함께 모델에 전달합니다.

