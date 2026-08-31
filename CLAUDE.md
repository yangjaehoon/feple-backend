## Backend (feple-backend)

- **언어/프레임워크**: Java 17, Spring Boot 3.4
- **DB**: MySQL + Spring Data JPA
- **인증**: Firebase Auth, 카카오 로그인, JWT
- **스토리지**: AWS S3
- **관리자 페이지**: Thymeleaf 기반 (`/admin`) — TypeScript 아님
- **보안**: Spring Security, Bucket4j Rate Limiting

### 자주 쓰는 명령어
```bash
./gradlew compileJava          # 컴파일 에러 확인
./gradlew compileTestJava      # 테스트 소스 컴파일 에러 확인
./gradlew test                 # 단위 테스트 실행
./gradlew bootJar -x test      # 빌드
./gradlew bootRun              # 로컬 개발 서버 실행 (application-local.yaml 필요)
```

### 환경 설정
민감 정보는 `application-local.yaml`(git 미추적)과 환경변수로 관리.
`ADMIN_USERNAME`, `ADMIN_PASSWORD`, `JWT_SECRET`, `EC2_HOST`, `EC2_SSH_KEY` 등은 GitHub Secrets에 등록.

### CI/CD
GitHub Actions 워크플로:
- `ci.yml` — PR → main 시 Spotless·컴파일·테스트·SpotBugs·JaCoCo 게이트.
- `deploy.yml` — push to main 시 EC2 자동 배포. `APPLICATION_LOCAL_YAML` Secret에
  `application-local.yaml` 전체 내용(YAML 형식)을 저장. 새 환경변수 추가 시 이 Secret에도
  반영해야 prod에서 동작.
- `claude-code-review.yml` — PR 생성·업데이트 시 Claude 자동 코드리뷰. 참고용이며 머지 게이트가
  아니다. Dependabot PR은 시크릿 미접근으로 동작하지 않는다.
- `claude.yml` — 이슈·PR에서 `@claude` 멘션 시 응답.

`CLAUDE_CODE_OAUTH_TOKEN` 시크릿은 구독 계정에서 `claude setup-token`으로 발급한 토큰이어야 한다 — 비구독 계정 토큰이면 리뷰가 조용히 즉시 실패한다.

### 주요 패턴
- CI 테스트: `./gradlew cleanTest test` — `cleanTest` 필수. runner 재사용 시 Gradle 빌드 캐시로 테스트 태스크가 UP-TO-DATE로 오판돼 stale 결과 사용됨
- 단위 테스트: `@ExtendWith(MockitoExtension.class)` + `@InjectMocks`/`@Mock`, 한글 메서드명, BDD(`given/willReturn/verify`) 패턴 사용
- DTO 테스트 모킹: `@Getter @NoArgsConstructor`만 있는 DTO(빌더/세터 없음, 예: `CreateCommentDto`) → `mock(Dto.class)` + `given(dto.getX()).willReturn(...)` 사용
- `PostRepository.findHotPosts`는 `List<Post>` 직접 반환(Page 아님) — 테스트 시 `PageImpl` 아닌 `List.of(...)` 목킹
- **인증**: OAuth 전용 — `OAuthLoginService` 인터페이스, `KakaoAuthService`·`FirebaseAuthService` 구현
- 서비스 인터페이스 분리: 주요 도메인은 `*Service`(사용자용) + `*AdminService`(관리자용) 인터페이스로 분리, 하나의 `*ServiceImpl`이 둘 다 구현
- 선택적 JWT 인증: `@AuthenticationPrincipal(required=false)` 미지원 → `Authentication authentication` 파라미터 + `(Long) authentication.getPrincipal()` null 체크
- 연관 엔티티 삭제 시 FK 순서: CommentLike → CommentReport → Comment
- `Page<T>.getContent()`로 List 변환 필요 (findBy...Ordered 메서드가 Page 반환 시)
- 크로스 도메인 삭제 위임: `*CascadeDeleteService`는 다른 도메인 Repository 직접 주입 금지 → 해당 도메인 Service 인터페이스로 위임 (예: `PostService.deletePostsByArtist()`)
- 동일 인터페이스 빈이 여러 개일 때 `@Qualifier` 불필요: 필드명을 빈 이름과 일치시키면 Spring이 이름으로 disambiguate (`OAuthLoginService kakaoAuthService` → `KakaoAuthService` 빈 자동 주입)
- Admin 컨트롤러는 Repository 직접 주입 금지 → 통계/목록은 `AdminStatsService` 경유
- 사용자에게 보여줄 검증·비즈니스 규칙 위반은 `InvalidRequestException`(=`global.exception`, `IllegalArgumentException` 하위), "찾을 수 없음"은 `ResourceNotFoundException`(=`global.exception`, `NoSuchElementException` 하위, `EntityLoader.getOrThrow`가 던짐)을 던진다. 전역 핸들러는 이 두 타입의 메시지만 응답에 노출하고, 순수 `IllegalArgumentException`/`NoSuchElementException`(JDK·라이브러리 발)은 일반 메시지로 대체한다 → `new IllegalArgumentException("한국어 메시지")` / `new NoSuchElementException("한국어 메시지")` 금지
- Admin 컨트롤러 예외 처리: `AdminActionUtils.tryAction/tryRender`는 `InvalidRequestException | BadWordException | ResourceNotFoundException`의 메시지만 사용자에게 노출, 그 외 `Exception`은 `onError`(log.error) + failMsg 일반 메시지. 컨트롤러에서 직접 `catch` 시에도 이 원칙을 따를 것 (내부 예외 절대 노출 금지)
- Admin 컨트롤러의 파일 업로드도 서비스 경유: `FestivalService.uploadPosterFile()`, `ArtistService.uploadProfile()`, `BoothService.uploadBoothImage()` — FileStorageService 직접 주입 금지
- S3 presigned URL 결과 타입: `file/dto/PresignResult` (독립 레코드, S3PresignService 중첩 타입 아님)
- 신고 타입 확장: `ReportAdminController`의 `list()` GET은 `Map<String, ReportAdminService>` 디스패치 — 신규 신고 유형 추가 시 ① `ReportAdminService` 구현체 추가, ② 컨트롤러에 명시적 필드 + POST 액션 엔드포인트 추가 필요
- 도메인 간 이벤트: 댓글 생성 시 알림은 `ApplicationEventPublisher` + `CommentCreatedEvent` 레코드 사용 (comment → notification 직접 의존 없음)
- LoD 준수: 엔티티 연관 ID 접근 시 `getUser().getId()` 체인 금지 → 엔티티에 `getUserId()` 등 헬퍼 직접 추가 (Post, Comment, ArtistFollow, ArtistFestival, Notification, PostReport, CommentReport, FestivalCertification, SongRequest, ArtistFestivalSong에 이미 적용됨)
- 카운터 증감(좋아요/스크랩/팔로워/참석 등)은 리포지토리의 원자적 `@Modifying UPDATE` 쿼리로 직접 수행 (예: `postRepository.incrementLikeCount(id)`) — 동시성 하 lost-update 방지. 엔티티에 `incrementXxx()`/`decrementXxx()` 인스턴스 메서드를 추가하지 말 것: JPA dirty checking으로 flush되면서 같은 행에 대한 동시 원자적 UPDATE 결과를 덮어쓸 수 있음 (Post/Comment/Festival/Artist/ArtistGalleryPhoto/FestivalCertification에서 이런 죽은 엔티티 메서드를 전부 제거한 이력 있음)
- TDA + CQS 조합: 엔티티 update 메서드는 `void` 반환 — 상태 변경과 값 반환을 동시에 하지 않음. 서비스에서 old value가 필요하면 `String old = entity.getX(); entity.updateX(newVal);` 순서로 분리 (예: `Artist.updateProfileImage`, `Festival.updatePoster`)
- Admin 서브컨트롤러에서 동일 redirect 문자열 반복 시 `private String *Redirect(Long festivalId)` 헬퍼로 추출 (예: `boothsRedirect`, `artistsRedirect`, `timetableRedirect`)
- Thymeleaf 알림 메시지: `class="alert alert-success"` / `class="alert alert-danger"` 사용 — inline `style=` 속성으로 직접 색상 지정 금지 (admin.css에 정의됨)
- 예외 타입 HTTP 매핑: `ResourceNotFoundException`/`NoSuchElementException`→404(전자는 메시지 노출, 후자는 일반 메시지), `InvalidRequestException`/`IllegalArgumentException`→400(전자는 메시지 노출, 후자는 일반 메시지), `ConflictException`→409(중복 리소스), `WebClientException`(외부 API 5xx·타임아웃)→502, `ExternalStorageException`(S3 등 외부 스토리지 장애)→502, `IOException`(파일 디코딩·읽기 실패)→500, `IllegalStateException`→500(예상 불가 서버 오류 전용) — 신규 409 케이스는 반드시 `ConflictException` 사용. S3 업로드 실패는 `FileStorageService`가 SDK unchecked 예외를 `ExternalStorageException`으로 변환한다(직접 `IllegalStateException` 금지)
- Hibernate 6 derived query 주의: `@ManyToOne` 필드만 있을 때 `findByUserIdAndFestivalId()`, `existsByFestivalIdAndArtistId()`, `deleteByUserIdAndToken()` 등 파생 쿼리는 startup 시 `PathElementException` 발생 → `existsBy`는 `@Query("SELECT CASE WHEN COUNT(...)>0 THEN TRUE ELSE FALSE END FROM ... WHERE e.user.id=:userId")`, `findBy`/`deleteBy`도 `@Query` 명시 필요. Repository 메서드 작성 시 `@ManyToOne` 연관 ID 접근은 항상 `@Query`로 명시할 것
- `Post`/`Comment`는 `Festival`/`Artist`와 동일하게 상시 `@SQLRestriction` 없이 소프트 삭제(`deleted_at`)와 블라인드(`blinded`)를 쓴다. 공개 조회 쿼리(피드·상세·인기글·태그·검색·통계)는 `PostRepository`의 `VISIBLE` 상수처럼 `deleted_at IS NULL AND blinded = false`를 쿼리마다 명시할 것 — 파생명 메서드로는 필터를 못 넣으므로 `@Query`로 작성한다. 관리자·신고 처리·본인 글/댓글 수정·삭제·캐스케이드 경로는 평범한 `findById`/`findAllById`/`delete`를 그대로 쓴다(`@SQLDelete`가 걸려 있어 `delete`는 소프트 삭제로 동작). 삭제·블라인드된 행을 찾는 전용 메서드는 `findSoftDeleted`/`findBlinded`/`restore`, 벌크 소프트 삭제는 `softDeleteByIds`.
- `PostReport`/`CommentReport`가 참조하는 `post`/`comment` 연관관계는 대상이 하드 삭제되면 `null`로 채워질 수 있다(EntityGraph LEFT JOIN이라 신고 행 자체는 남음) — 이 연관관계에서 파생되는 게터는 항상 null-safe하게 작성할 것 (예: `PostReport.getPostTitle()`처럼 null이면 대체 문자열/`null` 반환)
