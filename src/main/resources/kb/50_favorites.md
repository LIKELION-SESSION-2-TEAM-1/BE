# 즐겨찾기(Favorites)

## Base URL
- `/api/favorites`

## POST /api/favorites
설명: 즐겨찾기 추가 (인증 필요)

요청 바디(예시)
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

규칙
- storeName 필수
- (userId, storeName, address) 조합이 이미 있으면 400("이미 즐겨찾기에 추가된 여행지입니다.")

응답
- 201 Created + FavoritePlaceResponse

## GET /api/favorites
설명: 내 즐겨찾기 목록 (인증 필요)

응답
- FavoritePlaceResponse[] (id 내림차순)

## DELETE /api/favorites/{favoriteId}
설명: 즐겨찾기 삭제 (인증 필요)

규칙
- 본인 소유만 삭제 가능

응답
- 204 No Content
