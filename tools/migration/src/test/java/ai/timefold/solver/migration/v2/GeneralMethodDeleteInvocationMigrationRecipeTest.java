package ai.timefold.solver.migration.v2;

import static org.openrewrite.java.Assertions.java;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

@Execution(ExecutionMode.CONCURRENT)
class GeneralMethodDeleteInvocationMigrationRecipeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(new GeneralMethodDeleteInvocationMigrationRecipe())
                //                .typeValidationOptions(TypeValidation.builder().allowMissingType(ignore -> true).build())
                .parser(JavaParser.fromJavaVersion()
                        // We must add all old classes as stubs to the JavaTemplate
                        .dependsOn(
                                """
                                        package ai.timefold.solver.core.api.score.stream;
                                        public interface ConstraintStreamImplType {
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.domain.common;
                                        public interface DomainAccessType {
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream;
                                        public interface ConstraintFactory {
                                        }""",
                                """
                                        package ai.timefold.solver.core.config.score.director;
                                        import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
                                        import java.util.List;
                                        public interface ScoreDirectorFactoryConfig {
                                            ConstraintStreamImplType getConstraintStreamImplType();
                                            void setConstraintStreamImplType(ConstraintStreamImplType parameter);
                                            ScoreDirectorFactoryConfig withConstraintStreamImplType(ConstraintStreamImplType parameter);
                                            List<String> getScoreDrlList();
                                            void setScoreDrlList(List<String> parameter);
                                            ScoreDirectorFactoryConfig withScoreDrlList(List<String> scoreDrlList);
                                            ScoreDirectorFactoryConfig withScoreDrlList(String... scoreDrls);
                                        }""",
                                """
                                        package ai.timefold.solver.core.config.solver;
                                        import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
                                        import ai.timefold.solver.core.api.domain.common.DomainAccessType;
                                        public interface SolverConfig {
                                            SolverConfig withConstraintStreamImplType(ConstraintStreamImplType parameter);
                                            DomainAccessType getDomainAccessType();
                                            void setDomainAccessType(DomainAccessType parameter);
                                            SolverConfig withDomainAccessType(DomainAccessType parameter);
                                            DomainAccessType determineDomainAccessType();
                                        }""",
                                """
                                        package ai.timefold.solver.test.api.score.stream;
                                        import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
                                        public interface ConstraintVerifier {
                                            ConstraintVerifier withConstraintStreamImplType(ConstraintStreamImplType parameter);
                                        }""",
                                """
                                        package ai.timefold.solver.core.api.score.stream;
                                        import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
                                        public interface Constraint {
                                            String getConstraintPackage();
                                            String getConstraintId();
                                            ConstraintFactory getConstraintFactory();
                                        }"""));
    }

    @Test
    void removeConstraintStreamImplType() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
                        import ai.timefold.solver.core.config.solver.SolverConfig;
                        import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
                        import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;

                        public class Test {
                                ScoreDirectorFactoryConfig factoryConfig;
                                SolverConfig solverConfig;
                                ConstraintVerifier constraintVerifier;
                                public void test() {
                                    factoryConfig.getConstraintStreamImplType();
                                    ConstraintStreamImplType result = factoryConfig.getConstraintStreamImplType();
                                    factoryConfig.setConstraintStreamImplType(null);
                                    factoryConfig.withConstraintStreamImplType(null);
                                    solverConfig.withConstraintStreamImplType(null);
                                    constraintVerifier.withConstraintStreamImplType(null);
                                }
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
                        import ai.timefold.solver.core.config.solver.SolverConfig;
                        import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
                        import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;

                        public class Test {
                                ScoreDirectorFactoryConfig factoryConfig;
                                SolverConfig solverConfig;
                                ConstraintVerifier constraintVerifier;
                                public void test() {
                                }
                        }"""));
    }

    @Test
    void removeDroolsSupport() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
                        import java.util.List;

                        public class Test {
                                ScoreDirectorFactoryConfig factoryConfig;
                                public void test() {
                                    List<String> list;
                                    factoryConfig.getScoreDrlList();
                                    List<String> list2 = factoryConfig.getScoreDrlList();
                                    factoryConfig.setScoreDrlList(list);
                                    factoryConfig.withScoreDrlList(null);
                                    factoryConfig.withScoreDrlList("", "");
                                }
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
                        import java.util.List;

                        public class Test {
                                ScoreDirectorFactoryConfig factoryConfig;
                                public void test() {
                                    List<String> list;
                                }
                        }"""));
    }

    @Test
    void removeConstraintMethods() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.core.api.score.stream.Constraint;
                        import ai.timefold.solver.core.api.score.stream.ConstraintFactory;

                        public class Test {
                                Constraint constraint;
                                public void test() {
                                    constraint.getConstraintPackage();
                                    constraint.getConstraintId();
                                    constraint.getConstraintFactory();
                                    String pkg = constraint.getConstraintPackage();
                                    String id = constraint.getConstraintId();
                                    ConstraintFactory factory = constraint.getConstraintFactory();
                                }
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.core.api.score.stream.Constraint;
                        import ai.timefold.solver.core.api.score.stream.ConstraintFactory;

                        public class Test {
                                Constraint constraint;
                                public void test() {
                                }
                        }"""));
    }

    @Test
    void removeSolverConfigMethods() {
        rewriteRun(java(
                """
                        package timefold;

                        import ai.timefold.solver.core.config.solver.SolverConfig;
                        import ai.timefold.solver.core.api.domain.common.DomainAccessType;

                        public class Test {
                                SolverConfig solverConfig;
                                public void test() {
                                    solverConfig.withConstraintStreamImplType(null);
                                    solverConfig.getDomainAccessType();
                                    DomainAccessType type = solverConfig.getDomainAccessType();
                                    solverConfig.setDomainAccessType(null);
                                    solverConfig.withDomainAccessType(type);
                                    solverConfig.determineDomainAccessType();
                                    DomainAccessType type2 = solverConfig.determineDomainAccessType();
                                }
                        }""",
                """
                        package timefold;

                        import ai.timefold.solver.core.config.solver.SolverConfig;
                        import ai.timefold.solver.core.api.domain.common.DomainAccessType;

                        public class Test {
                                SolverConfig solverConfig;
                                public void test() {
                                }
                        }"""));
    }

}
