package com.memeboo2.haemi.elder.training.infrastructure;

import com.memeboo2.haemi.elder.training.domain.TrainingQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrainingQuestionRepository extends JpaRepository<TrainingQuestion, UUID> {

    List<TrainingQuestion> findBySessionIdOrderByQuestionNumberAsc(UUID sessionId);

    Optional<TrainingQuestion> findBySessionIdAndQuestionNumber(UUID sessionId, int questionNumber);
}
