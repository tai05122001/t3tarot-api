package vn.t3tarot.api.common.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record PageQuery(
        @Min(0) Integer page,
        @Min(1) @Max(PageQuery.MAX_SIZE) Integer size) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 50;

    public PageQuery {
        page = page == null ? DEFAULT_PAGE : page;
        size = size == null ? DEFAULT_SIZE : size;
    }

    public static PageQuery firstPage() {
        return new PageQuery(DEFAULT_PAGE, DEFAULT_SIZE);
    }

    public long offset() {
        return (long) page * size;
    }

    public Pageable toPageable(Sort sort) {
        return PageRequest.of(page, size, sort);
    }
}
