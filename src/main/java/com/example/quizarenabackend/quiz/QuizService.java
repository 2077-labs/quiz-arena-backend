package com.example.quizarenabackend.quiz;

import com.example.quizarenabackend.quiz.dtos.CreateQuizDto;
import com.example.quizarenabackend.quiz.dtos.QuizWithQuestionsDto;
import com.example.quizarenabackend.quiz.entities.AnswerOption;
import com.example.quizarenabackend.quiz.entities.Question;
import com.example.quizarenabackend.quiz.entities.Quiz;
import com.example.quizarenabackend.quiz.exceptons.QuizNotFoundException;
import com.example.quizarenabackend.quiz.exceptons.SameTitleQuizExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {
    private final QuizMapper quizMapper;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;

    @Transactional
    public Quiz createQuiz(CreateQuizDto dto) {
        Quiz quiz = quizMapper.toQuizEntity(dto);
        boolean isThereQuizWithTheSameName = isThereQuizWithTheSameName(quiz.getTitle());
        if (isThereQuizWithTheSameName) {
            throw new SameTitleQuizExistsException(quiz.getTitle());
        }
        quiz = quizRepository.save(quiz);
        return quiz;
    }

    @Transactional
    public void updateQuiz(Long quizId, CreateQuizDto dto) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new QuizNotFoundException(quizId, true));

        if (isThereQuizWithTheSameName(dto.getTitle())) {
            throw new SameTitleQuizExistsException(dto.getTitle());
        }

        quizMapper.updateQuizFromDto(dto, quiz);
        quizRepository.save(quiz);
    }

    public QuizWithQuestionsDto getQuizWithQuestions(Long quizId) {
        Quiz quiz = quizRepository.findQuizWithQuestionsById(quizId)
                .orElseThrow(() -> new QuizNotFoundException(quizId));
        List<AnswerOption> quizQuestionAnswerOptions = questionRepository.findAnswerOptionsByQuestionIds(
                quiz.getQuestions().stream().map(Question::getId).toList()
        );
        quiz.setQuestions(
                quiz.getQuestions().stream()
                        .map(q->{
                            q.setAnswerOptions(
                                    quizQuestionAnswerOptions.stream()
                                            .filter(ao -> ao.getQuestion().getId().equals(q.getId()))
                                            .toList()
                            );
                            return q;
                        })
                        .toList()
        );
        return quizMapper.toQuizWithQuestionsDto(quiz);
    }

    private boolean isThereQuizWithTheSameName(String title) {
        return quizRepository.existsByTitle(title);
    }
}
