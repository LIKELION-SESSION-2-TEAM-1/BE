# ✈️ Back-end API 명세 (Update)

## 📌 주요 변경 사항 (필독)
1. **회원가입 (`POST /api/user/signup`)**
   - 이제 회원가입 시 `username`(아이디)과 `password`만 보내면, **초기 닉네임은 아이디와 동일하게 자동 설정**됩니다.
   - 닉네임이 `null`이라서 발생하는 문제를 해결했습니다.

2. **프로필 수정 (`PUT /api/user/profile`) - 부분 수정 지원**
   - 모든 필드를 다 보낼 필요가 없습니다. **변경하고 싶은 필드만** 보내세요.
   - 보내지 않은 필드(Key가 없거나 Value가 `null`)는 기존 값이 유지됩니다.
   - 물론, 기존처럼 모든 데이터를 JSON에 담아 보내도 정상 작동합니다 (덮어쓰기).

3. **내 채팅방 조회 (`GET /api/chats/rooms`)**
   - 이제 모든 방을 불러오지 않고, **로그인한 유저가 참여 중인 채팅방만** 반환합니다.

---

## 📚 1. 사용자 (User)
**Base URL**: `/api/user`

| Method | Endpoint | 설명 | 인증 |
| :--- | :--- | :--- | :--- |
| `POST` | `/signup` | 회원가입 (닉네임 자동 설정됨) | X |
| `POST` | `/login` | 로그인 (JWT 토큰 발급) | X |
| `GET` | `/profile` | 내 프로필 조회 | O |
| `PUT` | `/profile` | 내 프로필 수정 (**부분 수정 가능**) | O |

### ✏️ 프로필 수정 예시 Payload
```json
{
  "nickname": "멋쟁이",
  "travelPace": "느림" 
  // 다른 필드를 안 보내면 기존 값 유지됨
}
```

---

## 💬 2. 채팅 (Chat)
**Base URL**: `/api/chats`

| Method | Endpoint | 설명 | 인증 |
| :--- | :--- | :--- | :--- |
| `POST` | `/rooms` | 채팅방 생성 (생성자 자동 참여) | O |
| `GET` | `/rooms` | **내 채팅방 목록 조회** (내가 멤버인 방만) | O |
| `GET` | `/{chatRoomId}` | 특정 방의 과거 대화 내역 조회 | O |

### 🗳️ 채팅방 투표 (Poll)

| Method | Endpoint | 설명 | 인증 |
| :--- | :--- | :--- | :--- |
| `POST` | `/rooms/{roomId}/polls` | 투표 생성 (생성 후 STOMP로 POLL 브로드캐스트) | O |
| `GET` | `/rooms/{roomId}/polls` | 투표 목록 조회(최신순) | O |
| `GET` | `/polls/{pollId}` | 투표 상세/결과 조회 | O |
| `POST` | `/polls/{pollId}/votes` | 투표하기(1인 1표) | O |

#### 투표 생성 요청
`POST /api/chats/rooms/{roomId}/polls`
```json
{
   "question": "점심 뭐 먹을까?",
   "options": ["국밥", "파스타", "샐러드"]
}
```

#### 투표 응답(요약)
```json
{
   "pollId": "675d...",
   "chatRoomId": 123,
   "createdByUserId": 1,
   "createdByName": "홍길동",
   "question": "점심 뭐 먹을까?",
   "options": [
      {"optionId": "a1...", "text": "국밥", "voteCount": 0},
      {"optionId": "b2...", "text": "파스타", "voteCount": 0}
   ],
   "myVotedOptionId": null,
   "createdAt": "2025-12-30T00:00:00Z"
}
```

#### 투표하기 요청
`POST /api/chats/polls/{pollId}/votes`
```json
{ "optionId": "a1..." }
```

#### STOMP(실시간) 전달사항
- 기존 채팅 SUB 경로 그대로 사용: `/sub/chat/{roomId}`
- 투표 생성 시 아래 형태의 메시지가 브로드캐스트됩니다.
```json
{
   "chatRoomId": 123,
   "senderUserId": 1,
   "senderName": "홍길동",
   "message": "점심 뭐 먹을까?",
   "pollId": "675d...",
   "messageType": "POLL",
   "timestamp": "2025-12-30T00:00:00Z"
}
```
- 클라이언트는 `messageType == "POLL"`이면 `pollId`로 `GET /api/chats/polls/{pollId}`를 호출해 옵션/결과를 렌더링하면 됩니다.

### 📢 채팅방 공지 (Notice)

| Method | Endpoint | 설명 | 인증 |
| :--- | :--- | :--- | :--- |
| `POST` | `/rooms/{roomId}/notices/polls` | 투표를 공지로 등록 (방 멤버면 누구나 가능) | O |
| `GET` | `/rooms/{roomId}/notices` | 공지 목록 조회(최신순) | O |

#### 투표를 공지로 등록 요청
`POST /api/chats/rooms/{roomId}/notices/polls`
```json
{
   "pollId": "675d...",
   "message": "투표 참여 부탁!" 
}
```

#### 공지 목록 응답(요약)
```json
[
   {
      "noticeId": "676e...",
      "chatRoomId": 123,
      "type": "POLL",
      "createdByUserId": 2,
      "createdByName": "김철수",
      "pollId": "675d...",
      "message": "투표 참여 부탁!",
      "createdAt": "2025-12-30T00:00:00Z"
   }
]
```

### 💡 참고 사항
- 채팅방 생성 시 `?name=방이름` 쿼리 파라미터 필수.
- 이제 방을 만들면 본인이 자동으로 `memberIds`에 추가됩니다.

---

## 🤖 3. AI & 4. 가게 정보 (기존 동일)
 - `/api/ai/**`
 - `/api/stores/**`

### 🧭 AI 여행 계획(Plan) - 방 기준(`chatRoomId`) 저장/조회

프론트 변경에 맞춰 백엔드가 `chatRoomId`를 지원합니다.

- `POST /api/ai/plan/confirm`
   - Body에 `chatRoomId`를 포함한 전체 플랜 JSON을 전달하면 해당 방 기준으로 저장됩니다.
   - 예시 Body:
   ```json
   {
      "chatRoomId": 123,
      "title": "제주도 힐링 여행",
      "description": "아름다운 해변과 맛집을 탐방",
      "schedule": [
         { "day": 1, "places": [ { "name": "협재 해변", "category": "관광지" } ] }
      ]
   }
   ```

- `GET /api/ai/plans?chatRoomId=123`
   - 쿼리 파라미터로 `chatRoomId`가 오면: 로그인 사용자 + 해당 `chatRoomId`에 저장된 플랜만 반환합니다.
   - 파라미터가 없으면: 로그인 사용자의 모든 플랜을 반환합니다.

- 중요: 기존에 저장된 플랜은 `chatRoomId = null`일 수 있습니다.
   - 방 기준 조회에서는 보이지 않는 것이 정상입니다.
   - 해결 방법: (a) DB 마이그레이션으로 채워넣거나, (b) 프론트에서 해당 플랜을 다시 확정(confirm)하여 저장.

---

## 📖 4. 앱 설명 챗봇 (Docs Bot)
**Base URL**: `/api/help`

| Method | Endpoint | 설명 | 인증 |
| :--- | :--- | :--- | :--- |
| `POST` | `/chat` | 지식베이스(md)를 근거로 앱/백엔드 설명 답변 | (현재 설정상) X |

### POST /api/help/chat (Body 예시)
```json
{
   "question": "내 채팅방 목록은 어떤 API로 조회해?",
   "chatHistory": ["사용자: ...", "봇: ..."]
}
```

---

## ⭐ 5. 여행지 즐겨찾기 (Favorites)
**Base URL**: `/api/favorites`

| Method | Endpoint | 설명 | 인증 |
| :--- | :--- | :--- | :--- |
| `POST` | `/` | 즐겨찾기 추가 | O |
| `GET` | `/` | 내 즐겨찾기 목록 조회 | O |
| `DELETE` | `/{favoriteId}` | 즐겨찾기 삭제 | O |

### POST /api/favorites (Body 예시)
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

---

## 🔎 6. 최근 검색 (Recent Searches)
**Base URL**: `/api/searches/recent`

| Method | Endpoint | 설명 | 인증 |
| :--- | :--- | :--- | :--- |
| `GET` | `/` | 내 최근 검색(검색어) 목록 | O |
| `POST` | `/` | 최근 검색 추가(자동저장 ON일 때만 저장) | O |
| `DELETE` | `/` | 최근 검색 전체 삭제 | O |
| `DELETE` | `/{recentSearchId}` | 최근 검색 1개 삭제 | O |

### POST /api/searches/recent (Body 예시)
```json
{ "keyword": "부산" }
```

### 자동저장 토글
최근 검색 자동저장 ON/OFF는 기존 프로필 수정 API로 제어합니다.

- `PUT /api/user/profile`
```json
{ "recentSearchEnabled": false }
```

---

## ✅ 연결 테스트 체크리스트 (프론트)
1. **회원가입**: 가입 후 바로 `/api/user/profile` 조회했을 때 `nickname`이 아이디와 같은지 확인.
2. **프로필 수정**: 닉네임만 바꿔보고, 다른 정보(생일 등)가 날라가지 않고 유지되는지 확인.
3. **채팅방**: 방을 하나 만들고 목록 조회(`GET /rooms`) 시 그 방이 나오는지 확인.
4. **AI 플랜**: `POST /api/ai/plan/confirm` 시 Body에 `chatRoomId` 포함 → `GET /api/ai/plans?chatRoomId=해당값`으로 조회 시 화면에 일정이 표시되는지 확인.

---

## 💬 7. 채팅 - 안 읽은 메시지(Unread)

프론트에서 채팅방 목록에 **안 읽은 메시지 개수 배지**를 표시하기 위한 최소 API입니다.

### 7-1) 내 채팅방 목록(안읽은 개수 포함)
- `GET /api/chats/rooms/summary`
- Header: `Authorization: Bearer {Token}`
- 응답 예시:
```json
[
   {
      "roomId": 123,
      "name": "부산 여행",
      "startDate": "2025-01-01",
      "endDate": "2025-01-03",
      "travelStyle": "느긋하게",
      "createdAt": "2025-12-30T12:34:56",
      "ownerUserId": 1,
      "unreadCount": 5
   }
]
```

### 7-2) 읽음 처리(배지 0으로)
- `POST /api/chats/rooms/{roomId}/read`
- Header: `Authorization: Bearer {Token}`
- 동작: 해당 방을 "지금 시점까지 읽음" 처리하여 `unreadCount`가 0이 되도록 합니다.

---

## 🔍 8. 채팅 검색 (전체/채팅방/메시지)

검색 화면(탭: 전체/채팅방/메시지) 구현을 위한 API입니다.

### 8-1) 전체 검색(요약)
- `GET /api/chats/search?keyword=부산`
- Header: `Authorization: Bearer {Token}`
- 설명: 채팅방 이름 검색 결과 + 메시지 내용 검색 결과를 **요약(기본 3개씩)** 으로 함께 반환
- 응답 예시:
```json
{
   "rooms": [
      { "roomId": 123, "name": "부산 여행", "lastMessage": "아놔 ai가 일정 짜줬다", "lastMessageAt": "2025-12-30T10:15:00Z" }
   ],
   "messages": [
      { "messageId": "...", "roomId": 123, "roomName": "부산 여행", "senderUserId": 1, "senderName": "부산여행", "message": "부산까지 가는데 2박은 있어야지", "timestamp": "2025-09-20T00:00:00Z" }
   ]
}
```

### 8-2) 채팅방 검색(더보기)
- `GET /api/chats/search/rooms?keyword=부산&limit=20`
- Header: `Authorization: Bearer {Token}`
- 응답: `ChatRoomSearchResponse[]`

### 8-3) 메시지 검색(더보기)
- `GET /api/chats/search/messages?keyword=부산&page=0&size=20`
- Header: `Authorization: Bearer {Token}`
- 정렬: `timestamp` 내림차순
- 응답: `ChatMessageSearchResponse[]`

---

## 🖼️ 9. 채팅방 대표 이미지

채팅방 생성 시 대표 이미지를 함께 업로드할 수 있습니다.

### 9-1) 채팅방 생성(대표 이미지 포함)
- `POST /api/chats/rooms`
- Header: `Authorization: Bearer {Token}`
- Content-Type: `multipart/form-data`

폼 데이터
- `request` (JSON): 채팅방 생성 요청
- `file` (이미지, optional): 채팅방 대표 이미지 (jpg/png/webp)

`request` 예시(JSON)
```json
{
   "name": "부산 여행",
   "startDate": "2025-09-28",
   "endDate": "2025-09-30",
   "travelStyle": "food"
}
```

응답
- 기존 채팅방 생성 응답과 동일 + `imageUrl` 포함
