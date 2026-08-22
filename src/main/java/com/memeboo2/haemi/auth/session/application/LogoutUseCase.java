package com.memeboo2.haemi.auth.session.application;

import com.memeboo2.haemi.auth.session.infrastructure.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void execute(UUID accountId) {
        refreshTokenRepository.deleteByAccountId(accountId);
    }
}
