package com.memeboo2.haemi.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    static JavaClasses classes;

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter().importPackages("com.memeboo2.haemi");
    }

    /**
     * AU-2: elder/presentation/dto 필드명에 score, correct, rate, accuracy, rank, level 금지.
     */
    @Test
    void AU2_어르신DTO에_점수_정답률_필드명_금지() {
        noClasses()
            .that().resideInAPackage("..elder.presentation.dto..")
            .should().haveSimpleNameContaining("Score")
            .orShould().haveSimpleNameContaining("Correct")
            .orShould().haveSimpleNameContaining("Accuracy")
            .orShould().haveSimpleNameContaining("Rank")
            .orShould().haveSimpleNameContaining("Level")
            .allowEmptyShould(true)
            .because("어르신 DTO에는 점수·정답률·순위 관련 필드명을 사용할 수 없습니다.")
            .check(classes);
    }

    /**
     * AU-3: elder 그룹은 guardian.api 외 guardian 패키지를 import할 수 없다.
     */
    @Test
    void AU3_elder는_guardian_api_외_import_금지() {
        noClasses()
            .that().resideInAPackage("..elder..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..guardian.family..",
                "..guardian.eldermanagement..",
                "..guardian.memory..",
                "..guardian.dailycare..",
                "..guardian.report..",
                "..guardian.presentation.."
            )
            .allowEmptyShould(true)
            .because("elder 그룹은 guardian.api 패키지만 접근할 수 있습니다.")
            .check(classes);
    }
}
