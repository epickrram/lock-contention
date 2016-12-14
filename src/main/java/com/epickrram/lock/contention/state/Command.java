package com.epickrram.lock.contention.state;

public final class Command
{
    enum Operation
    {
        ADD,
        SUBTRACT,
        GET_CURRENT,
        GET_HISTORY
    }

    private final long operand;
    private final Operation operation;

    public Command(final long operand, final Operation operation)
    {
        this.operand = operand;
        this.operation = operation;
    }

    long getOperand()
    {
        return operand;
    }

    Operation getOperation()
    {
        return operation;
    }
}
