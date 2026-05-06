# FEPLE Backend

페스티벌을 좋아하는 사람들의 커뮤니티 앱 **FEPLE**의 Spring Boot 백엔드 서버입니다.

## 주요 기능

- **인증** - Firebase Auth(이메일), 카카오 로그인, JWT 토큰 발급/갱신
- **페스티벌** - CRUD, 장르/지역 필터링, 좋아요, 타임테이블, 부스 관리
- **아티스트** - CRUD, 팔로우, 사진 갤러리(S3 업로드), 주간 랭킹
- **커뮤니티** - 게시글/댓글 CRUD, 좋아요
- **관리자** - Thymeleaf 기반 웹 관리 페이지

## 기술 스택

- **Java 17** / **Spring Boot 3.4**
- **Spring Data JPA** + MySQL
- **Spring Security** + JWT (jjwt)
- **Firebase Admin SDK** - 인증 연동
- **AWS S3** - 이미지 저장 (Spring Cloud AWS + Presigned URL)
- **Bucket4j + Caffeine** - Rate Limiting
- **Thymeleaf** - 관리자 페이지
- **Lombok** - 보일러플레이트 제거

## 프로젝트 구조

```
src/main/java/com/feple/feple_backend/
├── admin/          # 관리자 컨트롤러
├── artist/         # 아티스트 (프로필, 사진, 랭킹)
├── artistfestival/ # 아티스트-페스티벌 연결 (라인업)
├── artistfollow/   # 아티스트 팔로우
├── auth/           # 인증 (Firebase, Kakao, JWT)
├── booth/          # 페스티벌 부스
├── comment/        # 댓글
├── config/         # 설정 (Security, S3, CORS 등)
├── festival/       # 페스티벌
├── file/           # 파일 저장 서비스
├── global/         # 전역 예외 처리
├── post/           # 게시글
├── stage/          # 공연 스테이지
├── timetable/      # 타임테이블
└── user/           # 사용자
```

## 설정

### 환경 설정

`src/main/resources/application-local.yaml`에 아래 항목을 설정합니다 (git 미추적):

- MySQL 접속 정보
- JWT 시크릿 키
- AWS S3 버킷/리전/인증 정보
- Firebase 서비스 계정 키 경로
- Google Maps API 키

### 빌드 및 실행

```bash
./gradlew bootJar -x test
java -jar build/libs/feple_backend-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

## 프론트엔드

프론트엔드 레포지토리: [feple-frontend](https://github.com/yangjaehoon/feple-frontend)
