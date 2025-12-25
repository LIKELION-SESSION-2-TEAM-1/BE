# Formats for Commit Messages
먼저 커밋 메시지는 크게 제목, 본문, 꼬리말 세 가지로 나뉘고, 각 파트는 공백 줄로 구분합니다.

## type(타입) : title(제목) //Subject

## Body(본문, 생략 가능)

## Footer(생략 가능)
🔖 Message Tag
타입은 태그와 제목으로 구성되고, 태그는 영어로 작성, 첫 문자는 대문자로 합니다.
"태그: 제목"의 형태이며, : 뒤에 공백을 넣어주세요.

## Commit Message Subject 규칙
첫 글자는 대문자로 입력하고, 제목 줄을 마침표로 끝내지 않는다.
마지막에는 .(period)을 찍지 않으며 영문 기준 최대 50자를 넘지 않는다.
제목은 동사원형을 사용해 명령문의 형태로 작성한다.
본문과 주제를 공백 라인으로 구분한다.
### Example of Subject
Fix: 축제 관리자만 부스 목록에서 모든 데이터를 확인하도록 수정

## Commit Message Body 규칙
선택 사항이므로 모든 커밋에 작성할 필요는 없다.
각 줄은 최대 72자를 넘지 않도록 한다.
어떻게 변경했는지보다, 무엇을 변경했고, 왜 변경했는지를 자세히 설명한다.
설명뿐만 아니라 커밋의 이유를 작성할 때도 작성합니다.

### Example of Body
축제 관리자만 부스 목록에서 모든 데이터를 확인하도록 수정
  - BoothMapView.vue: 관리자 유형에 따른 부스 페이지에 대한 권한을 부여함.

## Commit Message Footer 규칙
선택사항이며, 관련된 이슈를 언급한다. 예) Fixes: #1, #2
주로 Closes(종료), Fixes(수정), Resolves(해결), Ref(참고), Related to(관련) 키워드를 사용한다.
이슈를 추적하기 위한 ID를 추가할 때 사용합니다.
- 해결 : 해결한 이슈 ID
- 관련 : 해당 커밋에 관련된 이슈 ID
- 참고 : 참고할만한 이슈 ID

### Example of Footer
해결: #123
관련: #321
참고: #222

## Example of Full Commmit Message
Fix: 축제 관리자만 부스 목록에서 모든 데이터를 확인하도록 수정(#123)
축제 관리자만 부스 목록에서 모든 데이터를 확인하도록 수정
  - BoothMapView.vue: 관리자 유형에 따른 부스 페이지에 대한 권한을 부여함. 

해결: #123

---
## 🚀 로컬 환경 실행 가이드 (Local Development)

이 프로젝트는 로컬 개발 시 **H2 데이터베이스**를 사용하여 외부 DB 의존성 없이 실행할 수 있습니다.

### 1. 로컬 프로필로 실행하기
아래 명령어를 터미널에 입력하여 서버를 실행하세요.

```bash
# Windows (PowerShell)
./gradlew bootRun --args='--spring.profiles.active=local'

# Mac / Linux
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 2. 주요 접속 정보
- **서버 주소**: `http://localhost:8080`
- **Swagger API 문서**: `http://localhost:8080/swagger-ui/index.html` (API 테스트 가능)
- **H2 콘솔**: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:testdb`
  - User Name: `sa`
  - Password: (비워둠)
