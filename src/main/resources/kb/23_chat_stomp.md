# 채팅(WebSocket/STOMP)

## 엔드포인트
- 서버 WebSocket 엔드포인트: `/stomp-ws/**` (Security 설정에서 permit)

## Pub/Sub 경로
- 클라이언트 SEND: `/pub/chat/{chatRoomId}`
- 클라이언트 SUB: `/sub/chat/{chatRoomId}`

## 메시지 포맷(ChatDto)
필드(요약)
- `chatRoomId`
- `senderUserId`, `senderName`
- `receiverUserId`, `receiverName` (DM일 때)
- `message`
- `imageUrl` (IMAGE일 때)
- `pollId` (POLL일 때)
- `messageType`: `JOIN|TALK|LEAVE|DM|IMAGE|POLL`
- `timestamp` (alias: `ts`)

## 서버 동작 요약
- STOMP 메시지 수신 시:
  - senderName을 userId 기반 displayName으로 재설정
  - timestamp를 서버 현재 시각으로 설정
  - MongoDB(ChatDocument)에 저장
  - `/sub/chat/{chatRoomId}`로 브로드캐스트
- DM(`messageType == DM`)이고 `receiverUserId`가 있으면
  - `convertAndSendToUser(String.valueOf(receiverUserId), "/queue/dm", dto)`로 전송
  - 즉, 수신자는 `/user/queue/dm` 구독 형태로 수신(사용자 식별은 userId 문자열)
