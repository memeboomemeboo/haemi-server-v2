package com.memeboo2.haemi.architecture;

import com.memeboo2.haemi.common.security.ElderAccessChecked;
import com.memeboo2.haemi.guardian.api.CareAccessQuery;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureTest {

    private static final String BASE_PACKAGE = "com.memeboo2.haemi";
    private static final String APPLICATION = "..application..";
    private static final String DOMAIN = "..domain..";
    private static final String PRESENTATION = "..presentation..";
    private static final String PRESENTATION_DTO = "..presentation.dto..";
    private static final String ELDER_PRESENTATION_DTO = "..elder..presentation.dto..";
    private static final String INFRASTRUCTURE = "..infrastructure..";

    static JavaClasses classes;

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    /** AU-1: 어르신 사용자 ID를 받는 공개 유스케이스는 접근 검증 마커를 남긴다. */
    @Test
    void AU1_어르신_UUID를_받는_유스케이스는_접근검증을_표시한다() {
        var unguardedMethods = classes.stream()
                .filter(ArchitectureTest::isElderUseCaseBoundary)
                .flatMap(javaClass -> javaClass.getMethods().stream())
                .filter(method -> method.getModifiers().contains(JavaModifier.PUBLIC))
                .filter(method -> method.getRawParameterTypes().stream()
                        .anyMatch(type -> type.isEquivalentTo(UUID.class)))
                .filter(method -> !method.isAnnotatedWith(ElderAccessChecked.class))
                .map(JavaMethod::getFullName)
                .toList();

        assertThat(unguardedMethods)
                .as("elder application의 UUID 입력 유스케이스는 @ElderAccessChecked를 가져야 합니다")
                .isEmpty();
    }

    /** AU-1 보강: 인증된 사용자 유스케이스 외의 내부 서비스에는 접근 검증 마커를 붙이지 않는다. */
    @Test
    void AU1_접근검증_마커는_사용자_유스케이스_경계에만_쓴다() {
        var invalidMarkerOwners = classes.stream()
                .filter(javaClass -> !javaClass.isInterface())
                .filter(javaClass -> javaClass.getMethods().stream()
                        .anyMatch(method -> method.isAnnotatedWith(ElderAccessChecked.class)))
                .filter(javaClass -> !isElderUseCaseBoundary(javaClass))
                .map(JavaClass::getName)
                .toList();

        assertThat(invalidMarkerOwners)
                .as("@ElderAccessChecked는 인증 사용자 경계인 elder UseCase에만 붙일 수 있습니다")
                .isEmpty();
    }

    /** AU-1 보강: 마커를 구현하는 사용자 유스케이스는 실제 인가 포트에 의존한다. */
    @Test
    void AU1_접근검증_유스케이스는_CareAccessQuery에_의존한다() {
        var markerWithoutAccessPort = classes.stream()
                .filter(javaClass -> !javaClass.isInterface())
                .filter(ArchitectureTest::isElderUseCaseBoundary)
                .filter(javaClass -> javaClass.getMethods().stream()
                        .anyMatch(method -> method.isAnnotatedWith(ElderAccessChecked.class)))
                .filter(javaClass -> javaClass.getDirectDependenciesFromSelf().stream()
                        .noneMatch(dependency -> dependency.getTargetClass().isEquivalentTo(CareAccessQuery.class)))
                .map(JavaClass::getName)
                .toList();

        assertThat(markerWithoutAccessPort)
                .as("@ElderAccessChecked 유스케이스는 CareAccessQuery에 의존해야 합니다")
                .isEmpty();
    }

    /**
     * AU-1 보강(메서드 레벨): 각 @ElderAccessChecked 메서드는 본문(또는 같은 클래스의 헬퍼)에서
     * 실제로 CareAccessQuery를 호출해야 한다. 클래스 레벨 의존만 보면, 다른 메서드가 인가를 호출하는
     * 클래스에 인가를 빠뜨린 메서드를 하나 추가해도 게이트를 통과한다 — 애노테이션이 "검증했다"고
     * 주장만 하고 아무도 검증하지 않는 사각(#136)을 이 규칙이 막는다.
     */
    @Test
    void AU1_접근검증_메서드는_본문에서_CareAccessQuery를_호출한다() {
        var methodsWithoutAccessCall = classes.stream()
                .filter(javaClass -> !javaClass.isInterface())
                .filter(ArchitectureTest::isElderUseCaseBoundary)
                .flatMap(javaClass -> javaClass.getMethods().stream()
                        .filter(method -> method.isAnnotatedWith(ElderAccessChecked.class))
                        .filter(method -> !invokesCareAccessQuery(method, javaClass, new HashSet<>())))
                .map(JavaMethod::getFullName)
                .toList();

        assertThat(methodsWithoutAccessCall)
                .as("@ElderAccessChecked 메서드는 본문(또는 같은 클래스 헬퍼)에서 CareAccessQuery를 호출해야 합니다")
                .isEmpty();
    }

    /** AU-2: 어르신 DTO에 점수·정답률·등수·레벨을 유추할 수 있는 필드명을 두지 않는다. */
    @Test
    void AU2_어르신DTO에_측정값_필드명_금지() {
        noFields()
                .that().areDeclaredInClassesThat().resideInAPackage(ELDER_PRESENTATION_DTO)
                .should().haveNameMatching("(?:score|correct|rate|accuracy|rank|level|percent|ratio|ranking)(?:$|[A-Z_].*)|.*[A-Z](?:Score|Correct|Rate|Accuracy|Rank|Level|Percent|Ratio|Ranking)(?:$|[A-Z_].*)")
                .allowEmptyShould(true)
                .because("어르신 DTO에는 점수·정답률·순위·레벨 관련 필드명을 사용할 수 없습니다.")
                .check(classes);
    }

    /** AU-3: elder는 guardian.api 외 guardian 내부 구현에 의존할 수 없다. */
    @Test
    void AU3_elder는_guardian_api_외_import_금지() {
        noClasses()
                .that().resideInAPackage("..elder..")
                .should(dependOnInternalTypeOf("guardian"))
                .allowEmptyShould(true)
                .because("elder 그룹은 guardian.api 패키지만 접근할 수 있습니다.")
                .check(classes);
    }

    /** AU-3 보강: guardian도 elder 내부 구현을 직접 참조하지 않고 이벤트·계약으로 통신한다. */
    @Test
    void AU3_guardian은_elder_내부구현_import_금지() {
        noClasses()
                .that().resideInAPackage("..guardian..")
                .should(dependOnInternalTypeOf("elder"))
                .allowEmptyShould(true)
                .because("guardian은 elder의 내부 구현이 아니라 공통 이벤트와 계약만 사용해야 합니다.")
                .check(classes);
    }

    @Test
    void 레이어는_역방향으로_의존하지_않는다() {
        noClasses()
                .that().resideInAPackage(APPLICATION)
                .should().dependOnClassesThat().resideInAPackage(PRESENTATION)
                .because("application은 HTTP DTO나 컨트롤러를 알면 안 됩니다.")
                .check(classes);

        noClasses()
                .that().resideInAPackage(DOMAIN)
                .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, PRESENTATION, INFRASTRUCTURE)
                .because("domain은 application·presentation·infrastructure 역방향 의존을 만들면 안 됩니다.")
                .check(classes);
    }

    @Test
    void domain은_web과_servlet에_의존하지_않는다() {
        // 이 코드베이스의 domain은 현재 JPA 엔티티를 직접 소유한다. JPA 분리는 별도 대규모 영속성 모델 전환 과제다.
        noClasses()
                .that().resideInAPackage(DOMAIN)
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework.web..", "jakarta.servlet..")
                .because("domain은 HTTP·Servlet 기술 세부사항에 의존하면 안 됩니다.")
                .check(classes);
    }

    @Test
    void Controller는_Repository를_직접_의존하지_않는다() {
        noClasses()
                .that().areAnnotatedWith(RestController.class)
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
                .because("Controller는 application을 통해 유스케이스를 호출해야 합니다.")
                .check(classes);
    }

    @Test
    void Entity는_presentation_DTO를_노출하지_않는다() {
        noClasses()
                .that().areAnnotatedWith(Entity.class)
                .should().dependOnClassesThat().resideInAPackage(PRESENTATION_DTO)
                .because("영속 엔티티는 HTTP 응답 DTO를 반환하거나 필드로 노출하면 안 됩니다.")
                .check(classes);
    }

    @Test
    void 트랜잭션은_application_계층에만_둔다() {
        var invalidTransactionalMethods = classes.stream()
                .flatMap(javaClass -> javaClass.getMethods().stream())
                .filter(method -> method.isAnnotatedWith(Transactional.class))
                .filter(method -> !method.getOwner().getPackageName().contains(".application"))
                .map(JavaMethod::getFullName)
                .toList();

        var invalidTransactionalClasses = classes.stream()
                .filter(javaClass -> javaClass.isAnnotatedWith(Transactional.class))
                .filter(javaClass -> !javaClass.getPackageName().contains(".application"))
                .map(JavaClass::getName)
                .toList();

        assertThat(invalidTransactionalMethods)
                .as("@Transactional 메서드는 application 계층에만 둘 수 있습니다")
                .isEmpty();
        assertThat(invalidTransactionalClasses)
                .as("@Transactional 클래스는 application 계층에만 둘 수 있습니다")
                .isEmpty();
    }

    @Test
    void 조회성_트랜잭션은_readOnly를_명시한다() {
        var writableReadMethods = classes.stream()
                .flatMap(javaClass -> javaClass.getMethods().stream())
                .filter(method -> method.isAnnotatedWith(Transactional.class))
                .filter(method -> method.getName().matches("^(get|find|list|exists|load|read).*"))
                .filter(method -> !method.getAnnotationOfType(Transactional.class).readOnly())
                .map(JavaMethod::getFullName)
                .toList();

        assertThat(writableReadMethods)
                .as("조회 목적의 트랜잭션은 @Transactional(readOnly = true)를 명시해야 합니다")
                .isEmpty();
    }

    @Test
    void 컴포넌트는_역할이_드러나는_이름을_쓴다() {
        var invalidControllerNames = classes.stream()
                .filter(javaClass -> javaClass.isAnnotatedWith(RestController.class))
                .filter(javaClass -> !javaClass.getSimpleName().endsWith("Controller"))
                .map(JavaClass::getName)
                .toList();
        var invalidServiceNames = classes.stream()
                .filter(javaClass -> javaClass.isAnnotatedWith(Service.class))
                .filter(javaClass -> javaClass.getPackageName().contains(".application"))
                .filter(javaClass -> !javaClass.getSimpleName().matches(
                        ".*(Service|UseCase|QueryImpl|CommandImpl|Saver|Recorder|Creator|Counter|Maintenance|Resolver|Generator|Batch|Replayer)$"))
                .map(JavaClass::getName)
                .toList();
        var invalidRepositoryNames = classes.stream()
                .filter(javaClass -> javaClass.isAssignableTo(Repository.class))
                .filter(javaClass -> !javaClass.getSimpleName().endsWith("Repository"))
                .map(JavaClass::getName)
                .toList();
        var invalidDtoNames = classes.stream()
                .filter(javaClass -> javaClass.getPackageName().contains(".presentation.dto"))
                .filter(javaClass -> !javaClass.getName().contains("$"))
                .filter(javaClass -> !javaClass.getSimpleName().matches(".*(Request|Response|Item|Summary|Detail)$"))
                .map(JavaClass::getName)
                .toList();

        assertThat(invalidControllerNames).as("REST 컨트롤러는 *Controller로 끝나야 합니다").isEmpty();
        assertThat(invalidServiceNames).as("application 서비스는 역할 접미사를 가져야 합니다").isEmpty();
        assertThat(invalidRepositoryNames).as("Spring Data 저장소는 *Repository로 끝나야 합니다").isEmpty();
        assertThat(invalidDtoNames).as("HTTP DTO는 Request/Response 또는 응답 구성 요소 접미사를 가져야 합니다").isEmpty();
    }

    @Test
    void 모듈_최상위_슬라이스는_순환참조하지_않는다() {
        slices()
                .matching("com.memeboo2.haemi.(*)..")
                .should().beFreeOfCycles()
                .because("최상위 모듈 간 순환참조는 Modulith 경계를 무너뜨립니다.")
                .check(classes);
    }

    @Test
    void RestController_경로는_api_v1_하위다() {
        var invalidMappings = classes.stream()
                .filter(javaClass -> javaClass.isAnnotatedWith(RestController.class))
                .flatMap(ArchitectureTest::requestMappingsOf)
                .filter(mapping -> !mapping.path().equals("/api/v1") && !mapping.path().startsWith("/api/v1/"))
                .map(ControllerMapping::description)
                .toList();

        assertThat(invalidMappings)
                .as("@RestController의 HTTP 경로는 /api/v1 하위여야 합니다")
                .isEmpty();
    }

    @Test
    void 일반_RuntimeException을_직접_생성하지_않고_출력_문을_사용하지_않는다() {
        noClasses()
                .should().callConstructor(RuntimeException.class)
                .because("예외는 DomainException 등 공통 오류 정책을 통해 표현해야 합니다.")
                .check(classes);
        noClasses()
                .should().accessField(System.class, "out")
                .because("표준 출력 대신 구조화된 로거를 사용해야 합니다.")
                .check(classes);
        noClasses()
                .should().accessField(System.class, "err")
                .because("표준 오류 대신 구조화된 로거를 사용해야 합니다.")
                .check(classes);
        noClasses()
                .should().callMethod(Throwable.class, "printStackTrace")
                .because("printStackTrace 대신 예외 맥락을 포함해 로깅해야 합니다.")
                .check(classes);
    }

    private static Stream<ControllerMapping> requestMappingsOf(JavaClass javaClass) {
        Class<?> controller = javaClass.reflect();
        List<String> classPaths = mappingPaths(controller);

        return Arrays.stream(controller.getDeclaredMethods())
                .filter(method -> requestMapping(method) != null)
                .flatMap(method -> mappingPaths(method).stream()
                        .flatMap(methodPath -> classPaths.stream()
                                .map(classPath -> new ControllerMapping(
                                        controller.getSimpleName(), method.getName(), joinPath(classPath, methodPath)))));
    }

    /**
     * {@code method}가 CareAccessQuery의 메서드를 직접 호출하거나, 같은 클래스의 헬퍼를 통해
     * 도달하는지 검사한다. {@code requireElderId} 같은 사설 헬퍼로 인가를 감싼 경우도 인정한다.
     */
    private static boolean invokesCareAccessQuery(JavaMethod method, JavaClass owner, Set<String> visited) {
        if (!visited.add(method.getFullName())) {
            return false;
        }
        for (JavaMethodCall call : method.getMethodCallsFromSelf()) {
            JavaClass targetOwner = call.getTargetOwner();
            if (targetOwner.isEquivalentTo(CareAccessQuery.class)) {
                return true;
            }
            if (targetOwner.getName().equals(owner.getName())) {
                String targetName = call.getTarget().getName();
                boolean reached = owner.getMethods().stream()
                        .filter(candidate -> candidate.getName().equals(targetName))
                        .anyMatch(candidate -> invokesCareAccessQuery(candidate, owner, visited));
                if (reached) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isElderUseCaseBoundary(JavaClass javaClass) {
        return javaClass.getPackageName().matches("com\\.memeboo2\\.haemi\\.elder\\..*\\.application")
                && (javaClass.getSimpleName().endsWith("UseCase")
                || javaClass.getInterfaces().stream().anyMatch(type -> type.getName().endsWith("UseCase")));
    }

    private static RequestMapping requestMapping(AnnotatedElement element) {
        return AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
    }

    private static List<String> mappingPaths(AnnotatedElement element) {
        RequestMapping mapping = requestMapping(element);
        if (mapping == null) {
            return List.of("");
        }
        String[] paths = mapping.path().length == 0 ? mapping.value() : mapping.path();
        return paths.length == 0 ? List.of("") : Arrays.asList(paths);
    }

    private static String joinPath(String base, String path) {
        String normalizedBase = base.isBlank() ? "" : (base.startsWith("/") ? base : "/" + base);
        String normalizedPath = path.isBlank() ? "" : (path.startsWith("/") ? path : "/" + path);
        if (normalizedBase.isEmpty()) {
            return normalizedPath;
        }
        if (normalizedPath.isEmpty()) {
            return normalizedBase;
        }
        return normalizedBase + normalizedPath;
    }

    private static ArchCondition<JavaClass> dependOnInternalTypeOf(String module) {
        DescribedPredicate<JavaClass> internalType = new DescribedPredicate<>(module + " 내부 타입") {
            @Override
            public boolean test(JavaClass javaClass) {
                String packageName = javaClass.getPackageName();
                return packageName.startsWith(BASE_PACKAGE + "." + module)
                        && !packageName.equals(BASE_PACKAGE + "." + module + ".api")
                        && !packageName.startsWith(BASE_PACKAGE + "." + module + ".api.");
            }
        };

        return new ArchCondition<>(module + " 내부 타입에 의존한다") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getDirectDependenciesFromSelf().stream()
                        .filter(dependency -> internalType.test(dependency.getTargetClass()))
                        .forEach(dependency -> events.add(SimpleConditionEvent.violated(item,
                                item.getName() + " -> " + dependency.getTargetClass().getName())));
            }
        };
    }

    private record ControllerMapping(String controller, String method, String path) {
        String description() {
            return controller + "#" + method + " -> " + path;
        }
    }
}
