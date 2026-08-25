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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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

    private MemoryQuery.MemoryMaterial memory(String title, int year) {
        return new MemoryQuery.MemoryMaterial(
                UUID.randomUUID(), title, year, List.of("memory/" + title + ".jpg"), TODAY);
    }

    private ContentMaterial content(String title, int year) {
        return new ContentMaterial(
                UUID.randomUUID(), title, "content/" + title + ".jpg", year, List.of(title, "추억"));
    }
}
