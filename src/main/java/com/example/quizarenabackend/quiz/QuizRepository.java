package com.example.quizarenabackend.quiz;

import com.example.quizarenabackend.quiz.entities.Quiz;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long>, JpaSpecificationExecutor<Quiz> {
    boolean existsByTitle(String title);
    @EntityGraph(attributePaths = {"questions"})
    Optional<Quiz> findQuizWithQuestionsById(Long id);
    // Custom query to find trending quizzes based on the number of game results throughout the given time period
    @Query("select q from Quiz q join q.gameResults gr " +
            "where gr.playedAt >= :since " +
            "group by q.id " +
            "order by count(gr.roomSessionId) desc" )
    List<Quiz> findTrendingQuizzes(@Param("since")Instant since, Pageable pageable);
    List<Quiz> findByOrderByRunCountDesc(Pageable pageable);
}
