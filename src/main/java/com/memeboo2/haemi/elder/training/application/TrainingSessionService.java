package com.memeboo2.haemi.elder.training.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.common.event.CognitiveTrainingCompleted;
import com.memeboo2.haemi.common.event.TrainingSessionCompleted;
import com.memeboo2.haemi.common.security.ElderAccessChecked;
import com.memeboo2.haemi.common.time.HaemiClock;
import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.SessionStatus;
import com.memeboo2.haemi.elder.training.domain.TrainingAnswer;
import com.memeboo2.haemi.elder.training.domain.TrainingQuestion;
import com.memeboo2.haemi.elder.training.domain.TrainingSession;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingAnswerRepository;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingQuestionRepository;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingSessionRepository;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.memeboo2.haemi.guardian.api.ElderProfileQuery;
import com.memeboo2.haemi.platform.api.MediaPurpose;
import com.memeboo2.haemi.platform.api.MediaUploadCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** CIST-TRN-001~006의 세션 진입, 실제 답변, 결과 조회를 조합한다. */
@Service
@RequiredArgsConstructor
public class TrainingSessionService implements TrainingSessionUseCase {

    private final TrainingSessionRepository trainingSessionRepository;
    private final TrainingQuestionRepository questionRepository;
    private final TrainingAnswerRepository answerRepository;
    private final TrainingQuestionGenerationService questionGenerationService;
    private final TrainingResultService resultService;
    private final TrainingDifficultyService difficultyService;
    private final TrainingPolicyProperties policy;
    private final HaemiClock clock;
    private final CareAccessQuery careAccessQuery;
    private final ElderProfileQuery elderProfileQuery;
    private final MediaUploadCommand mediaUploadCommand;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @ElderAccessChecked
    @Transactional
    public TrainingSessionView enter(UUID elderUserId) {
        UUID elderId = requireElderId(elderUserId);
        Instant now = clock.now();
        return trainingSessionRepository
                .findFirstByElderIdAndStatusForUpdate(elderId, SessionStatus.IN_PROGRESS)
                .map(session -> currentView(session, elderId, now))
                .orElseGet(() -> completedTodayOrStart(elderId, now));
    }

    @Override
    @ElderAccessChecked
    @Transactional
    public TrainingSessionView submitCurrentAnswer(
            UUID elderUserId,
            UUID sessionId,
            UUID questionId,
            int questionNumber,
            String selectedOption,
            String textAnswer,
            UUID voiceMediaRefId
    ) {
        UUID elderId = requireElderId(elderUserId);
        Instant now = clock.now();
        TrainingSession session = trainingSessionRepository
                .findFirstByElderIdAndStatusForUpdate(elderId, SessionStatus.IN_PROGRESS)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "진행 중인 인지 훈련 세션이 없습니다."));
        if (!session.getId().equals(sessionId) || !Integer.valueOf(questionNumber).equals(session.getCurrentQuestionNumber())) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "현재 진행 중인 문항이 아닙니다.");
        }

        TrainingQuestion question = questionRepository.findBySessionIdAndQuestionNumber(sessionId, questionNumber)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "인지 훈련 문항을 찾을 수 없습니다."));
        if (!question.getId().equals(questionId)) {
            throw new DomainException(ErrorCode.INVALID_INPUT, "현재 진행 중인 문항이 아닙니다.");
        }

        String voiceMediaKey = voiceMediaRefId == null ? null
                : mediaUploadCommand.confirmUpload(elderUserId, voiceMediaRefId, MediaPurpose.RESPONSE_VOICE).toString();
        Boolean evaluated = question.evaluate(selectedOption, textAnswer, voiceMediaKey);
        answerRepository.save(TrainingAnswer.record(
                sessionId, questionId, elderId, questionNumber, question.getQuestionType(),
                selectedOption, textAnswer, voiceMediaKey, evaluated, now));

        boolean lastQuestion = questionNumber == policy.totalQuestionCount();
        QuestionType nextStep = lastQuestion ? null : questionRepository
                .findBySessionIdAndQuestionNumber(sessionId, questionNumber + 1)
                .map(TrainingQuestion::getQuestionType)
                .orElseThrow(() -> new IllegalStateException("다음 인지 훈련 문항이 없습니다."));
        session.completeCurrentQuestion(
                sessionId, question.getQuestionType(), questionNumber, nextStep, lastQuestion, now);
        // 비관 잠금으로 조회한 managed 엔티티이므로 트랜잭션 커밋 시 변경 감지로 저장된다.
        if (session.getStatus() == SessionStatus.COMPLETED) {
            difficultyService.evaluateCompletedSession(session);
            LocalDate completedDate = HaemiClock.dateInKst(session.getCompletedAt());
            eventPublisher.publishEvent(new TrainingSessionCompleted(session.getElderId(), completedDate));
            eventPublisher.publishEvent(new CognitiveTrainingCompleted(
                    session.getElderId(), session.getId(), completedDate, cognitiveAreaResults(session.getId())));
        }
        return viewOf(session, feedbackFor(question, evaluated, session));
    }

    @Override
    @ElderAccessChecked
    @Transactional(readOnly = true)
    public TrainingResultView result(UUID elderUserId) {
        UUID elderId = requireElderId(elderUserId);
        LocalDate today = HaemiClock.dateInKst(clock.now());
        TrainingSession session = completedOn(elderId, today)
                .orElseThrow(() -> new DomainException(ErrorCode.RESOURCE_NOT_FOUND, "오늘 완료한 인지 훈련이 없습니다."));
        return resultService.resultFor(session);
    }

    private TrainingSessionView completedTodayOrStart(UUID elderId, Instant now) {
        LocalDate today = HaemiClock.dateInKst(now);
        return completedOn(elderId, today)
                .map(this::viewOf)
                .orElseGet(() -> start(elderId, now, today));
    }

    private Optional<TrainingSession> completedOn(UUID elderId, LocalDate date) {
        Instant startOfDay = date.atStartOfDay(HaemiClock.KST).toInstant();
        Instant startOfNextDay = date.plusDays(1).atStartOfDay(HaemiClock.KST).toInstant();
        return trainingSessionRepository
                .findFirstByElderIdAndStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
                        elderId, SessionStatus.COMPLETED, startOfDay, startOfNextDay);
    }

    private TrainingSessionView start(UUID elderId, Instant now, LocalDate today) {
        TrainingSession session = TrainingSession.start(elderId, now, today);
        TrainingSession saved;
        try {
            saved = trainingSessionRepository.saveAndFlush(session);
        } catch (DataIntegrityViolationException ex) {
            throw new DomainException(ErrorCode.TRAINING_SESSION_ALREADY_STARTED);
        }
        questionGenerationService.generateIfAbsent(saved, elderId, elderAge(elderId, today), today);
        return viewOf(saved);
    }

    private TrainingSessionView currentView(TrainingSession session, UUID elderId, Instant now) {
        LocalDate today = HaemiClock.dateInKst(now);
        questionGenerationService.generateIfAbsent(session, elderId, elderAge(elderId, today), today);
        return viewOf(session);
    }

    private TrainingSessionView viewOf(TrainingSession session) {
        return viewOf(session, null);
    }

    private TrainingSessionView viewOf(TrainingSession session, String feedback) {
        TrainingQuestionView currentQuestion = session.getStatus() == SessionStatus.IN_PROGRESS
                ? questionRepository.findBySessionIdAndQuestionNumber(session.getId(), session.getCurrentQuestionNumber())
                        .map(TrainingQuestionView::from)
                        .orElseThrow(() -> new IllegalStateException("현재 인지 훈련 문항이 없습니다."))
                : null;
        TrainingResultView result = session.getStatus() == SessionStatus.COMPLETED ? resultService.resultFor(session) : null;
        return TrainingSessionView.from(
                session, policy.totalQuestionCount(), policy.inactivityReminderSeconds(), feedback, currentQuestion, result);
    }

    /**
     * 완료 트랜잭션 안에서 리포트용 최소 집계만 만든다.
     * guardian/report는 이 이벤트를 소비할 뿐 training 저장소를 역으로 조회하지 않는다.
     */
    private List<CognitiveTrainingCompleted.CognitiveAreaResult> cognitiveAreaResults(UUID sessionId) {
        return java.util.Arrays.stream(QuestionType.values())
                .map(type -> {
                    List<TrainingAnswer> answers = answerRepository.findBySessionIdAndQuestionType(sessionId, type);
                    int scored = (int) answers.stream().filter(answer -> answer.getCorrect() != null).count();
                    int correct = (int) answers.stream().filter(answer -> Boolean.TRUE.equals(answer.getCorrect())).count();
                    return new CognitiveTrainingCompleted.CognitiveAreaResult(type.name(), scored, correct);
                })
                .toList();
    }

    private String feedbackFor(TrainingQuestion question, Boolean evaluated, TrainingSession session) {
        if (session.getStatus() == SessionStatus.COMPLETED) {
            return "훈련을 마쳤어요. 오늘도 수고했어요.";
        }
        if (question.getQuestionType() != QuestionType.ORIENTATION) {
            return "답변을 기록했어요. 다음 문제로 가볼까요?";
        }
        return Boolean.TRUE.equals(evaluated)
                ? "맞아요. 다음 문제로 가볼까요?"
                : "괜찮아요. 다음 문제로 가볼까요?";
    }

    private Integer elderAge(UUID elderId, LocalDate today) {
        LocalDate birthDate = elderProfileQuery.findById(elderId).birthDate();
        return birthDate == null ? null : Period.between(birthDate, today).getYears();
    }

    private UUID requireElderId(UUID elderUserId) {
        UUID elderId = careAccessQuery.elderIdForUser(elderUserId);
        careAccessQuery.requireSelf(elderUserId, elderId);
        return elderId;
    }

}
