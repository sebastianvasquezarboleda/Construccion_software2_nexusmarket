package application.domain.ports.out;

import application.domain.models.PageResult;

import java.util.List;

public interface PagedRepositoryPort<T> {
    List<T> findAll();

    default PageResult<T> findPage(int page, int size) {
        return PageResult.from(findAll(), page, size);
    }
}
