package com.example.quizarenabackend.quiz;

import com.example.quizarenabackend.quiz.entities.Quiz;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    boolean existsByTitle(String title);
    @EntityGraph(attributePaths = {"questions"})
    Optional<Quiz> findQuizWithQuestionsById(Long id);
}
