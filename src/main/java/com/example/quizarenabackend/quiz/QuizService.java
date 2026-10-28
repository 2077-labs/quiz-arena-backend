package com.example.quizarenabackend.quiz;

import com.example.quizarenabackend.quiz.dtos.CreateQuizDto;
import com.example.quizarenabackend.quiz.dtos.GetQuizzesRequest;
import com.example.quizarenabackend.quiz.dtos.QuizDto;
import com.example.quizarenabackend.quiz.dtos.QuizWithQuestionsDto;
import com.example.quizarenabackend.quiz.entities.AnswerOption;
import com.example.quizarenabackend.quiz.entities.Question;
import com.example.quizarenabackend.quiz.entities.Quiz;
import com.example.quizarenabackend.quiz.exceptons.QuizNotFoundException;
import com.example.quizarenabackend.quiz.exceptons.SameTitleQuizExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuizService {
    private final QuizMapper quizMapper;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;

    @Value("${quizarena.search.trending-timeframe}")
    private Duration trendingTimeframe;

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

    public Quiz getQuizWithQuestions(Long quizId) {
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
        return quiz;
    }

    public QuizWithQuestionsDto getQuizWithQuestionsDto(Long quizId) {
        Quiz quiz = getQuizWithQuestions(quizId);
        return quizMapper.toQuizWithQuestionsDto(quiz);
    }

    @Transactional(readOnly = true)
    public Page<QuizDto> getQuizzes(GetQuizzesRequest request, Pageable pageable) {
        Specification<Quiz> spec = Specification.unrestricted();
        spec = spec.and(QuizSpecification.search(request.getSearchText()));

        return quizRepository.findAll(spec, pageable)
                .map(quizMapper::toQuizDto);
    }

    @Transactional(readOnly = true)
    public List<QuizDto> getTrendingQuizzes(int limit) {
        var res = quizRepository.findTrendingQuizzes(
                Instant.now().minusMillis(trendingTimeframe.toMillis()),
                Pageable.ofSize(limit)
        );
        if (res.isEmpty()) {
            res = quizRepository.findByOrderByRunCountDesc(Pageable.ofSize(limit));
        }
        return res.stream()
                .map(quizMapper::toQuizDto)
                .toList();
    }

    private boolean isThereQuizWithTheSameName(String title) {
        return quizRepository.existsByTitle(title);
    }
}
