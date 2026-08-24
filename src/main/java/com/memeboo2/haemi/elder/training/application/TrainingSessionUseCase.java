package com.memeboo2.haemi.elder.training.application;

import com.memeboo2.haemi.common.security.ElderAccessChecked;
import com.memeboo2.haemi.elder.training.domain.QuestionType;

import java.util.UUID;

/** CIST-TRN-001 인지 훈련 세션의 진입과 단계 완료 계약이다. */
public interface TrainingSessionUseCase {

    @ElderAccessChecked
    TrainingSessionView enter(UUID elderUserId);

    @ElderAccessChecked
    TrainingSessionView completeCurrentQuestion(UUID elderUserId, QuestionType questionType);
}
