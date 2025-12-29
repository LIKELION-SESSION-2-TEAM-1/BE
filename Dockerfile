# ==========================================
# 1단계: 빌드 환경 (Builder)
# ==========================================
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# 1. Gradle 캐싱 효율화를 위해 설정 파일만 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 2. 실행 권한 부여
RUN chmod +x gradlew

# 3. 의존성만 미리 다운로드 (소스코드 변경 시에도 이 단계는 캐시됨)
# --no-daemon으로 메모리 절약
RUN ./gradlew dependencies --no-daemon

# 4. 소스코드 복사 및 빌드
COPY src src
RUN ./gradlew clean build -x test --no-daemon

# ==========================================
# 2단계: 실행 환경 (Runner)
# ==========================================
FROM eclipse-temurin:21-jdk
WORKDIR /app

# 1. Python3, Pip, Chromium 설치 (크롤링 환경)
# Selenium 실행을 위해 필요한 패키지들입니다.
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    chromium \
    chromium-driver \
    fonts-ipafont-gothic \
    fonts-wqy-zenhei \
    fonts-thai-tlwg \
    fonts-kacst \
    fonts-freefont-ttf \
    libxss1 \
    && rm -rf /var/lib/apt/lists/*

# 2. Python 라이브러리 설치
# (필요하다면 requirements.txt를 만들어 복사하는 것이 더 관리에 좋습니다)
RUN pip3 install selenium webdriver-manager --break-system-packages

# 3. 빌드 단계(builder)에서 생성된 JAR 파일만 복사
# 파일 이름을 app.jar로 통일하여 버전 변경 문제 해결
COPY --from=builder /app/build/libs/*.jar app.jar

# 4. 환경 변수 설정
ENV PYTHON_EXECUTABLE=python3
# 크롬 실행 시 공유 메모리 문제 방지용 포트 등 설정이 필요할 수 있음

# 5. 포트 노출 및 실행
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
