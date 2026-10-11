package com.example.quizarenabackend.quiz;

import com.example.quizarenabackend.quiz.entities.AnswerOption;
import com.example.quizarenabackend.quiz.entities.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    @Query("SELECT ao FROM AnswerOption ao WHERE ao.question.id IN :questionIds")
    List<AnswerOption> findAnswerOptionsByQuestionIds(List<Long> questionIds);
}
