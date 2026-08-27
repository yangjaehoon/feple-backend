package com.feple.feple_backend.notification.repository;

import com.feple.feple_backend.notification.entity.PendingPush;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PendingPushRepository extends JpaRepository<PendingPush, Long> {

    // 발송 처리(flushPendingPushes)는 트랜잭션 없이 항목별로 FCM을 호출하므로, 지연 로딩되는
    // userIds(@ElementCollection)를 이 조회 시점에 함께 가져와야 세션 밖에서도 접근할 수 있다.
    @Query("SELECT DISTINCT p FROM PendingPush p LEFT JOIN FETCH p.userIds")
    List<PendingPush> findAllWithRecipients();
}
