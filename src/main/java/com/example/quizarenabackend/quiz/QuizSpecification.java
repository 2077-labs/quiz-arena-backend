package com.example.quizarenabackend.quiz;

import com.example.quizarenabackend.quiz.entities.Quiz;
import org.springframework.data.jpa.domain.Specification;

public class QuizSpecification {
    public static Specification<Quiz> search(String searchText) {
        return (root, query, cb) -> {
            if (searchText == null || searchText.isEmpty()) {
                return cb.conjunction();
            }
            return cb.or(
                    cb.like(cb.lower(root.get("title")), "%" + searchText.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + searchText.toLowerCase() + "%")
            );
        };
    }
}
