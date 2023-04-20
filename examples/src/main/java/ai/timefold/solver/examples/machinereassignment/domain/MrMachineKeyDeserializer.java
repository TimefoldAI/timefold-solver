package ai.timefold.solver.examples.machinereassignment.domain;

import ai.timefold.solver.examples.common.persistence.jackson.AbstractKeyDeserializer;
import ai.timefold.solver.examples.machinereassignment.persistence.MachineReassignmentSolutionFileIO;

/**
 * @see MachineReassignmentSolutionFileIO
 */
final class MrMachineKeyDeserializer extends AbstractKeyDeserializer<MrMachine> {

    public MrMachineKeyDeserializer() {
        super(MrMachine.class);
    }

    @Override
    protected MrMachine createInstance(long id) {
        return new MrMachine(id);
    }
}
