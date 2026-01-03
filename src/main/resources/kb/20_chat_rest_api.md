# 채팅(Chat) REST API (방/멤버/이미지)

## Base URL
- `/api/chats`

## POST /api/chats/rooms
설명: 채팅방 생성 (인증 필요)

요청 바디
```json
{
  "name": "방 이름",
  "startDate": "2025-01-01",
  "endDate": "2025-01-03",
  "travelStyle": "맛집 위주"
}
```

동작
- roomId는 `System.currentTimeMillis()` 기반
- 생성자는 memberIds에 자동 포함
- ownerUserId = 생성자

응답
- 200 OK + ChatRoom

## POST /api/chats/rooms (multipart)
설명: 대표 이미지 포함 생성(인증 필요)

요청
- `multipart/form-data`
- 파트:
  - `request`: JSON(ChatRoomRequest)
  - `file`: 이미지(선택)

## GET /api/chats/rooms
설명: 내 채팅방 목록(인증 필요)

응답
- 200 OK + ChatRoom[]

## GET /api/chats/rooms/{roomId}/members
설명: 멤버/인원 조회(방 멤버만)

응답
- `roomId`, `memberCount`, `members[{userId, displayName, profileImageUrl}]`

## POST /api/chats/rooms/{roomId}/members
설명: 멤버 추가(방 멤버만)

요청 바디
```json
{ "identifier": "닉네임 또는 username(email)" }
```

## GET /api/chats/users/search
설명: 유저 검색(멤버 추가용)

쿼리
- `identifier=...` (닉네임/username/email)

응답
- 200 OK: `userId`, `displayName`, `username`
- 404: 없음

## POST /api/chats/rooms/{roomId}/invite-link
설명: 초대 링크(코드) 생성(방 멤버만)

응답
```json
{
  "roomId": 123,
  "inviteCode": "...",
  "inviteUrl": "<FRONTEND_URL>/chat/join?roomId=...&code=..." 
}
```

## POST /api/chats/rooms/{roomId}/join
설명: 초대 코드로 참가(인증 필요)

요청 바디
```json
{ "inviteCode": "..." }
```

## POST /api/chats/rooms/{roomId}/images
설명: 채팅 이미지 업로드(방 멤버만)

요청
- `multipart/form-data`
- 파트: `file` (jpg/png/webp)

응답
```json
{ "imageUrl": "https://..." }
```

## DELETE /api/chats/rooms/{roomId}
설명: 채팅방 삭제(방장만)

동작
- Mongo 채팅 내역(chatRoomId) 삭제 후 방 삭제

## DELETE /api/chats/rooms/{roomId}/leave
설명: 방 나가기(방 멤버만)

동작
- 본인을 memberIds에서 제거
- 방장이 나가면 첫 멤버로 ownerUserId 승계(없으면 null)

## GET /api/chats/rooms/{roomId}/messages/last
설명: 특정 채팅방의 가장 최근 메시지 1건 조회(방 멤버만)

응답
- 200 OK + ChatDocument
- 204 No Content: 메시지가 아직 없는 방
- 403 Forbidden: 방 멤버가 아님
- 404 Not Found: 방이 없음
