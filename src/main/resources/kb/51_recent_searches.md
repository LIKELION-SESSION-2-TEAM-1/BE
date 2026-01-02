# 최근 검색(Recent Searches)

## Base URL
- `/api/searches/recent`

## GET /api/searches/recent
설명: 내 최근 검색 목록 (인증 필요)

응답
- RecentSearchResponse[] (createdAt 내림차순)

## POST /api/searches/recent
설명: 최근 검색 추가 (인증 필요)

요청 바디
```json
{ "keyword": "해운대 맛집" }
```

동작
- 사용자의 `recentSearchEnabled`가 false면 저장하지 않고 204 반환
- 동일 keyword가 이미 있으면 삭제 후 다시 저장(최신화)
- 최대 10개 유지(초과분은 오래된 것부터 삭제)

응답
- 204 No Content

## DELETE /api/searches/recent/{recentSearchId}
설명: 최근 검색 1개 삭제 (인증 필요)

응답
- 204 No Content

## DELETE /api/searches/recent
설명: 최근 검색 전체 삭제 (인증 필요)

응답
- 204 No Content
