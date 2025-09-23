package ai.timefold.solver.core.impl.solver.change;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.api.solver.change.ProblemChange;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishEntity;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishEntityGroup;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishSolution;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishValue;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishValueGroup;

import org.junit.jupiter.api.Test;

class DefaultProblemChangeDirectorTest {

    @Test
    void complexProblemChange_correctlyNotifiesScoreDirector() {
        var entityGroupOne = new TestdataLavishEntityGroup("entityGroupOne");
        var valueGroupOne = new TestdataLavishValueGroup("valueGroupOne");
        var addedEntity = new TestdataLavishEntity("newly added entity", entityGroupOne);
        var removedEntity = new TestdataLavishEntity("entity to remove", entityGroupOne);
        var addedFact = new TestdataLavishValue("newly added fact", valueGroupOne);
        var removedFact = new TestdataLavishValue("fact to remove", valueGroupOne);
        var changedEntity = new TestdataLavishEntity("changed entity", entityGroupOne);
        var changedEntityValue = new TestdataLavishValue("changed entity value", valueGroupOne);

        InnerScoreDirector<TestdataSolution, ?> scoreDirectorMock = mock(InnerScoreDirector.class);
        when(scoreDirectorMock.lookUpWorkingObject(removedEntity)).thenReturn(removedEntity);
        when(scoreDirectorMock.lookUpWorkingObject(changedEntity)).thenReturn(changedEntity);
        when(scoreDirectorMock.lookUpWorkingObject(removedFact)).thenReturn(removedFact);
        var defaultProblemChangeDirector = new DefaultProblemChangeDirector<>(scoreDirectorMock);

        ProblemChange<TestdataLavishSolution> problemChange = ((workingSolution, problemChangeDirector) -> {
            // Add an entity.
            problemChangeDirector.addEntity(addedEntity, workingSolution.getEntityList()::add);
            // Remove an entity.
            problemChangeDirector.removeEntity(removedEntity, workingSolution.getEntityList()::remove);
            // Change a planning variable.
            problemChangeDirector.changeVariable(changedEntity, TestdataLavishEntity.VALUE_FIELD,
                    testdataEntity -> testdataEntity.setValue(changedEntityValue));
            // Change a property
            problemChangeDirector.changeProblemProperty(changedEntity,
                    workingEntity -> workingEntity.setEntityGroup(null));
            // Add a problem fact.
            problemChangeDirector.addProblemFact(addedFact, workingSolution.getValueList()::add);
            // Remove a problem fact.
            problemChangeDirector.removeProblemFact(removedFact, workingSolution.getValueList()::remove);
        });

        var testdataSolution = TestdataLavishSolution.generateSolution();
        testdataSolution.getEntityList().add(removedEntity);
        testdataSolution.getEntityList().add(changedEntity);
        testdataSolution.getValueList().add(removedFact);
        testdataSolution.getValueList().add(changedEntityValue);

        problemChange.doChange(testdataSolution, defaultProblemChangeDirector);
        verify(scoreDirectorMock, times(1)).beforeEntityAdded(addedEntity);
        verify(scoreDirectorMock, times(1)).afterEntityAdded(addedEntity);

        verify(scoreDirectorMock, times(1)).beforeEntityRemoved(removedEntity);
        verify(scoreDirectorMock, times(1)).afterEntityRemoved(removedEntity);

        verify(scoreDirectorMock, times(1))
                .beforeVariableChanged(changedEntity, TestdataEntity.VALUE_FIELD);
        verify(scoreDirectorMock, times(1))
                .afterVariableChanged(changedEntity, TestdataEntity.VALUE_FIELD);

        verify(scoreDirectorMock, times(1)).beforeProblemPropertyChanged(changedEntity);
        verify(scoreDirectorMock, times(1)).afterProblemPropertyChanged(changedEntity);

        verify(scoreDirectorMock, times(1)).beforeProblemFactAdded(addedFact);
        verify(scoreDirectorMock, times(1)).afterProblemFactAdded(addedFact);

        verify(scoreDirectorMock, times(1)).beforeProblemFactRemoved(removedFact);
        verify(scoreDirectorMock, times(1)).afterProblemFactRemoved(removedFact);
    }

    @Test
    void verify_noResetSolutionIfNoEntitiesAddedOrRemoved() {
        var entityGroupOne = new TestdataLavishEntityGroup("entityGroupOne");
        var valueGroupOne = new TestdataLavishValueGroup("valueGroupOne");
        var addedFact = new TestdataLavishValue("newly added fact", valueGroupOne);
        var removedFact = new TestdataLavishValue("fact to remove", valueGroupOne);
        var changedEntity = new TestdataLavishEntity("changed entity", entityGroupOne);
        var changedEntityValue = new TestdataLavishValue("changed entity value", valueGroupOne);

        InnerScoreDirector<TestdataSolution, ?> scoreDirectorMock = mock(InnerScoreDirector.class);
        when(scoreDirectorMock.lookUpWorkingObject(changedEntity)).thenReturn(changedEntity);
        when(scoreDirectorMock.lookUpWorkingObject(removedFact)).thenReturn(removedFact);
        var defaultProblemChangeDirector = new DefaultProblemChangeDirector<>(scoreDirectorMock);

        ProblemChange<TestdataLavishSolution> problemChange = ((workingSolution, problemChangeDirector) -> {
            // Change a planning variable.
            problemChangeDirector.changeVariable(changedEntity, TestdataLavishEntity.VALUE_FIELD,
                    testdataEntity -> testdataEntity.setValue(changedEntityValue));
            // Change a property
            problemChangeDirector.changeProblemProperty(changedEntity,
                    workingEntity -> workingEntity.setEntityGroup(null));
            // Add a problem fact.
            problemChangeDirector.addProblemFact(addedFact, workingSolution.getValueList()::add);
            // Remove a problem fact.
            problemChangeDirector.removeProblemFact(removedFact, workingSolution.getValueList()::remove);
            problemChangeDirector.updateShadowVariables();
        });

        var testdataSolution = TestdataLavishSolution.generateSolution();
        problemChange.doChange(testdataSolution, defaultProblemChangeDirector);

        verify(scoreDirectorMock, times(0)).setWorkingSolution(any());
        verify(scoreDirectorMock, times(1)).triggerVariableListeners();
    }

    @Test
    void verify_ResetSolutionIfEntitiesAddedOrRemoved() {
        var entityGroupOne = new TestdataLavishEntityGroup("entityGroupOne");
        var addedEntity = new TestdataLavishEntity("newly added entity", entityGroupOne);
        var removedEntity = new TestdataLavishEntity("entity to remove", entityGroupOne);

        InnerScoreDirector<TestdataSolution, ?> scoreDirectorMock = mock(InnerScoreDirector.class);
        when(scoreDirectorMock.lookUpWorkingObject(removedEntity)).thenReturn(removedEntity);
        var defaultProblemChangeDirector = new DefaultProblemChangeDirector<>(scoreDirectorMock);

        ProblemChange<TestdataLavishSolution> addProblemChange = ((workingSolution, problemChangeDirector) -> {
            // Add an entity.
            problemChangeDirector.addEntity(addedEntity, workingSolution.getEntityList()::add);
            problemChangeDirector.updateShadowVariables();
        });

        var testdataSolution = TestdataLavishSolution.generateSolution();
        addProblemChange.doChange(testdataSolution, defaultProblemChangeDirector);

        verify(scoreDirectorMock, times(1)).setWorkingSolution(any());
        verify(scoreDirectorMock, times(0)).triggerVariableListeners();

        ProblemChange<TestdataLavishSolution> removeProblemChange = ((workingSolution, problemChangeDirector) -> {
            // Add an entity.
            problemChangeDirector.removeEntity(removedEntity, workingSolution.getEntityList()::remove);
            problemChangeDirector.updateShadowVariables();
        });

        testdataSolution = TestdataLavishSolution.generateSolution();
        removeProblemChange.doChange(testdataSolution, defaultProblemChangeDirector);

        verify(scoreDirectorMock, times(2)).setWorkingSolution(any());
        verify(scoreDirectorMock, times(0)).triggerVariableListeners();
    }

    @Test
    void verify_ResetSolutionOnceIfEntitiesAddedOrRemovedThenVariablesChanged() {
        var entityGroupOne = new TestdataLavishEntityGroup("entityGroupOne");
        var valueGroupOne = new TestdataLavishValueGroup("valueGroupOne");
        var addedEntity = new TestdataLavishEntity("newly added entity", entityGroupOne);
        var removedEntity = new TestdataLavishEntity("entity to remove", entityGroupOne);
        var addedFact = new TestdataLavishValue("newly added fact", valueGroupOne);
        var removedFact = new TestdataLavishValue("fact to remove", valueGroupOne);
        var changedEntity = new TestdataLavishEntity("changed entity", entityGroupOne);
        var changedEntityValue = new TestdataLavishValue("changed entity value", valueGroupOne);

        InnerScoreDirector<TestdataSolution, ?> scoreDirectorMock = mock(InnerScoreDirector.class);
        when(scoreDirectorMock.lookUpWorkingObject(removedEntity)).thenReturn(removedEntity);
        when(scoreDirectorMock.lookUpWorkingObject(changedEntity)).thenReturn(changedEntity);
        when(scoreDirectorMock.lookUpWorkingObject(removedFact)).thenReturn(removedFact);
        var defaultProblemChangeDirector = new DefaultProblemChangeDirector<>(scoreDirectorMock);

        ProblemChange<TestdataLavishSolution> problemChange = ((workingSolution, problemChangeDirector) -> {
            // Add an entity.
            problemChangeDirector.addEntity(addedEntity, workingSolution.getEntityList()::add);
            // Remove an entity.
            problemChangeDirector.removeEntity(removedEntity, workingSolution.getEntityList()::remove);

            problemChangeDirector.updateShadowVariables();

            // Change a planning variable.
            problemChangeDirector.changeVariable(changedEntity, TestdataLavishEntity.VALUE_FIELD,
                    testdataEntity -> testdataEntity.setValue(changedEntityValue));
            // Change a property
            problemChangeDirector.changeProblemProperty(changedEntity,
                    workingEntity -> workingEntity.setEntityGroup(null));
            // Add a problem fact.
            problemChangeDirector.addProblemFact(addedFact, workingSolution.getValueList()::add);
            // Remove a problem fact.
            problemChangeDirector.removeProblemFact(removedFact, workingSolution.getValueList()::remove);

            problemChangeDirector.updateShadowVariables();
        });

        var testdataSolution = TestdataLavishSolution.generateSolution();
        testdataSolution.getEntityList().add(removedEntity);
        testdataSolution.getEntityList().add(changedEntity);
        testdataSolution.getValueList().add(removedFact);
        testdataSolution.getValueList().add(changedEntityValue);

        problemChange.doChange(testdataSolution, defaultProblemChangeDirector);

        verify(scoreDirectorMock, times(1)).setWorkingSolution(any());
        verify(scoreDirectorMock, times(1)).triggerVariableListeners();
    }
}
