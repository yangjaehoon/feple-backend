package com.feple.feple_backend.nickname.repository;

import com.feple.feple_backend.nickname.entity.NicknameRestriction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NicknameRestrictionRepository extends JpaRepository<NicknameRestriction, Long> {

    boolean existsByWord(String word);

    @Query("SELECT n.word FROM NicknameRestriction n")
    List<String> findAllWords();
}
