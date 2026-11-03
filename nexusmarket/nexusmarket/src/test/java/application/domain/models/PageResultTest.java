package application.domain.models;

import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageResultTest {
    @Test
    void slicesCollectionsAndReportsPageMetadata() {
        PageResult<Integer> result = PageResult.from(List.of(1, 2, 3, 4, 5), 1, 2);

        assertEquals(List.of(3, 4), result.content());
        assertEquals(1, result.page());
        assertEquals(2, result.size());
        assertEquals(5, result.totalElements());
        assertEquals(3, result.totalPages());
    }

    @Test
    void validatesPageBoundsAndReturnsEmptyPagesPastTheEnd() {
        assertEquals(List.of(), PageResult.from(List.of(1, 2), 5, 2).content());
        assertThrows(IllegalArgumentException.class, () -> PageResult.from(List.of(1), -1, 10));
        assertThrows(IllegalArgumentException.class, () -> PageResult.from(List.of(1), 0, 101));
    }

    @Test
    void serializesAllPaginationMetadata() throws Exception {
        String json = JsonMapper.builder().build().writeValueAsString(PageResult.from(List.of(1, 2, 3), 0, 2));

        assertTrue(json.contains("\"content\":[1,2]"));
        assertTrue(json.contains("\"page\":0"));
        assertTrue(json.contains("\"size\":2"));
        assertTrue(json.contains("\"totalElements\":3"));
        assertTrue(json.contains("\"totalPages\":2"));
    }
}
