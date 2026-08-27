package com.memeboo2.haemi.elder.response;

import com.memeboo2.haemi.elder.response.application.GetResponsesUseCase;
import com.memeboo2.haemi.elder.response.domain.Response;
import com.memeboo2.haemi.elder.response.infrastructure.ResponseRepository;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.ElderMemoryQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetResponsesUseCaseTest {

    @Mock ResponseRepository responseRepository;
    @Mock CareAccessQuery careAccessQuery;
    @Mock ElderMemoryQuery elderMemoryQuery;
    @InjectMocks GetResponsesUseCase sut;

    @Test
    void 본인_답변_목록을_조회한다() {
        UUID elderUserId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();
        UUID memoryId = UUID.randomUUID();
        given(careAccessQuery.elderIdForUser(elderUserId)).willReturn(elderId);
        var memoryItem = new ElderMemoryQuery.MemoryItem(
                memoryId, "제목", "메모", "메시지", 2020, List.of(),
                false, java.time.Instant.now(), "홍길동",
                com.memeboo2.haemi.guardian.api.GuardianRole.DAUGHTER);
        given(elderMemoryQuery.findForElder(memoryId, elderId)).willReturn(Optional.of(memoryItem));
        Response response = org.mockito.Mockito.mock(Response.class);
        given(responseRepository.findByMemoryIdAndElderId(memoryId, elderId))
                .willReturn(List.of(response));

        List<Response> result = sut.execute(elderUserId, memoryId);

        assertThat(result).containsExactly(response);
    }

    @Test
    void 존재하지_않는_추억이면_404() {
        UUID elderUserId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();
        UUID memoryId = UUID.randomUUID();
        given(careAccessQuery.elderIdForUser(elderUserId)).willReturn(elderId);
        given(elderMemoryQuery.findForElder(memoryId, elderId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> sut.execute(elderUserId, memoryId))
                .isInstanceOf(DomainException.class)
                .extracting(e -> ((DomainException) e).getErrorCode())
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }
}
