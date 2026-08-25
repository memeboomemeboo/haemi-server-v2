package com.memeboo2.haemi.elder.training.infrastructure;

import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.TrainingAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface TrainingAnswerRepository extends JpaRepository<TrainingAnswer, UUID> {

    List<TrainingAnswer> findBySessionIdOrderByQuestionNumberAsc(UUID sessionId);

    @Query("""
            SELECT a FROM TrainingAnswer a
            WHERE a.sessionId = :sessionId AND a.questionType = :questionType
            ORDER BY a.questionNumber ASC
            """)
    List<TrainingAnswer> findBySessionIdAndQuestionType(UUID sessionId, QuestionType questionType);
}
