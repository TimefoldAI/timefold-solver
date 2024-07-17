package ai.timefold.jpyinterpreter.opcodes.descriptor;

import java.util.function.Function;

import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.opcodes.Opcode;
import ai.timefold.jpyinterpreter.opcodes.collection.BuildConstantKeyMapOpcode;
import ai.timefold.jpyinterpreter.opcodes.collection.BuildListOpcode;
import ai.timefold.jpyinterpreter.opcodes.collection.BuildMapOpcode;
import ai.timefold.jpyinterpreter.opcodes.collection.BuildSetOpcode;
import ai.timefold.jpyinterpreter.opcodes.collection.BuildSliceOpcode;
import ai.timefold.jpyinterpreter.opcodes.collection.BuildTupleOpcode;
import ai.timefold.jpyinterpreter.opcodes.collection.CollectionAddAllOpcode;
import ai.timefold.jpyinterpreter.opcodes.collection.CollectionAddOpcode;
import ai.timefold.jpyinterpreter.opcodes.collection.ContainsOpcode;
import ai.timefold.jpyinterpreter.opcodes.collection.DeleteItemOpcode;
import ai.timefold.jpyinterpreter.opcodes.collection.GetIterOpcode;
import ai.timefold.jpyinterpreter.opcodes.collection.ListToTupleOpcode;
import ai.timefold.jpyinterpreter.opcodes.collection.MapMergeOpcode;
import ai.timefold.jpyinterpreter.opcodes.collection.MapPutAllOpcode;
import ai.timefold.jpyinterpreter.opcodes.collection.MapPutOpcode;
import ai.timefold.jpyinterpreter.opcodes.collection.SetItemOpcode;
import ai.timefold.jpyinterpreter.opcodes.collection.UnpackSequenceOpcode;
import ai.timefold.jpyinterpreter.opcodes.collection.UnpackSequenceWithTailOpcode;

public enum CollectionOpDescriptor implements OpcodeDescriptor {
    /**
     * Implements TOS = iter(TOS).
     */
    GET_ITER(GetIterOpcode::new),

    /**
     * Implements TOS1[TOS] = TOS2.
     */
    STORE_SUBSCR(SetItemOpcode::new),

    /**
     * Implements del TOS1[TOS].
     */
    DELETE_SUBSCR(DeleteItemOpcode::new),
    CONTAINS_OP(ContainsOpcode::new),
    UNPACK_SEQUENCE(UnpackSequenceOpcode::new),
    UNPACK_EX(UnpackSequenceWithTailOpcode::new),

    // **************************************************
    // Collection Construction Operations
    // **************************************************
    BUILD_SLICE(BuildSliceOpcode::new),
    BUILD_TUPLE(BuildTupleOpcode::new),
    BUILD_LIST(BuildListOpcode::new),
    BUILD_SET(BuildSetOpcode::new),
    BUILD_MAP(BuildMapOpcode::new),
    BUILD_CONST_KEY_MAP(BuildConstantKeyMapOpcode::new),

    // **************************************************
    // Collection Edit Operations
    // **************************************************
    LIST_TO_TUPLE(ListToTupleOpcode::new),

    /**
     * Calls set.add(TOS1[-i], TOS). Used to implement set comprehensions.
     * <p>
     * The added value is popped off, the container object remains on the stack so that it is available for further
     * iterations of the loop.
     */
    SET_ADD(CollectionAddOpcode::new),

    /**
     * Calls list.append(TOS1[-i], TOS). Used to implement list comprehensions.
     * <p>
     * The added value is popped off, the container object remains on the stack so that it is available for further
     * iterations of the loop.
     */
    LIST_APPEND(CollectionAddOpcode::new),

    /**
     * Calls dict.__setitem__(TOS1[-i], TOS1, TOS). Used to implement dict comprehensions.
     * <p>
     * The key/value pair is popped off, the container object remains on the stack so that it is available for further
     * iterations of the loop.
     */
    MAP_ADD(MapPutOpcode::new),
    LIST_EXTEND(CollectionAddAllOpcode::new),
    SET_UPDATE(CollectionAddAllOpcode::new),
    DICT_UPDATE(MapPutAllOpcode::new),
    DICT_MERGE(MapMergeOpcode::new);

    final VersionMapping versionLookup;

    CollectionOpDescriptor(Function<PythonBytecodeInstruction, Opcode> instructionToOpcode) {
        this.versionLookup = VersionMapping.constantMapping(instructionToOpcode);
    }

    @Override
    public VersionMapping getVersionMapping() {
        return versionLookup;
    }
}
