# FEPLE Backend

페스티벌을 좋아하는 사람들의 커뮤니티 앱 **FEPLE**의 Spring Boot 백엔드 서버입니다.

## 주요 기능

- **인증** — Firebase Auth(이메일/소셜), 카카오 로그인, JWT 액세스/리프레시 토큰
- **페스티벌** — CRUD, 장르·지역 필터링, 좋아요, 타임테이블, 부스 맵, 공기질(AQI) 정보
- **아티스트** — CRUD, 팔로우, 사진 갤러리(S3), 노래 목록, 공연 일정, 주간 팔로워 랭킹
- **커뮤니티** — Hot/자유/동행 게시판, 아티스트·페스티벌 전용 게시판, 스크랩, 무한 스크롤 페이지네이션
- **댓글** — 대댓글(스레드), 좋아요, 신고
- **검색** — 아티스트·페스티벌·게시글 통합 검색
- **페스티벌 인증** — 사진 제출 → 관리자 승인 → 배지 지급
- **노래 요청** — 사용자 곡 추가 요청 → 관리자 처리
- **알림** — FCM 푸시 알림, 인앱 알림 목록, 전체 읽음
- **신고** — 게시글·댓글·아티스트 사진 신고 → 관리자 검토
- **관리자** — Thymeleaf 기반 웹 어드민(`/admin`): 페스티벌·아티스트·인증·신고 관리
- **보안** — Rate Limiting(Bucket4j + Caffeine), Spring Security

## 기술 스택

| 분류 | 기술 |
|------|------|
| 언어 / 프레임워크 | Java 17, Spring Boot 3.4 |
| 데이터베이스 | MySQL, Spring Data JPA (Hibernate 6) |
| 인증 | Firebase Admin SDK, Kakao OAuth, JWT (jjwt) |
| 스토리지 | AWS S3 (Spring Cloud AWS, Presigned URL) |
| 알림 | Firebase Cloud Messaging (FCM) |
| 캐시 / Rate Limit | Caffeine, Bucket4j |
| 관리자 UI | Thymeleaf |
| 유틸 | Lombok |

## 프로젝트 구조

```
src/main/java/com/feple/feple_backend/
├── admin/            # 관리자 컨트롤러 및 통계
├── artist/           # 아티스트 (프로필, 사진 갤러리, 랭킹)
├── artistfestival/   # 아티스트-페스티벌 라인업
├── artistfollow/     # 아티스트 팔로우
├── auth/             # 인증 (Firebase, Kakao, JWT)
├── booth/            # 페스티벌 부스 맵
├── certification/    # 페스티벌 인증 (제출 / 관리자 승인)
├── comment/          # 댓글 (대댓글, 좋아요, 신고)
├── config/           # 설정 (Security, S3, CORS, Rate Limit 등)
├── festival/         # 페스티벌 (CRUD, 필터, AQI)
├── file/             # S3 파일 업로드 서비스
├── global/           # 전역 예외 처리, 공통 유틸
├── notification/     # FCM 알림, 인앱 알림
├── post/             # 게시글 (게시판, 스크랩, 신고)
├── search/           # 통합 검색
├── stage/            # 공연 스테이지
├── timetable/        # 타임테이블 엔트리
└── user/             # 사용자 프로필
```

## 환경 설정

`src/main/resources/application-local.yaml`을 생성하고 아래 항목을 설정합니다 (git 미추적):

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/feple
    username: ...
    password: ...

jwt:
  secret: ...

aws:
  s3:
    bucket: ...
    region: ...
  credentials:
    access-key: ...
    secret-key: ...

firebase:
  service-account-key: /path/to/serviceAccountKey.json

kakao:
  rest-api-key: ...
```

## 빌드 및 실행

```bash
# 컴파일 에러 확인
./gradlew compileJava

# 빌드 (테스트 제외)
./gradlew bootJar -x test

# 로컬 실행
./gradlew bootRun

# JAR 직접 실행
java -jar build/libs/feple_backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## CI/CD

GitHub Actions를 통해 `main` 브랜치 푸시 시 EC2에 자동 배포됩니다.
민감 정보(`ADMIN_USERNAME`, `ADMIN_PASSWORD`, `JWT_SECRET`, `EC2_HOST`, `EC2_SSH_KEY` 등)는 GitHub Secrets에 등록합니다.

## 프론트엔드

프론트엔드 레포지토리: [feple-frontend](https://github.com/yangjaehoon/feple-frontend)
