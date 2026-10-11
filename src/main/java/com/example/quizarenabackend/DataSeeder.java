package com.example.quizarenabackend;

import com.example.quizarenabackend.quiz.QuizRepository;
import com.example.quizarenabackend.quiz.entities.AnswerOption;
import com.example.quizarenabackend.quiz.entities.Question;
import com.example.quizarenabackend.quiz.entities.Quiz;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final QuizRepository quizRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (quizRepository.count() != 0) {
            System.out.println("Quizy już istnieją w bazie danych, pomijam tworzenie testowego quizu.");
            return;
        }
        Quiz quiz = new Quiz();
        quiz.setTitle("Quiz Testowy (Live)");
        quiz.setDescription("To jest testowy quiz stworzony automatycznie przy starcie aplikacji. Możesz go usunąć lub edytować.");

        Question q1 = new Question();
        q1.setText("Ile to 2 + 2?");
        q1.setPosition(0);

        AnswerOption a1 = new AnswerOption();
        a1.setText("3");
        a1.setPosition(0);

        AnswerOption a2 = new AnswerOption();
        a2.setText("4");
        a2.setCorrect(true);
        a2.setPosition(1);

        List.of(a1, a2).forEach(q1::addAnswerOption);
        quiz.addQuestion(q1);

        quizRepository.save(quiz);
        System.out.println("Dodano testowy quiz o ID: " + quiz.getId());
    }
}
