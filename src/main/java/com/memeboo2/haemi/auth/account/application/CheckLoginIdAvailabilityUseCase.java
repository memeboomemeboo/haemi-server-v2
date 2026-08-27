package com.memeboo2.haemi.auth.account.application;

import com.memeboo2.haemi.auth.account.infrastructure.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckLoginIdAvailabilityUseCase {

    private final AccountRepository accountRepository;

    /** loginId가 아직 사용 가능하면 true. 회원가입/프로필 수정/어르신 등록의 "중복 확인" 버튼용. */
    @Transactional(readOnly = true)
    public boolean isAvailable(String loginId) {
        return !accountRepository.existsByLoginId(loginId);
    }
}
