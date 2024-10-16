package ai.timefold.solver.core.impl.heuristic.selector.move.generic.chained;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.inverserelation.SingletonInverseVariableSupply;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.SwapMove;
import ai.timefold.solver.core.impl.score.director.VariableDescriptorAwareScoreDirector;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class ChainedSwapMove<Solution_> extends SwapMove<Solution_> {

    protected final List<Object> oldLeftTrailingEntityList;
    protected final List<Object> oldRightTrailingEntityList;

    public ChainedSwapMove(List<GenuineVariableDescriptor<Solution_>> variableDescriptorList,
            List<SingletonInverseVariableSupply> inverseVariableSupplyList, Object leftEntity, Object rightEntity) {
        super(variableDescriptorList, leftEntity, rightEntity);
        oldLeftTrailingEntityList = new ArrayList<>(inverseVariableSupplyList.size());
        oldRightTrailingEntityList = new ArrayList<>(inverseVariableSupplyList.size());
        for (SingletonInverseVariableSupply inverseVariableSupply : inverseVariableSupplyList) {
            boolean hasSupply = inverseVariableSupply != null;
            oldLeftTrailingEntityList.add(hasSupply ? inverseVariableSupply.getInverseSingleton(leftEntity) : null);
            oldRightTrailingEntityList.add(hasSupply ? inverseVariableSupply.getInverseSingleton(rightEntity) : null);
        }
    }

    public ChainedSwapMove(List<GenuineVariableDescriptor<Solution_>> genuineVariableDescriptors,
            Object leftEntity, Object rightEntity,
            List<Object> oldLeftTrailingEntityList, List<Object> oldRightTrailingEntityList) {
        super(genuineVariableDescriptors, leftEntity, rightEntity);
        this.oldLeftTrailingEntityList = oldLeftTrailingEntityList;
        this.oldRightTrailingEntityList = oldRightTrailingEntityList;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Solution_> scoreDirector) {
        for (var i = 0; i < variableDescriptorList.size(); i++) {
            GenuineVariableDescriptor<Solution_> variableDescriptor = variableDescriptorList.get(i);
            var oldLeftValue = variableDescriptor.getValue(leftEntity);
            var oldRightValue = variableDescriptor.getValue(rightEntity);
            if (!Objects.equals(oldLeftValue, oldRightValue)) {
                var castScoreDirector = (VariableDescriptorAwareScoreDirector<Solution_>) scoreDirector;
                boolean isChained = variableDescriptor instanceof BasicVariableDescriptor<Solution_> basicVariableDescriptor
                        && basicVariableDescriptor.isChained();
                if (!isChained) {
                    castScoreDirector.changeVariableFacade(variableDescriptor, leftEntity, oldRightValue);
                    castScoreDirector.changeVariableFacade(variableDescriptor, rightEntity, oldLeftValue);
                } else {
                    var oldLeftTrailingEntity = oldLeftTrailingEntityList.get(i);
                    var oldRightTrailingEntity = oldRightTrailingEntityList.get(i);
                    if (oldRightValue == leftEntity) {
                        // Change the right entity
                        castScoreDirector.changeVariableFacade(variableDescriptor, rightEntity, oldLeftValue);
                        // Change the left entity
                        castScoreDirector.changeVariableFacade(variableDescriptor, leftEntity, rightEntity);
                        // Reroute the new left chain
                        if (oldRightTrailingEntity != null) {
                            castScoreDirector.changeVariableFacade(variableDescriptor, oldRightTrailingEntity, leftEntity);
                        }
                    } else if (oldLeftValue == rightEntity) {
                        // Change the right entity
                        castScoreDirector.changeVariableFacade(variableDescriptor, leftEntity, oldRightValue);
                        // Change the left entity
                        castScoreDirector.changeVariableFacade(variableDescriptor, rightEntity, leftEntity);
                        // Reroute the new left chain
                        if (oldLeftTrailingEntity != null) {
                            castScoreDirector.changeVariableFacade(variableDescriptor, oldLeftTrailingEntity, rightEntity);
                        }
                    } else {
                        // Change the left entity
                        castScoreDirector.changeVariableFacade(variableDescriptor, leftEntity, oldRightValue);
                        // Change the right entity
                        castScoreDirector.changeVariableFacade(variableDescriptor, rightEntity, oldLeftValue);
                        // Reroute the new left chain
                        if (oldRightTrailingEntity != null) {
                            castScoreDirector.changeVariableFacade(variableDescriptor, oldRightTrailingEntity, leftEntity);
                        }
                        // Reroute the new right chain
                        if (oldLeftTrailingEntity != null) {
                            castScoreDirector.changeVariableFacade(variableDescriptor, oldLeftTrailingEntity, rightEntity);
                        }
                    }
                }
            }
        }
    }

    @Override
    public ChainedSwapMove<Solution_> rebase(ScoreDirector<Solution_> destinationScoreDirector) {
        return new ChainedSwapMove<>(variableDescriptorList,
                destinationScoreDirector.lookUpWorkingObject(leftEntity),
                destinationScoreDirector.lookUpWorkingObject(rightEntity),
                rebaseList(oldLeftTrailingEntityList, destinationScoreDirector),
                rebaseList(oldRightTrailingEntityList, destinationScoreDirector));
    }

}
