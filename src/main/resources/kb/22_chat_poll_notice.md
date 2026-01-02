# 채팅 투표(Poll) / 공지(Notice)

## Base URL
- `/api/chats`

## Poll

### POST /api/chats/rooms/{roomId}/polls
설명: 투표 생성 + 채팅 타임라인에 POLL 메시지 저장 + STOMP 브로드캐스트

요청 바디
```json
{
  "question": "점심 뭐 먹을까?",
  "options": ["국밥", "파스타", "샐러드"]
}
```

규칙
- options는 공백 제거 후 최소 2개

응답
- PollResponse

### GET /api/chats/rooms/{roomId}/polls
설명: 방 투표 목록(최신순)

### GET /api/chats/polls/{pollId}
설명: 투표 상세/결과

응답
- options에 `voteCount` 포함
- 내 투표는 `myVotedOptionId`에 표시

### POST /api/chats/polls/{pollId}/votes
설명: 투표하기(1인 1표)

요청 바디
```json
{ "optionId": "..." }
```

오류
- 이미 투표했으면 400("이미 투표하셨습니다.")

### POST /api/chats/polls/{pollId}/close
설명: 투표 종료(마감)

규칙
- 투표 생성자만 가능
- 종료된 투표는 추가 투표 불가(400)

## Notice

### POST /api/chats/rooms/{roomId}/notices/polls
설명: 투표를 공지로 등록(방 멤버면 누구나)

요청 바디
```json
{
  "pollId": "...",
  "message": "투표 참여 부탁!" 
}
```

동작
- message가 비어있으면 poll.question 사용

### GET /api/chats/rooms/{roomId}/notices
설명: 공지 목록(최신순)
