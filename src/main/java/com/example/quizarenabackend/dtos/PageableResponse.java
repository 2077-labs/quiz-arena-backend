package com.example.quizarenabackend.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageableResponse<T> {
    private long totalElements;
    private int totalPages;
    private int pageSize;
    private List<T> content;

    public static <T> PageableResponse<T> of(Page<T> page) {
        return new PageableResponse<>(page.getTotalElements(), page.getTotalPages(), page.getSize(), page.getContent());
    }
}
