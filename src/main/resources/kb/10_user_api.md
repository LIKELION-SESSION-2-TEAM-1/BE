# 사용자(User) / 인증

## 인증 방식 요약
- JWT 헤더: `Authorization: Bearer <token>`
- 서버는 Spring Security + JWT 필터를 사용합니다.

## Base URL
- `/api/user`

## POST /api/user/signup
설명: 자체 회원가입

요청 바디
```json
{
  "username": "아이디(또는 이메일)",
  "password": "비밀번호"
}
```

동작
- username 중복이면 실패
- 가입 시 기본 nickname은 username과 동일하게 설정

응답
- 201 Created + 문자열 메시지

## POST /api/user/login
설명: 자체 로그인(JWT 발급)

요청 바디
```json
{
  "username": "아이디(또는 이메일)",
  "password": "비밀번호"
}
```

응답
- 200 OK
- Header: `Authorization: <token>`
- Body:
```json
{
  "message": "로그인 성공",
  "token": "<token>"
}

## 이메일 인증(자체 회원가입 유저)

### POST /api/user/email/verification/send
설명: 로그인한 유저에게 이메일 인증 메일을 발송합니다.

요청 헤더
- `Authorization: Bearer <token>`

응답
- 204 No Content: 발송 요청 성공
- 400 Bad Request: 이미 인증됨 / username이 이메일이 아님 등

### POST /api/user/email/verification/confirm
설명: 메일로 받은 token으로 이메일 인증을 완료합니다.

요청 바디
```json
{ "token": "..." }
```

응답
- 200 OK
```json
{ "message": "이메일 인증 완료" }
```
- 400 Bad Request: 유효하지 않음/만료/이미 사용됨

### GET /api/user/email/verification/confirm?token=...
설명: 메일 링크용(쿼리 token으로 인증 완료)

## POST /api/user/logout
설명: 로그아웃(JWT 무효화)

요청 헤더
- `Authorization: Bearer <token>`

동작
- 전달된 access token을 서버 블랙리스트에 등록하여, 만료 시각까지 해당 토큰을 더 이상 인증에 사용하지 못하게 합니다.

응답
- 200 OK
```json
{ "message": "로그아웃 완료" }
```
```

## GET /api/user/profile
설명: 내 프로필 조회 (인증 필요)

응답
- 200 OK + `UserProfileResponse`

## PUT /api/user/profile
설명: 내 프로필 수정(부분 수정) (인증 필요)

요청 바디(예시)
```json
{
  "nickname": "멋쟁이",
  "birthDate": "1999-01-01",
  "travelPace": "느림",
  "dailyRhythm": "아침형",
  "foodPreferences": ["고기", "국밥"],
  "foodRestrictions": ["해산물"],
  "recentSearchEnabled": true
}
```

동작
- 보내지 않은 필드(null)는 기존 값 유지
- birthDate는 `YYYY-MM-DD` 형식만 허용(아니면 400)

응답
- 200 OK + 문자열 메시지

## POST /api/user/profile/image
설명: 프로필 이미지 업로드 (인증 필요)

요청
- `multipart/form-data`
- 파트: `file` (jpg/png/webp)

응답
- public bucket이면:
```json
{ "profileImageUrl": "https://..." }
```
- private bucket이면 400 + 안내 메시지(현재는 signed url 미구현)

## DELETE /api/user
설명: 회원 탈퇴 (인증 필요)

응답
- 200 OK + 문자열 메시지

## OAuth2 로그인(설명용)
- `GET /api/user/google/login` → `/oauth2/authorization/google`로 리다이렉트
- `GET /api/user/kakao/login` → `/oauth2/authorization/kakao`로 리다이렉트
- `GET /api/user/naver/login` → `/oauth2/authorization/naver`로 리다이렉트
