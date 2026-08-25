package com.memeboo2.haemi.elder.training.application;

import com.memeboo2.haemi.elder.training.domain.MaterialSource;
import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.TrainingQuestion;
import com.memeboo2.haemi.elder.training.domain.TrainingSession;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingDifficultyRepository;
import com.memeboo2.haemi.elder.training.infrastructure.TrainingQuestionRepository;
import com.memeboo2.haemi.guardian.api.MemoryQuery;
import com.memeboo2.haemi.platform.content.api.ContentMaterial;
import com.memeboo2.haemi.platform.content.api.ContentQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/** CIST-TRN-002~005의 문항 순서와 앨범 우선 선택을 고정한다. */
@ExtendWith(MockitoExtension.class)
class TrainingQuestionGenerationServiceTest {

    private static final UUID ELDER_ID = UUID.randomUUID();
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 25);

    @Mock TrainingQuestionRepository questionRepository;
    @Mock TrainingDifficultyRepository difficultyRepository;
    @Mock MemoryQuery memoryQuery;
    @Mock ContentQuery contentQuery;

    private TrainingQuestionGenerationService generator;

    @BeforeEach
    void setUp() {
        generator = new TrainingQuestionGenerationService(
                questionRepository,
                difficultyRepository,
                memoryQuery,
                contentQuery,
                new TrainingPolicyProperties(3, 3, 2, 2, 90, 0.8, 0.4, 2));
        given(questionRepository.findBySessionIdOrderByQuestionNumberAsc(org.mockito.ArgumentMatchers.any()))
                .willReturn(List.of());
        given(difficultyRepository.findByElderIdAndQuestionType(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .willReturn(Optional.empty());
        given(questionRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void 앨범_사진이_충분하면_콘텐츠_풀_대신_앨범을_사용해_10문항을_고정한다() {
        given(memoryQuery.materialsFor(ELDER_ID, 3)).willReturn(List.of(
                memory("첫 번째 추억", 1970), memory("두 번째 추억", 1975), memory("세 번째 추억", 1980)));
        TrainingSession session = TrainingSession.start(ELDER_ID, Instant.parse("2026-08-25T00:00:00Z"), TODAY);

        List<TrainingQuestion> questions = generator.generateIfAbsent(session, ELDER_ID, 75, TODAY);

        assertThat(questions).hasSize(10);
        assertThat(questions).extracting(TrainingQuestion::getQuestionType).containsExactly(
                QuestionType.ORIENTATION, QuestionType.ORIENTATION, QuestionType.ORIENTATION,
                QuestionType.RECALL, QuestionType.RECALL, QuestionType.RECALL,
                QuestionType.LANGUAGE, QuestionType.LANGUAGE,
                QuestionType.DELAYED_RECALL, QuestionType.DELAYED_RECALL);
        assertThat(questions.subList(3, 10)).allSatisfy(question ->
                assertThat(question.getMaterialSource()).isEqualTo(MaterialSource.MEMORY));
        assertThat(questions.subList(8, 10)).extracting(TrainingQuestion::getMaterialId)
                .allMatch(questions.subList(3, 6).stream().map(TrainingQuestion::getMaterialId).toList()::contains);
        assertThat(questions.subList(0, 3)).allSatisfy(question -> assertThat(question.getOptions()).hasSize(4));
        then(contentQuery).shouldHaveNoInteractions();
        then(difficultyRepository).should(never())
                .findByElderIdAndQuestionType(ELDER_ID, QuestionType.LANGUAGE);
    }

    @Test
    void 앨범이_부족하면_필요한_수만큼_큐레이션_콘텐츠로_보완한다() {
        given(memoryQuery.materialsFor(ELDER_ID, 3)).willReturn(List.of(memory("앨범 추억", 1970)));
        given(contentQuery.selectForTraining(eq(ELDER_ID), eq(75), eq(2), anySet())).willReturn(List.of(
                content("골목 풍경", 1968), content("가족 나들이", 1973)));
        TrainingSession session = TrainingSession.start(ELDER_ID, Instant.parse("2026-08-25T00:00:00Z"), TODAY);

        generator.generateIfAbsent(session, ELDER_ID, 75, TODAY);

        ArgumentCaptor<List<TrainingQuestion>> captor = ArgumentCaptor.forClass(List.class);
        then(questionRepository).should().saveAll(captor.capture());
        assertThat(captor.getValue().subList(3, 6)).extracting(TrainingQuestion::getMaterialSource)
                .containsExactly(MaterialSource.MEMORY, MaterialSource.CONTENT, MaterialSource.CONTENT);
    }

    @Test
    void 생년월일이_없으면_null_나이를_그대로_콘텐츠_선택기에_전달한다() {
        given(memoryQuery.materialsFor(ELDER_ID, 3)).willReturn(List.of());
        given(contentQuery.selectForTraining(eq(ELDER_ID), isNull(), eq(3), anySet())).willReturn(List.of(
                content("골목 풍경", 1968), content("가족 나들이", 1973), content("추석 저녁", 1978)));
        TrainingSession session = TrainingSession.start(ELDER_ID, Instant.parse("2026-08-25T00:00:00Z"), TODAY);

        List<TrainingQuestion> questions = generator.generateIfAbsent(session, ELDER_ID, null, TODAY);

        assertThat(questions).hasSize(10);
        then(contentQuery).should().selectForTraining(eq(ELDER_ID), isNull(), eq(3), anySet());
    }

    @Test
    void 이미지가_없는_추억은_건너뛰고_콘텐츠로_보완한다() {
        MemoryQuery.MemoryMaterial imageLessMemory = new MemoryQuery.MemoryMaterial(
                UUID.randomUUID(), "사진 없는 추억", 1970, List.of(), TODAY);
        given(memoryQuery.materialsFor(ELDER_ID, 3)).willReturn(List.of(imageLessMemory));
        given(contentQuery.selectForTraining(eq(ELDER_ID), eq(75), eq(3), anySet())).willReturn(List.of(
                content("골목 풍경", 1968), content("가족 나들이", 1973), content("추석 저녁", 1978)));
        TrainingSession session = TrainingSession.start(ELDER_ID, Instant.parse("2026-08-25T00:00:00Z"), TODAY);

        List<TrainingQuestion> questions = generator.generateIfAbsent(session, ELDER_ID, 75, TODAY);

        assertThat(questions.subList(3, 6)).extracting(TrainingQuestion::getMaterialSource)
                .containsOnly(MaterialSource.CONTENT);
    }

    @Test
    void 레벨1_연도_보기의_오답은_허용_오차보다_멀리_생성된다() {
        given(memoryQuery.materialsFor(ELDER_ID, 3)).willReturn(List.of(
                memory("첫 번째 추억", 1970), memory("두 번째 추억", 1975), memory("세 번째 추억", 1980)));
        TrainingSession session = TrainingSession.start(ELDER_ID, Instant.parse("2026-08-25T00:00:00Z"), TODAY);

        List<TrainingQuestion> questions = generator.generateIfAbsent(session, ELDER_ID, 75, TODAY);

        TrainingQuestion firstRecall = questions.get(3);
        int answerYear = Integer.parseInt(firstRecall.getAnswerKey());
        assertThat(firstRecall.getOptions())
                .filteredOn(option -> !option.equals(firstRecall.getAnswerKey()))
                .allSatisfy(option -> assertThat(Math.abs(Integer.parseInt(option) - answerYear)).isGreaterThan(10));
    }

    @Test
    void 재료가_부족하면_마지막_사진만_반복하지_않고_순환한다() {
        MemoryQuery.MemoryMaterial album = memory("앨범 추억", 1970);
        given(memoryQuery.materialsFor(ELDER_ID, 3)).willReturn(List.of(album));
        ContentMaterial curated = content("골목 풍경", 1968);
        given(contentQuery.selectForTraining(eq(ELDER_ID), eq(75), eq(2), anySet())).willReturn(List.of(curated));
        TrainingSession session = TrainingSession.start(ELDER_ID, Instant.parse("2026-08-25T00:00:00Z"), TODAY);

        List<TrainingQuestion> questions = generator.generateIfAbsent(session, ELDER_ID, 75, TODAY);

        assertThat(questions.subList(3, 6)).extracting(TrainingQuestion::getMaterialId)
                .containsExactly(album.id(), curated.id(), album.id());
    }

    private MemoryQuery.MemoryMaterial memory(String title, int year) {
        return new MemoryQuery.MemoryMaterial(
                UUID.randomUUID(), title, year, List.of("memory/" + title + ".jpg"), TODAY);
    }

    private ContentMaterial content(String title, int year) {
        return new ContentMaterial(
                UUID.randomUUID(), title, "content/" + title + ".jpg", year, List.of(title, "추억"));
    }
}
