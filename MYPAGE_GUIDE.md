# 👤 마이페이지(내 프로필) 연동 가이드

프론트엔드에서 **마이페이지**를 구현할 때 필요한 API 명세입니다.

## 1. 내 프로필 조회
화면에 진입했을 때 사용자 정보를 불러옵니다.

- **URL**: `GET /api/user/profile`
- **Header**: `Authorization: Bearer {Token}`
- **응답 예시 (JSON)**:
  ```json
  {
    "username": "user1234",          // 아이디 (수정 불가)
    "nickname": "여행왕",             // 닉네임 (기본값 = 아이디)
    "birthDate": "1999-01-01",       // YYYY-MM-DD
    "profileImageUrl": "https://...",
    "travelPace": "느림",             // 느림 / 보통 / 빠름
    "dailyRhythm": "아침형",          // 아침형 / 유연 / 야행성
    "foodPreferences": ["한식", "일식"], // 선호 음식 (배열)
    "foodRestrictions": ["오이"]      // 못 먹는 음식 (배열)
  }
  ```

---

## 2. 내 프로필 수정
사용자가 정보를 수정하고 '저장' 버튼을 눌렀을 때 호출합니다.
**⚠️ 변경된 값만 보내도 됩니다 (부분 수정 지원).**

- **URL**: `PUT /api/user/profile`
- **Header**: `Authorization: Bearer {Token}`
- **Body 예시 (JSON)**:
  ```json
  {
    "nickname": "새로운닉네임",
    "travelPace": "빠름",
    "foodPreferences": ["고기"]
  }
  ```
  *(위와 같이 보내면 `birthDate` 등 다른 정보는 변하지 않고 유지됩니다.)*

---

## 💡 프론트엔드 체크리스트
1. **닉네임 초기값**: 회원가입 직후에는 `nickname`이 `username`(아이디)과 동일하게 되어 있습니다.
2. **배열 필드**: `foodPreferences`, `foodRestrictions`는 문자열(`"a,b"`)이 아니라 **JSON 배열(`["a", "b"]`)** 형태로 주고받습니다.
