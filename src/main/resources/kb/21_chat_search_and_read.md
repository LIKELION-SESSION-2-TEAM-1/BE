# 채팅(Chat) REST API (검색/요약/읽음/히스토리)

## Base URL
- `/api/chats`

## GET /api/chats/{chatRoomId}
설명: 채팅 내역 조회

응답
- ChatDocument[] (timestamp 오름차순)

## GET /api/chats/search
설명: 통합 검색(방 이름 + 메시지) (인증 필요)

쿼리
- `keyword` (필수)
- `roomLimit` (선택, 기본 3, 최대 20)
- `messageLimit` (선택, 기본 3, 최대 20)

응답
- `rooms[]` (방 요약: roomId/name/lastMessage/lastMessageAt)
- `messages[]` (메시지 요약: messageId/roomId/roomName/senderName/message/timestamp)

## GET /api/chats/search/rooms
설명: 방 이름 검색(더보기) (인증 필요)

쿼리
- `keyword` (필수)
- `limit` (기본 20)

## GET /api/chats/search/messages
설명: 메시지 검색(더보기) (인증 필요)

쿼리
- `keyword` (필수)
- `page` (기본 0)
- `size` (기본 20, 최대 50)

정렬
- timestamp 내림차순

## GET /api/chats/rooms/summary
설명: 내 채팅방 목록 + 방별 안읽은 개수(unreadCount) (인증 필요)

동작
- 최초 조회 시 read state가 없으면 "현재 시점"으로 초기화(기존 메시지가 unread로 뜨는 것 방지)

## POST /api/chats/rooms/{roomId}/read
설명: 특정 방을 지금 시점까지 읽음 처리(방 멤버만)

응답
- 204 No Content
