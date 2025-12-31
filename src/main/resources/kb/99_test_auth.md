# (주의) 테스트용 토큰 발급 API

배포 환경에서는 노출되면 위험할 수 있으니 주의하세요.

## Base URL
- `/api/test/auth`

## GET /api/test/auth/token?email=...
설명
- 이메일만으로 즉시 JWT 토큰을 발급합니다.
- 해당 email의 사용자가 없으면 자동으로 생성합니다.

응답
```json
{
  "accessToken": "<token>",
  "email": "user@example.com",
  "nickname": "TestUser_user"
}
```
