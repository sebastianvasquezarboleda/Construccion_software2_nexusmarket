package application.domain.models;

import java.util.List;

public record PageResult<T>(List<T> content, int page, int size, long totalElements, long totalPages) {
    public PageResult(List<T> content, int page, int size, long totalElements) {
        this(content, page, size, totalElements, calculateTotalPages(totalElements, size));
    }

    public PageResult {
        if (page < 0) {
            throw new IllegalArgumentException("page cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        if (totalElements < 0) {
            throw new IllegalArgumentException("totalElements cannot be negative");
        }
        if (totalPages != calculateTotalPages(totalElements, size)) {
            throw new IllegalArgumentException("totalPages does not match totalElements and size");
        }
        content = List.copyOf(content);
    }

    public static <T> PageResult<T> from(List<T> items, int page, int size) {
        validate(page, size);
        long start = (long) page * size;
        int fromIndex = (int) Math.min(start, items.size());
        int toIndex = (int) Math.min((long) fromIndex + size, items.size());
        return new PageResult<>(items.subList(fromIndex, toIndex), page, size, items.size());
    }

    public static void validate(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page cannot be negative");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
    }

    private static long calculateTotalPages(long totalElements, int size) {
        if (size < 1) {
            throw new IllegalArgumentException("size must be positive");
        }
        return totalElements == 0 ? 0 : (totalElements - 1) / size + 1;
    }
}
