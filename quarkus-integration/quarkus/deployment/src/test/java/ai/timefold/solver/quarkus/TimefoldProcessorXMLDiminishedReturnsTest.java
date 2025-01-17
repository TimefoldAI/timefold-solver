package ai.timefold.solver.quarkus;

class TimefoldProcessorXMLDiminishedReturnsTest {
    // Quarkus test framework is missing a function to test a startup exception,
    // see https://github.com/quarkusio/quarkus/issues/45669
    //    @RegisterExtension
    //    static final QuarkusUnitTest config = new QuarkusUnitTest()
    //            .overrideRuntimeConfigKey("quarkus.timefold.solver.termination.diminished-returns.enabled", "true")
    //            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
    //                    .addAsResource("ai/timefold/solver/quarkus/customSolverConfigWithPhaseList.xml",
    //                            "solverConfig.xml")
    //                    .addClasses(TestdataQuarkusEntity.class,
    //                            TestdataQuarkusSolution.class,
    //                            TestdataQuarkusConstraintProvider.class))
    //            .setExpectedException(IllegalArgumentException.class);
    //
    //    @Inject
    //    SolverConfig solverConfig;
    //
    //    @Test
    //    void solverConfigXml_default() {
    //        Assertions.fail();
    //    }
}
