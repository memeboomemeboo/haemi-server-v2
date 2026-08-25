package com.memeboo2.haemi.guardian.profile.application;

import com.memeboo2.haemi.auth.api.AccountQuery;
import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.guardian.api.GuardianRole;
import com.memeboo2.haemi.guardian.eldermanagement.domain.Elder;
import com.memeboo2.haemi.guardian.eldermanagement.domain.ElderRepository;
import com.memeboo2.haemi.guardian.eldermanagement.domain.GuardianElderLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetGuardianProfileUseCase {

    private final AccountQuery accountQuery;
    private final GuardianElderLinkRepository linkRepository;
    private final ElderRepository elderRepository;

    public record ElderCard(UUID elderId, String name, LocalDate birthDate, GuardianRole role) {}

    public record GuardianProfile(
            UUID userId,
            String name,
            String loginId,
            String phone,
            String birthDate,
            String profileImageUrl,
            List<ElderCard> elders
    ) {}

    @Transactional(readOnly = true)
    public GuardianProfile execute(UUID guardianId) {
        AccountQuery.AccountInfo account = accountQuery.findById(guardianId)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND));

        List<ElderCard> elders = linkRepository.findAllByGuardianId(guardianId).stream()
                .map(link -> {
                    Elder elder = elderRepository.findById(link.getElderId()).orElse(null);
                    if (elder == null) return null;
                    return new ElderCard(elder.getId(), elder.getName(), elder.getBirthDate(), link.getRole());
                })
                .filter(c -> c != null)
                .toList();

        return new GuardianProfile(
                guardianId,
                account.name(),
                account.loginId(),
                account.phone(),
                account.birthDate(),
                account.profileImageUrl(),
                elders
        );
    }
}
