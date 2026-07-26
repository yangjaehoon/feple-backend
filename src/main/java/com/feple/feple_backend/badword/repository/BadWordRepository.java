package com.feple.feple_backend.badword.repository;

import com.feple.feple_backend.badword.entity.BadWord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BadWordRepository extends JpaRepository<BadWord, Long> {

    boolean existsByWord(String word);

    @Query("SELECT b.word FROM BadWord b")
    List<String> findAllWords();
}
