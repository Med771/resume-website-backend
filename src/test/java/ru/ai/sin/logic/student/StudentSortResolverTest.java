package ru.ai.sin.logic.student;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import ru.ai.sin.logic.student.dto.FilterStudentReq;
import ru.ai.sin.logic.student.dto.StudentSortDirection;
import ru.ai.sin.logic.student.dto.StudentSortField;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StudentSortResolverTest {

    @Test
    void defaultRanking_usesManualOrderAvatarThenScoreThenCreated() {
        Sort sort = StudentSortResolver.resolve(emptyFilter());
        List<Sort.Order> orders = sort.toList();
        assertThat(orders.get(0).getProperty()).isEqualTo("manualSortOrder");
        assertThat(orders.get(0).getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(orders.get(0).getNullHandling()).isEqualTo(Sort.NullHandling.NATIVE);
        assertThat(orders.get(1).getProperty()).isEqualTo("profileTextScore");
        assertThat(orders.get(1).getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(orders.get(2).getProperty()).isEqualTo("imagePath");
        assertThat(orders.get(2).getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(orders.get(2).getNullHandling()).isEqualTo(Sort.NullHandling.NATIVE);
        assertThat(orders.get(3).getProperty()).isEqualTo("timestamps.createdAt");
    }

    @Test
    void explicitLastNameAsc() {
        FilterStudentReq f = new FilterStudentReq(
                null, null, null, null, null, null, null,
                StudentSortField.LAST_NAME,
                StudentSortDirection.ASC,
                false,
                null,
                null
        );
        Sort sort = StudentSortResolver.resolve(f);
        assertThat(sort.toList().getFirst().getProperty()).isEqualTo("userInformation.lastName");
        assertThat(sort.toList().getFirst().getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void explicitManualSortOrderDesc() {
        FilterStudentReq f = new FilterStudentReq(
                null, null, null, null, null, null, null,
                StudentSortField.MANUAL_SORT_ORDER,
                StudentSortDirection.DESC,
                false,
                null,
                null
        );
        Sort sort = StudentSortResolver.resolve(f);
        assertThat(sort.toList().getFirst().getProperty()).isEqualTo("manualSortOrder");
        assertThat(sort.toList().getFirst().getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    private static FilterStudentReq emptyFilter() {
        return new FilterStudentReq(null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
