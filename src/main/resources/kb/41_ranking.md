# 인기 여행지 랭킹(관광데이터랩)

## Base URL
- `/api/ranking`

## GET /api/ranking/weekly
설명: 주간 인기 여행지 Top 10

동작
- 기본: 최근 데이터(2일 전)를 먼저 시도
- 없으면: 1주 전 시도
- 그래도 없으면: 데모용 고정 날짜(20240115)로 fallback

집계
- 2개 API 데이터를 합쳐서 방문객 수를 합산
  - 기초지자체(Locgo)
  - 광역지자체(Metco)
- 현지인 제외(`touDivCd == "1"` 제외)

응답
- `RankingItemDto[]` (rank, region, visitorCount)

필수 설정
- `tour.datalab.service.key`
