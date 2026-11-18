package vn.t3tarot.api.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class PageResponseTest {

    @Test
    void copiesMetadataFromSpringPage() {
        PageImpl<String> source = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 5);

        PageResponse<String> response = PageResponse.from(source);

        assertThat(response.items()).containsExactly("a", "b");
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalItems()).isEqualTo(5);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void hasNoNextWhenOnLastPage() {
        PageResponse<String> response = PageResponse.of(List.of("e"), new PageQuery(2, 2), 5);

        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void hasZeroPagesWhenNoItems() {
        PageResponse<String> response = PageResponse.of(List.of(), PageQuery.firstPage(), 0);

        assertThat(response.totalPages()).isZero();
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void mapsItemsAndKeepsMetadata() {
        PageResponse<Integer> response = PageResponse.of(List.of(1, 2), new PageQuery(0, 2), 3).map(number -> number * 10);

        assertThat(response.items()).containsExactly(10, 20);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void isImmutableWhenSourceListChanges() {
        List<String> source = new ArrayList<>(List.of("a"));

        PageResponse<String> response = PageResponse.of(source, PageQuery.firstPage(), 1);
        source.add("b");

        assertThat(response.items()).containsExactly("a");
        assertThatThrownBy(() -> response.items().add("c")).isInstanceOf(UnsupportedOperationException.class);
    }
}
