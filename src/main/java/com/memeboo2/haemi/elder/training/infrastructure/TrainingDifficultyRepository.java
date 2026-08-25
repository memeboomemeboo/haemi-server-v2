package com.memeboo2.haemi.elder.training.infrastructure;

import com.memeboo2.haemi.elder.training.domain.QuestionType;
import com.memeboo2.haemi.elder.training.domain.TrainingDifficulty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TrainingDifficultyRepository extends JpaRepository<TrainingDifficulty, UUID> {

    Optional<TrainingDifficulty> findByElderIdAndQuestionType(UUID elderId, QuestionType questionType);
}
