package com.memeboo2.haemi.guardian.presentation;

import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.family.application.GetFamilyDetailUseCase.ElderCard;
import com.memeboo2.haemi.guardian.family.application.GetFamilyDetailUseCase.FamilyDetail;
import com.memeboo2.haemi.guardian.family.application.GetFamilyDetailUseCase.GuardianMember;
import com.memeboo2.haemi.guardian.presentation.dto.FamilyDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FamilyDetailResponseTest {

    @Test
    @DisplayName("FamilyDetail로부터 가족 정보, 보호자 목록, 어르신 목록을 매핑한다")
    void from_전체_필드를_매핑한다() {
        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID elderId = UUID.randomUUID();
        LocalDate birthDate = LocalDate.of(1945, 3, 1);

        GuardianMember member = new GuardianMember(userId, "이보호자", GuardianRole.DAUGHTER, true);
        ElderCard elderCard = new ElderCard(elderId, "김할머니", birthDate, GuardianRole.DAUGHTER);

        FamilyDetail detail = new FamilyDetail(
                familyId, "우리가족", "메모", "http://image", "INVITE123",
                List.of(member), List.of(elderCard));

        FamilyDetailResponse response = FamilyDetailResponse.from(detail);

        assertThat(response.familyId()).isEqualTo(familyId);
        assertThat(response.name()).isEqualTo("우리가족");
        assertThat(response.memo()).isEqualTo("메모");
        assertThat(response.profileImageUrl()).isEqualTo("http://image");
        assertThat(response.inviteCode()).isEqualTo("INVITE123");

        assertThat(response.guardians()).hasSize(1);
        assertThat(response.guardians().get(0).userId()).isEqualTo(userId);
        assertThat(response.guardians().get(0).name()).isEqualTo("이보호자");
        assertThat(response.guardians().get(0).role()).isEqualTo(GuardianRole.DAUGHTER);
        assertThat(response.guardians().get(0).roleLabel()).isEqualTo("딸");
        assertThat(response.guardians().get(0).isMe()).isTrue();

        assertThat(response.elders()).hasSize(1);
        assertThat(response.elders().get(0).elderId()).isEqualTo(elderId);
        assertThat(response.elders().get(0).name()).isEqualTo("김할머니");
        assertThat(response.elders().get(0).birthDate()).isEqualTo(birthDate);
        assertThat(response.elders().get(0).myRole()).isEqualTo(GuardianRole.DAUGHTER);
        assertThat(response.elders().get(0).myRoleLabel()).isEqualTo("딸");
    }

    @Test
    @DisplayName("보호자 role이 null이면 roleLabel도 null이다")
    void from_보호자_role이_null이면_roleLabel도_null이다() {
        GuardianMember member = new GuardianMember(UUID.randomUUID(), "무역할", null, false);
        FamilyDetail detail = new FamilyDetail(
                UUID.randomUUID(), "가족", null, null, "CODE",
                List.of(member), List.of());

        FamilyDetailResponse response = FamilyDetailResponse.from(detail);

        assertThat(response.guardians().get(0).role()).isNull();
        assertThat(response.guardians().get(0).roleLabel()).isNull();
    }

    @Test
    @DisplayName("어르신 myRole이 null이면 myRoleLabel도 null이다")
    void from_어르신_myRole이_null이면_myRoleLabel도_null이다() {
        ElderCard elderCard = new ElderCard(UUID.randomUUID(), "무관계어르신", LocalDate.of(1950, 1, 1), null);
        FamilyDetail detail = new FamilyDetail(
                UUID.randomUUID(), "가족", null, null, "CODE",
                List.of(), List.of(elderCard));

        FamilyDetailResponse response = FamilyDetailResponse.from(detail);

        assertThat(response.elders().get(0).myRole()).isNull();
        assertThat(response.elders().get(0).myRoleLabel()).isNull();
    }
}
