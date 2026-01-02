# AI 여행 계획 API

## Base URL
- `/api/ai`

## POST /api/ai/keywords/{chatRoomId}
설명: 채팅 히스토리에서 키워드 추출

동작
- Mongo 채팅 내역을 읽어 Gemini에 전달
- 지역 키워드 + 음식 키워드를 같이 추출하도록 프롬프트 구성

응답
- `AiDto` (keywords + chatRoomId)

## POST /api/ai/plan
설명: 키워드 기반 여행 계획 생성

요청 바디
- `AiDto` (chatRoomId, keywords)

동작(요약)
- Mongo 채팅방(ChatRoom) 조회 → startDate/endDate로 기간 계산
- 채팅방 memberIds를 Postgres User로 매핑해서 "그룹 프로필"(페이스/리듬/선호/금지 음식) 집계
- 키워드로 관광 API 검색(crawlingService.searchStoresBatch)
- Gemini에 프롬프트 전달(필터링 규칙/고평점 우선/동선 최적화/하루 최소 4개 등)
- 결과 JSON을 `AiPlanDto`로 파싱
- 생성된 플랜은 Mongo(AiGeneratedPlan)로 저장

응답
- `AiPlanDto`

## POST /api/ai/plan/confirm
설명: 여행 계획 확정(저장) (인증 필요)

요청 바디
- `AiPlanDto` 전체

동작
- UserTravelPlan(userId, chatRoomId, plan, savedAt)으로 저장

응답
- `UserTravelPlan`

## PUT /api/ai/plan/{planId}
설명: 여행 계획 수정(저장본 업데이트) (인증 필요)

규칙
- planId의 userId가 본인인지 확인
- updatedPlan.chatRoomId가 null이고 기존 값이 있으면 기존 값을 유지

## GET /api/ai/plans
설명: 내 계획 목록 조회 (인증 필요)

쿼리
- `chatRoomId` (선택)

동작
- chatRoomId가 없으면 전체
- 있으면 해당 chatRoomId만

## DELETE /api/ai/plan/{planId}
설명: 내 계획 삭제 (인증 필요)
