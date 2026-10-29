package com.example.quizarenabackend.quiz;

import com.example.quizarenabackend.game.dtos.CurrentQuestionDto;
import com.example.quizarenabackend.game.dtos.GameQuestionOptionDto;
import com.example.quizarenabackend.quiz.dtos.*;
import com.example.quizarenabackend.quiz.entities.AnswerOption;
import com.example.quizarenabackend.quiz.entities.Question;
import com.example.quizarenabackend.quiz.entities.Quiz;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface QuizMapper {
    AnswerOption toAnswerOptionEntity(CreateQuestionAnswerDto createQuestionAnswerDto);
    @Mapping(target="answerOptions", source="options")
    Question toQuestionEntity(CreateQuestionDto createQuestionDto);
    Quiz toQuizEntity(CreateQuizDto createQuizDto);

    AnswerOptionDto toAnswerOptionDto(AnswerOption answerOption);
    @Mapping(target="options", source="answerOptions")
    QuestionDto toQuestionDto(Question question);
    QuizDto toQuizDto(Quiz quiz);
    QuizWithQuestionsDto toQuizWithQuestionsDto(Quiz quiz);
    GameQuestionOptionDto toGameQuestionOptionDto(AnswerOption answerOption);
    @Mapping(target = "index", source="questionQuizIndex")
    @Mapping(target="questionId", source="question.id")
    @Mapping(target="options", source="question.answerOptions")
    CurrentQuestionDto toCurrentQuestionDto(Question question, int questionQuizIndex, int totalQuestions, long endsAt, long serverNow);

    void updateQuizFromDto(CreateQuizDto createQuizDto, @MappingTarget Quiz quiz);

    @AfterMapping
    default void linkQuestionsAndSetPoisition(@MappingTarget Quiz quiz) {
        if (quiz.getQuestions() != null) {
            for (int i=0; i<quiz.getQuestions().size(); i++) {
                Question question = quiz.getQuestions().get(i);
                // Setting the question positions according to their order in the list. Simple solution but there is no
                // need for a more complex one.
                question.setPosition(i);
                question.setQuiz(quiz);
            }
        }
    }

    @AfterMapping
    default void linkAnswersAndSetPosition(@MappingTarget Question question) {
        if (question.getAnswerOptions() != null) {
            for (int i=0; i<question.getAnswerOptions().size(); i++) {
                AnswerOption answerOption = question.getAnswerOptions().get(i);
                // The same idea for answer options as for questions. The order in the list is the order in which they will be displayed.
                answerOption.setPosition(i);
                answerOption.setQuestion(question);
            }
        }
    }
}
