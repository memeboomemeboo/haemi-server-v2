package com.memeboo2.haemi.elder.training.application;

import com.memeboo2.haemi.common.error.DomainException;
import com.memeboo2.haemi.common.error.ErrorCode;
import com.memeboo2.haemi.elder.training.domain.DifficultyLevel;
import com.memeboo2.haemi.elder.training.domain.MaterialSource;
import com.memeboo2.haemi.elder.training.domain.QuestionKind;
import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.TrainingDifficulty;
import com.memeboo2.haemi.elder.training.domain.TrainingMaterial;
import com.memeboo2.haemi.elder.training.domain.TrainingQuestion;
import com.memeboo2.haemi.elder.training.domain.TrainingSession;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingDifficultyRepository;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingQuestionRepository;
import com.memeboo2.haemi.guardian.api.MemoryQuery;
import com.memeboo2.haemi.platform.content.api.ContentMaterial;
import com.memeboo2.haemi.platform.content.api.ContentQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** CIST-TRN-002~005의 10개 문항을 세션 시작 시 고정해 이탈 후에도 같은 지점에서 이어가게 한다. */
@Service
@RequiredArgsConstructor
public class TrainingQuestionGenerationService {

    private static final Locale KOREAN = Locale.KOREAN;

    private final TrainingQuestionRepository questionRepository;
    private final TrainingDifficultyRepository difficultyRepository;
    private final MemoryQuery memoryQuery;
    private final ContentQuery contentQuery;
    private final TrainingPolicyProperties policy;

    List<TrainingQuestion> generateIfAbsent(TrainingSession session, UUID elderId, Integer elderAge, LocalDate today) {
        List<TrainingQuestion> existing = questionRepository.findBySessionIdOrderByQuestionNumberAsc(session.getId());
        if (!existing.isEmpty()) {
            return existing;
        }

        DifficultyLevel recallDifficulty = difficultyOf(elderId, QuestionType.RECALL);
        // LANGUAGE는 자유 서술/음성 참여형이라 자동 채점·난이도 조정을 하지 않는다.
        DifficultyLevel languageDifficulty = DifficultyLevel.LEVEL_1;
        List<TrainingMaterial> materials = trainingMaterials(elderId, elderAge);

        List<TrainingQuestion> questions = new ArrayList<>();
        questions.addAll(orientationQuestions(session.getId(), today));
        questions.addAll(recallQuestions(session.getId(), materials, recallDifficulty));
        questions.addAll(languageQuestions(session.getId(), materials, languageDifficulty));
        questions.addAll(delayedRecallQuestions(session.getId(), materials, recallDifficulty));

        if (questions.size() != policy.totalQuestionCount()) {
            throw new IllegalStateException("CIST 문항 구성 수가 설정값과 일치하지 않습니다.");
        }
        return questionRepository.saveAll(questions);
    }

    private List<TrainingMaterial> trainingMaterials(UUID elderId, Integer elderAge) {
        List<TrainingMaterial> materials = new ArrayList<>(memoryQuery.materialsFor(elderId, policy.recallQuestionCount())
                .stream()
                .filter(memory -> !memory.imageKeys().isEmpty())
                .map(memory -> new TrainingMaterial(
                        memory.id(), MaterialSource.MEMORY, memory.title(), memory.imageKeys().getFirst(),
                        memory.memoryYear(), List.of(memory.title())))
                .toList());

        if (materials.size() < policy.recallQuestionCount()) {
            Set<UUID> excluded = materials.stream().map(TrainingMaterial::id).collect(Collectors.toSet());
            List<ContentMaterial> contents = contentQuery.selectForTraining(
                    elderId, elderAge, policy.recallQuestionCount() - materials.size(), excluded);
            materials.addAll(contents.stream()
                    .map(content -> new TrainingMaterial(
                            content.id(), MaterialSource.CONTENT, content.title(), content.imageKey(),
                            content.contentYear(), content.answerKeywords()))
                    .toList());
        }

        if (materials.isEmpty()) {
            throw new DomainException(ErrorCode.TRAINING_MATERIAL_UNAVAILABLE);
        }
        List<TrainingMaterial> sourceMaterials = List.copyOf(materials);
        while (materials.size() < policy.recallQuestionCount()) {
            materials.add(sourceMaterials.get(materials.size() % sourceMaterials.size()));
        }
        return materials;
    }

    private List<TrainingQuestion> orientationQuestions(UUID sessionId, LocalDate today) {
        List<TrainingQuestion> questions = new ArrayList<>();
        int number = 1;
        questions.add(TrainingQuestion.choice(
                sessionId, number++, QuestionType.ORIENTATION, QuestionKind.ORIENTATION_DATE,
                "오늘은 며칠인가요?", null, null, formatDate(today), 0, null,
                dateOptions(today)));
        questions.add(TrainingQuestion.choice(
                sessionId, number++, QuestionType.ORIENTATION, QuestionKind.ORIENTATION_WEEKDAY,
                "오늘은 무슨 요일인가요?", null, null,
                today.getDayOfWeek().getDisplayName(TextStyle.FULL, KOREAN), 0, null,
                weekdayOptions(today)));
        questions.add(TrainingQuestion.choice(
                sessionId, number, QuestionType.ORIENTATION, QuestionKind.ORIENTATION_YEAR,
                "올해는 몇 년도인가요?", null, null, String.valueOf(today.getYear()), 0, null,
                yearOptions(today.getYear(), 4, 0)));
        return questions;
    }

    private List<TrainingQuestion> recallQuestions(UUID sessionId, List<TrainingMaterial> materials,
                                                   DifficultyLevel difficulty) {
        List<TrainingQuestion> questions = new ArrayList<>();
        int start = policy.orientationQuestionCount() + 1;
        for (int index = 0; index < policy.recallQuestionCount(); index++) {
            TrainingMaterial material = materials.get(index);
            if (material.year() != null) {
                questions.add(TrainingQuestion.choice(
                        sessionId, start + index, QuestionType.RECALL, QuestionKind.RECALL_YEAR,
                        "이 사진은 몇 년도쯤의 추억일까요?", material.imageKey(), material,
                        String.valueOf(material.year()), difficulty.yearTolerance(), hint(difficulty),
                        yearOptions(material.year(), difficulty.choiceCount(), difficulty.yearTolerance())));
            } else {
                questions.add(TrainingQuestion.choice(
                        sessionId, start + index, QuestionType.RECALL, QuestionKind.RECALL_TITLE,
                        "이 사진과 가장 잘 어울리는 추억은 무엇인가요?", material.imageKey(), material,
                        materialAnswerKey(material), 0, hint(difficulty), choiceOptions(material.title(), difficulty.choiceCount())));
            }
        }
        return questions;
    }

    private List<TrainingQuestion> languageQuestions(UUID sessionId, List<TrainingMaterial> materials,
                                                     DifficultyLevel difficulty) {
        List<TrainingQuestion> questions = new ArrayList<>();
        int start = policy.orientationQuestionCount() + policy.recallQuestionCount() + 1;
        for (int index = 0; index < policy.languageQuestionCount(); index++) {
            TrainingMaterial material = materials.get(index % materials.size());
            QuestionKind kind = index == 0 ? QuestionKind.LANGUAGE_NAMING : QuestionKind.LANGUAGE_DESCRIPTION;
            String prompt = index == 0
                    ? "사진을 보고 떠오르는 이름을 말씀해 주세요."
                    : "이 사진에 대해 짧게 이야기해 주세요.";
            questions.add(TrainingQuestion.textOrVoice(
                    sessionId, start + index, QuestionType.LANGUAGE, kind, prompt,
                    material.imageKey(), material, materialAnswerKey(material), hint(difficulty)));
        }
        return questions;
    }

    private List<TrainingQuestion> delayedRecallQuestions(UUID sessionId, List<TrainingMaterial> materials,
                                                           DifficultyLevel difficulty) {
        List<TrainingQuestion> questions = new ArrayList<>();
        int start = policy.orientationQuestionCount() + policy.recallQuestionCount() + policy.languageQuestionCount() + 1;
        for (int index = 0; index < policy.delayedRecallQuestionCount(); index++) {
            TrainingMaterial material = materials.get(index % materials.size());
            questions.add(TrainingQuestion.choice(
                    sessionId, start + index, QuestionType.DELAYED_RECALL, QuestionKind.DELAYED_RECALL,
                    "아까 보신 사진을 떠올려 보세요. 어떤 추억이었을까요?", material.imageKey(), material,
                    materialAnswerKey(material), 0, hint(difficulty), choiceOptions(material.title(), difficulty.choiceCount())));
        }
        return questions;
    }

    private DifficultyLevel difficultyOf(UUID elderId, QuestionType type) {
        return difficultyRepository.findByElderIdAndQuestionType(elderId, type)
                .map(TrainingDifficulty::getLevel)
                .orElse(DifficultyLevel.LEVEL_1);
    }

    private List<String> dateOptions(LocalDate today) {
        return List.of(today, today.minusDays(1), today.plusDays(1), today.plusDays(2)).stream()
                .map(this::formatDate)
                .toList();
    }

    private String formatDate(LocalDate date) {
        return date.getMonthValue() + "월 " + date.getDayOfMonth() + "일";
    }

    private List<String> weekdayOptions(LocalDate today) {
        return List.of(today, today.plusDays(1), today.plusDays(2), today.plusDays(3)).stream()
                .map(date -> date.getDayOfWeek().getDisplayName(TextStyle.FULL, KOREAN))
                .toList();
    }

    private List<String> yearOptions(int year, int count, int tolerance) {
        List<String> options = new ArrayList<>();
        options.add(String.valueOf(year));
        int minimumOffset = tolerance + 1;
        int[] offsets = {-minimumOffset, minimumOffset, -2 * minimumOffset, 2 * minimumOffset};
        for (int offset : offsets) {
            if (options.size() == count) break;
            options.add(String.valueOf(year + offset));
        }
        return options;
    }

    private List<String> choiceOptions(String answer, int count) {
        List<String> options = new ArrayList<>(List.of(answer, "가족과 함께한 시간", "정겨운 물건", "즐거운 여행", "계절 풍경"));
        List<String> distinct = new ArrayList<>(options.stream().distinct().toList());
        while (distinct.size() < count) {
            distinct.add("다른 추억 " + (distinct.size() + 1));
        }
        return distinct.stream().limit(count).toList();
    }

    private String materialAnswerKey(TrainingMaterial material) {
        List<String> keywords = new ArrayList<>();
        keywords.add(material.title());
        keywords.addAll(material.keywords());
        return keywords.stream()
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.joining("\u001F"));
    }

    private String hint(DifficultyLevel difficulty) {
        return difficulty.hintProvided() ? "사진을 천천히 다시 살펴보세요." : null;
    }
}
