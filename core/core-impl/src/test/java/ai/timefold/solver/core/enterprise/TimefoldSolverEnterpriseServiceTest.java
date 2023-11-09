package ai.timefold.solver.core.enterprise;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class TimefoldSolverEnterpriseServiceTest {

    @Test
    void failsOnLoad() {
        Assertions.assertThat(TimefoldSolverEnterpriseService.load())
                .isNull();
    }

    @Test
    void banner() {
        Assertions.assertThat(TimefoldSolverEnterpriseService.getBanner())
                .isEqualTo("""
                             ____         _______
                            |    |       /      /
                          __|    |______/______/   _     _                       __           _       _
                         /             /          | |_  (_)  _ __ ___     ___   / _|   ___   | |   __| |
                        /___      ____/_______    | __| | | | '_ ` _ \\   / _ \\ | |_   / _ \\  | |  / _` |
                            |    |    /      /    | |_  | | | | | | | | |  __/ |  _| | (_) | | | | (_| |
                            |    |___/______/      \\__| |_| |_| |_| |_|  \\___| |_|    \\___/  |_|  \\__,_|
                            |       /
                            |______/            Timefold Solver Community Edition (Development snapshot)
                        """);
    }

}
