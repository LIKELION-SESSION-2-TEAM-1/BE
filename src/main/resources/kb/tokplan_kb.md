# TokPlan(가칭) 백엔드 지식베이스

## 서비스 개요
- 사용자 로그인(JWT/OAuth2) 후 채팅방에서 여행을 함께 계획하는 서비스입니다.
- 채팅(REST + WebSocket/STOMP), 투표/공지, AI 여행 계획 생성/저장, 즐겨찾기/최근검색, 가게/여행지 검색 기능이 있습니다.

## 인증/인가
- JWT 헤더: `Authorization: Bearer <token>`
- 회원가입/로그인 등 일부 엔드포인트는 인증 없이 접근 가능, 나머지는 인증 필요(프로젝트 Security 설정에 따름).

## 사용자(User) API 요약
Base URL: `/api/user`
- `POST /signup` 회원가입(닉네임 자동 설정)
- `POST /login` 로그인(JWT 토큰 발급)
- `GET /profile` 내 프로필 조회(인증 필요)
- `PUT /profile` 내 프로필 수정(부분 수정 지원, 인증 필요)

프로필 수정 예시
```json
{
  "nickname": "멋쟁이",
  "travelPace": "느림"
}
```

## 채팅(Chat) API 요약
Base URL: `/api/chats`
- `POST /rooms` 채팅방 생성(생성자 자동 참여, 인증 필요)
- `GET /rooms` 내 채팅방 목록(내가 멤버인 방만, 인증 필요)
- `GET /{chatRoomId}` 특정 방 과거 대화 내역(인증 필요)

### 채팅방 투표(Poll)
- `POST /rooms/{roomId}/polls` 투표 생성(생성 후 STOMP로 POLL 브로드캐스트)
- `GET /rooms/{roomId}/polls` 투표 목록(최신순)
- `GET /polls/{pollId}` 투표 상세/결과
- `POST /polls/{pollId}/votes` 투표하기(1인 1표)

투표 생성 요청 예시
```json
{
  "question": "점심 뭐 먹을까?",
  "options": ["국밥", "파스타", "샐러드"]
}
```

### 채팅방 공지(Notice)
- `POST /rooms/{roomId}/notices/polls` 투표를 공지로 등록
- `GET /rooms/{roomId}/notices` 공지 목록 조회(최신순)

## STOMP(실시간) 참고
- 채팅 SUB 경로: `/sub/chat/{roomId}`
- 투표 생성 시 브로드캐스트 메시지에 `messageType == "POLL"` 및 `pollId` 포함
- 클라이언트는 `GET /api/chats/polls/{pollId}`로 상세를 조회해 렌더링

## AI API 요약
Base URL: `/api/ai`
- `POST /keywords/{chatRoomId}` 채팅방 대화에서 키워드(여행지) 추출
- `POST /plan` 키워드 기반 여행 계획 생성
- `POST /plan/confirm` 여행 계획 확정(저장)
- `PUT /plan/{planId}` 여행 계획 수정
- `GET /plans?chatRoomId=123` 내 여행 계획 목록(선택적으로 방 기준)
- `DELETE /plan/{planId}` 여행 계획 삭제

## 즐겨찾기(Favorites) API 요약
Base URL: `/api/favorites`
- `POST /` 즐겨찾기 추가
- `GET /` 내 즐겨찾기 목록
- `DELETE /{favoriteId}` 즐겨찾기 삭제

즐겨찾기 추가 예시
```json
{
  "storeName": "파라다이스 호텔 부산",
  "category": "숙소",
  "address": "부산 ...",
  "rating": "4.6",
  "reviewCount": "0",
  "link": "",
  "imageUrl": "https://..."
}
```

## 최근 검색(Recent Searches) API
Base URL: `/api/searches/recent`
- 최근 검색 관련 엔드포인트(프로젝트 API 문서 기준)

## 챗봇(설명봇) API
Base URL: `/api/help`
- `POST /chat` 지식베이스(md)를 근거로 답변

요청 예시
```json
{
  "question": "내 채팅방 목록은 어떤 API로 조회해?",
  "chatHistory": ["사용자: ...", "봇: ..."]
}
```
