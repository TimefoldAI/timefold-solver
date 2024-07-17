package ai.timefold.jpyinterpreter.opcodes.descriptor;

public enum AsyncOpDescriptor implements OpcodeDescriptor {
    /**
     * Implements TOS = get_awaitable(TOS), where get_awaitable(o) returns o if o is a coroutine object or a generator
     * object with the CO_ITERABLE_COROUTINE flag, or resolves o.__await__.
     */
    GET_AWAITABLE,

    /**
     * Implements TOS = TOS.__aiter__().
     */
    GET_AITER,

    /**
     * Implements PUSH(get_awaitable(TOS.__anext__())). See {@link #GET_AWAITABLE} for details about get_awaitable
     */
    GET_ANEXT,

    /**
     * Terminates an async for loop. Handles an exception raised when awaiting a next item. If TOS is StopAsyncIteration
     * pop 7 values from the stack and restore the exception state using the second three of them. Otherwise re-raise the
     * exception using the three values from the stack. An exception handler block is removed from the block stack.
     */
    END_ASYNC_FOR,

    /**
     * Resolves __aenter__ and __aexit__ from the object on top of the stack.
     * Pushes __aexit__ and result of __aenter__() to the stack.
     */
    BEFORE_ASYNC_WITH,

    /**
     * Creates a new frame object.
     */
    SETUP_ASYNC_WITH;

    @Override
    public VersionMapping getVersionMapping() {
        // TODO
        return VersionMapping.unimplemented();
    }
}
