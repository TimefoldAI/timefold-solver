
package ai.timefold.solver.examples.conferencescheduling.persistence;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.OpenDataFilesTest;
import ai.timefold.solver.examples.conferencescheduling.app.ConferenceSchedulingApp;
import ai.timefold.solver.examples.conferencescheduling.domain.ConferenceSolution;

class ConferenceSchedulingOpenDataFilesTest extends OpenDataFilesTest<ConferenceSolution> {

    @Override
    protected CommonApp<ConferenceSolution> createCommonApp() {
        return new ConferenceSchedulingApp();
    }
}
