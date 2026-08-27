package com.memeboo2.haemi.elder.training;

import com.memeboo2.haemi.elder.training.domain.MaterialSource;
import com.memeboo2.haemi.elder.training.domain.TrainingMaterial;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TrainingMaterialTest {

    @Test
    void 레코드_필드가_올바르게_매핑된다() {
        UUID id = UUID.randomUUID();
        TrainingMaterial m = new TrainingMaterial(
                id, MaterialSource.MEMORY, "가족사진", "images/family.jpg", 2020, List.of("가족", "여행"));

        assertThat(m.id()).isEqualTo(id);
        assertThat(m.source()).isEqualTo(MaterialSource.MEMORY);
        assertThat(m.title()).isEqualTo("가족사진");
        assertThat(m.imageKey()).isEqualTo("images/family.jpg");
        assertThat(m.year()).isEqualTo(2020);
        assertThat(m.keywords()).containsExactly("가족", "여행");
    }

    @Test
    void null_필드도_허용된다() {
        TrainingMaterial m = new TrainingMaterial(UUID.randomUUID(), MaterialSource.CONTENT, "제목", null, null, null);

        assertThat(m.imageKey()).isNull();
        assertThat(m.year()).isNull();
        assertThat(m.keywords()).isNull();
    }

    @Test
    void 동일_필드면_equals가_true이다() {
        UUID id = UUID.randomUUID();
        TrainingMaterial m1 = new TrainingMaterial(id, MaterialSource.MEMORY, "t", "k", 2020, List.of());
        TrainingMaterial m2 = new TrainingMaterial(id, MaterialSource.MEMORY, "t", "k", 2020, List.of());

        assertThat(m1).isEqualTo(m2);
    }
}
