package vn.t3tarot.api.common.web;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages,
        boolean hasNext) {

    public PageResponse {
        items = List.copyOf(items);
    }

    public static <T> PageResponse<T> from(Page<T> source) {
        return new PageResponse<>(
                source.getContent(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages(),
                source.hasNext());
    }

    public static <T> PageResponse<T> of(List<T> items, PageQuery query, long totalItems) {
        int totalPages = Math.toIntExact((totalItems + query.size() - 1) / query.size());
        boolean hasNext = query.page() + 1 < totalPages;
        return new PageResponse<>(items, query.page(), query.size(), totalItems, totalPages, hasNext);
    }

    public <R> PageResponse<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mappedItems = items.stream().<R>map(mapper).toList();
        return new PageResponse<>(mappedItems, page, size, totalItems, totalPages, hasNext);
    }
}
