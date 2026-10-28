package com.example.quizarenabackend.quiz;

import com.example.quizarenabackend.quiz.entities.GameResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameResultRepository extends JpaRepository<GameResult, Long> {
}