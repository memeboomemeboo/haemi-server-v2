package com.memeboo2.haemi.elder.training.application;

import com.memeboo2.haemi.common.security.ElderAccessChecked;
import java.util.UUID;

/** CIST-TRN-001~006 인지 훈련의 세션·응답·결과 조회 계약이다. */
public interface TrainingSessionUseCase {

    @ElderAccessChecked
    TrainingSessionView enter(UUID elderUserId);

    @ElderAccessChecked
    TrainingSessionView submitCurrentAnswer(
            UUID elderUserId,
            UUID sessionId,
            UUID questionId,
            int questionNumber,
            String selectedOption,
            String textAnswer,
            UUID voiceMediaRefId
    );

    @ElderAccessChecked
    TrainingResultView result(UUID elderUserId);
}
