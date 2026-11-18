package vn.t3tarot.api.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class PageQueryTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void appliesDefaultsWhenParamsMissing() {
        PageQuery query = new PageQuery(null, null);

        assertThat(query.page()).isEqualTo(PageQuery.DEFAULT_PAGE);
        assertThat(query.size()).isEqualTo(PageQuery.DEFAULT_SIZE);
        assertThat(query).isEqualTo(PageQuery.firstPage());
    }

    @Test
    void computesOffsetWithoutIntOverflowWhenPageIsLarge() {
        PageQuery query = new PageQuery(Integer.MAX_VALUE, PageQuery.MAX_SIZE);

        assertThat(query.offset()).isEqualTo((long) Integer.MAX_VALUE * PageQuery.MAX_SIZE);
    }

    @Test
    void rejectsNegativePageAndOversizedSize() {
        assertThat(VALIDATOR.validate(new PageQuery(-1, PageQuery.MAX_SIZE + 1)))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("page", "size");
    }

    @Test
    void rejectsZeroSize() {
        assertThat(VALIDATOR.validate(new PageQuery(0, 0))).hasSize(1);
    }

    @Test
    void buildsPageableWithServerChosenSort() {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        Pageable pageable = new PageQuery(2, 10).toPageable(sort);

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort()).isEqualTo(sort);
    }
}
