# 가게/여행지 검색(크롤링/관광 API)

## Base URL
- `/api/stores`

## GET /api/stores/search
설명: 키워드 기반 가게/장소 검색

쿼리
- `keyword` (예: "해운대 맛집")

동작
- 공공 관광 API(KorService2/searchKeyword2)를 호출해서 JSON 결과를 가공
- 결과가 많으면 maxItems 기준으로 상위 N개 반환
- link가 없으면 visitkorea 검색 링크를 생성해서 넣음

응답
- `StoreDto[]`

필수 설정
- `tour.api.service.key` 또는 환경변수 `TOUR_API_SERVICE_KEY`
- 키가 없으면 500(IllegalStateException: Tour API key not configured)
