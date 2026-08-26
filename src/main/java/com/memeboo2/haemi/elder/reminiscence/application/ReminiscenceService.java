package com.memeboo2.haemi.elder.reminiscence.application;

import com.memeboo2.haemi.common.security.ElderAccessChecked;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.guardian.api.ElderProfileQuery;
import com.memeboo2.haemi.guardian.api.ElderQuery;
import com.memeboo2.haemi.elder.reminiscence.domain.GeneratedReminiscence;
import com.memeboo2.haemi.elder.reminiscence.infrastructure.GeneratedReminiscenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.UUID;

/** 어르신 컨텍스트로 Gemini 프롬프트를 구성해 개인화 회상 콘텐츠를 생성·저장한다. */
@Service
@RequiredArgsConstructor
public class ReminiscenceService {

    /** ai_reminiscence_contents.content 컬럼 상한 (V127). LLM 응답이 길어도 초과 저장 실패를 막는다. */
    private static final int MAX_CONTENT_LENGTH = 2000;

    private final AiTextGenerator generator;
    private final ElderQuery elderQuery;
    private final ElderProfileQuery elderProfileQuery;
    private final GeneratedReminiscenceRepository repository;
    private final HaemiClock clock;

    /**
     * 지정 어르신의 지정 날짜 회상 콘텐츠를 생성(또는 갱신)한다. (elderId, date) 당 하나.
     * <p>elderId는 호출부에서 확정된 도메인 ID다: 어르신 엔드포인트는 JWT 사용자 ID를
     * {@code CareAccessQuery.elderIdForUser}로 해석해 본인으로 제한하고, 배치는 신뢰된 시스템 호출이다.
     */
    @ElderAccessChecked
    @Transactional
    public GeneratedReminiscence generateForElder(UUID elderId, LocalDate date) {
        String prompt = buildPrompt(elderId, date);
        String content = truncate(generator.generate(prompt));
        boolean live = generator.isLive();

        GeneratedReminiscence saved = repository.findByElderIdAndContentDate(elderId, date)
                .map(existing -> {
                    existing.update(content, live);
                    return existing;
                })
                .orElseGet(() -> repository.save(GeneratedReminiscence.of(elderId, date, content, live)));
        return saved;
    }

    @ElderAccessChecked
    @Transactional(readOnly = true)
    public Optional<GeneratedReminiscence> findForElder(UUID elderId, LocalDate date) {
        return repository.findByElderIdAndContentDate(elderId, date);
    }

    private String truncate(String content) {
        if (content == null) {
            return "";
        }
        return content.length() <= MAX_CONTENT_LENGTH ? content : content.substring(0, MAX_CONTENT_LENGTH);
    }

    private String buildPrompt(UUID elderId, LocalDate date) {
        String name = elderQuery.findById(elderId).map(ElderQuery.ElderInfo::name).orElse("어르신");
        Integer age = ageOf(elderId, date);
        String generation = age == null ? "어르신" : (age / 10 * 10) + "대";

        return """
                당신은 노인 인지 건강을 돕는 따뜻한 회상 도우미입니다.
                아래 대상 어르신을 위해 오늘의 개인화 회상 콘텐츠를 한국어로 3~4문장 작성하세요.
                - 대상: %s님 (%s)
                - 목적: 옛 기억을 부드럽게 떠올리도록 유도 (노래, 명절, 음식, 고향 등 소재)
                - 어조: 존댓말, 다정하고 이해하기 쉬운 문장
                - 비교·점수·평가 표현 금지, 부담을 주지 않을 것
                오늘 날짜는 %s 입니다.
                """.formatted(name, generation, date);
    }

    private Integer ageOf(UUID elderId, LocalDate date) {
        try {
            ElderProfileQuery.ElderProfile profile = elderProfileQuery.findById(elderId);
            if (profile == null || profile.birthDate() == null) {
                return null;
            }
            return Period.between(profile.birthDate(), date).getYears();
        } catch (RuntimeException e) {
            return null;
        }
    }
}
