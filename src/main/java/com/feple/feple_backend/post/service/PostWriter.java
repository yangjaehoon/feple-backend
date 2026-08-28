package com.feple.feple_backend.post.service;

import com.feple.feple_backend.global.EntityLoader;
import com.feple.feple_backend.post.dto.PostRequestDto;
import com.feple.feple_backend.post.entity.Post;
import com.feple.feple_backend.post.entity.PostImage;
import com.feple.feple_backend.post.entity.PostTag;
import com.feple.feple_backend.post.event.PostCreatedEvent;
import com.feple.feple_backend.post.repository.PostDraftRepository;
import com.feple.feple_backend.post.repository.PostImageRepository;
import com.feple.feple_backend.post.repository.PostRepository;
import com.feple.feple_backend.post.repository.PostTagRepository;
import com.feple.feple_backend.user.entity.User;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 쓰기의 DB 영속 로직만 담는 트랜잭션 경계.
 *
 * <p>PostServiceImpl의 create/update 진입점은 트랜잭션을 열지 않은 상태에서 입력 검증
 * (금칙어 검사, S3 오브젝트 존재 확인 등 외부 I/O 포함)을 끝낸 뒤 이 빈에 위임한다. 검증을
 * 같은 클래스의 private 메서드로 두면 self-invocation이라 트랜잭션 경계를 분리할 수 없어
 * (FestivalWeatherStore와 동일한 이유) 별도 컴포넌트로 뺐다. 덕분에 S3 HeadObject 호출이
 * DB 커넥션을 점유하지 않는다.
 */
@Component
@RequiredArgsConstructor
class PostWriter {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostTagRepository postTagRepository;
    private final PostDraftRepository postDraftRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 호출부(PostServiceImpl)가 내용·이미지 검증을 마쳤다는 전제로 저장만 수행한다. */
    @Transactional
    Long save(PostRequestDto dto, User user, PostContext ctx) {
        Post saved = postRepository.save(buildPost(dto, user, ctx));
        saveImages(saved, dto.getImageUrls());
        saveTags(saved, dto.getTags());
        // 게시글이 실제로 등록됐으니 남아있던 임시저장은 정리한다 (없어도 no-op).
        postDraftRepository.deleteByUserId(user.getId());
        // 포인트 지급 등 AFTER_COMMIT 리스너가 동작하려면 활성 트랜잭션 안에서 발행해야 한다.
        eventPublisher.publishEvent(new PostCreatedEvent(user.getId(), saved.getId()));
        return saved.getId();
    }

    /** 호출부가 소유자·내용·이미지 검증을 마쳤다는 전제로 수정만 수행한다. */
    @Transactional
    void update(Long postId, PostRequestDto dto) {
        Post post = EntityLoader.getOrThrow(postRepository::findById, postId, "게시글");
        post.update(dto.getTitle(), dto.getContent());
        postImageRepository.deleteByPostId(postId);
        saveImages(post, dto.getImageUrls());
        postTagRepository.deleteByPostId(postId);
        saveTags(post, dto.getTags());
    }

    private Post buildPost(PostRequestDto dto, User user, PostContext ctx) {
        return Post.builder()
                .title(dto.getTitle())
                .content(dto.getContent())
                .boardType(ctx.boardType())
                .likeCount(0)
                .user(user)
                .artist(ctx.artist())
                .festival(ctx.festival())
                .anonymous(dto.isAnonymous())
                .build();
    }

    private void saveImages(Post post, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return;
        List<PostImage> images = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(PostImage.builder().post(post).imageKey(imageUrls.get(i)).sortOrder(i).build());
        }
        postImageRepository.saveAll(images);
    }

    // 대소문자·앞뒤 공백만 다른 태그가 중복 저장되지 않도록 정규화 후 중복을 제거한다.
    private void saveTags(Post post, List<String> tags) {
        if (tags == null || tags.isEmpty()) return;
        List<PostTag> postTags = tags.stream()
                .map(PostTags::normalize)
                .filter(tag -> !tag.isBlank())
                .distinct()
                .map(tag -> PostTag.builder().post(post).tag(tag).build())
                .toList();
        postTagRepository.saveAll(postTags);
    }
}
