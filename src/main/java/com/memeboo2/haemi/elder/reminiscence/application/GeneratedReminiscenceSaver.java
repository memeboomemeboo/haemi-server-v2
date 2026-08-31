package com.memeboo2.haemi.elder.reminiscence.application;

import com.memeboo2.haemi.elder.reminiscence.domain.GeneratedReminiscence;
import com.memeboo2.haemi.elder.reminiscence.infrastructure.GeneratedReminiscenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 회상 콘텐츠 upsert 전용 짧은 트랜잭션 경계.
 * Gemini 대기(외부 HTTP)와 분리된 별도 빈이라, 저장 트랜잭션이 LLM 응답을 기다리며
 * DB 커넥션을 점유하지 않는다. (self-invocation 시 @Transactional이 무시되는 것을 피하기 위한 분리)
 */
@Service
@RequiredArgsConstructor
public class GeneratedReminiscenceSaver {

    private final GeneratedReminiscenceRepository repository;

    /** (elderId, date) 당 하나를 생성하거나 갱신한다. */
    @Transactional
    public GeneratedReminiscence upsert(UUID elderId, LocalDate date, String content, boolean aiGenerated) {
        return repository.findByElderIdAndContentDate(elderId, date)
                .map(existing -> {
                    existing.update(content, aiGenerated);
                    return existing;
                })
                .orElseGet(() -> repository.save(GeneratedReminiscence.of(elderId, date, content, aiGenerated)));
    }
}
