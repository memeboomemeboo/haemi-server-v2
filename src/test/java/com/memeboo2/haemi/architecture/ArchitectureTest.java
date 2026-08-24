package com.memeboo2.haemi.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.memeboo2.haemi.common.security.ElderAccessChecked;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static org.assertj.core.api.Assertions.assertThat;

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
        noFields()
            .that().areDeclaredInClassesThat().resideInAPackage("..elder.presentation.dto..")
            .should().haveNameMatching("(?i).*(score|correct|rate|accuracy|rank|level).*")
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
            .should(dependOnGuardianInternalType())
            .allowEmptyShould(true)
            .because("elder 그룹은 guardian.api 패키지만 접근할 수 있습니다.")
            .check(classes);
    }

    /** AU-1: 어르신 사용자 ID를 받는 유스케이스는 명시적으로 본인 접근 검증을 남긴다. */
    @Test
    void AU1_어르신_UUID를_받는_유스케이스는_접근검증을_표시한다() {
        var unguardedMethods = classes.stream()
                .filter(javaClass -> javaClass.getPackageName().matches("com\\.memeboo2\\.haemi\\.elder\\..*\\.application"))
                .filter(javaClass -> !javaClass.getSimpleName().endsWith("QueryImpl"))
                .flatMap(javaClass -> javaClass.getMethods().stream())
                .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                .filter(method -> method.getRawParameterTypes().stream()
                        .anyMatch(type -> type.isEquivalentTo(UUID.class)))
                .filter(method -> !method.isAnnotatedWith(ElderAccessChecked.class))
                .map(JavaMethod::getFullName)
                .toList();

        assertThat(unguardedMethods)
                .as("elder application의 UUID 입력 유스케이스는 @ElderAccessChecked와 CareAccessQuery 검증을 가져야 합니다")
                .isEmpty();
    }

    private static ArchCondition<JavaClass> dependOnGuardianInternalType() {
        DescribedPredicate<JavaClass> guardianInternal = new DescribedPredicate<>("guardian.api 외 guardian 타입") {
            @Override
            public boolean test(JavaClass javaClass) {
                String packageName = javaClass.getPackageName();
                return packageName.startsWith("com.memeboo2.haemi.guardian")
                        && !packageName.equals("com.memeboo2.haemi.guardian.api")
                        && !packageName.startsWith("com.memeboo2.haemi.guardian.api.");
            }
        };

        return new ArchCondition<>("guardian.api 외 guardian 타입에 의존한다") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().stream()
                        .filter(dependency -> guardianInternal.test(dependency.getTargetClass()))
                        .forEach(dependency -> events.add(SimpleConditionEvent.violated(item,
                                item.getName() + " -> " + dependency.getTargetClass().getName())));
            }
        };
    }
}
