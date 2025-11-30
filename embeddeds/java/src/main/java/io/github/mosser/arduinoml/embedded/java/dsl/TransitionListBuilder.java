package io.github.mosser.arduinoml.embedded.java.dsl;

import io.github.mosser.arduinoml.kernel.behavioral.Expression;
import io.github.mosser.arduinoml.kernel.behavioral.LOGIC;
import io.github.mosser.arduinoml.kernel.behavioral.LogicalExpression;
import io.github.mosser.arduinoml.kernel.behavioral.SignalTransition;
import io.github.mosser.arduinoml.kernel.behavioral.State;
import io.github.mosser.arduinoml.kernel.structural.Sensor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class TransitionListBuilder {

    private final TransitionTableBuilder parent;
    private final State sourceState;
    private Expression rootExpression;

    private final Deque<LogicalExpression> expressionStack = new ArrayDeque<>();

    private LOGIC flatConnector;
    private final List<SignalTransition> flatTransitions = new ArrayList<>();
    private boolean nestedMode = false;

    TransitionListBuilder(TransitionTableBuilder parent, String source) {
        this.parent = parent;
        this.sourceState = parent.findState(source);
    }

    public TransitionListBuilder and() {
        ensureFlatMode("and()");
        if (flatConnector != null && flatConnector != LOGIC.AND) {
            throw new IllegalStateException(
                    "Connector already defined as " + flatConnector + ", cannot change to AND."
                            + "\nHow to fix  : Do not mix AND/OR connectors in the same flat expression."
            );
        }
        this.flatConnector = LOGIC.AND;
        return this;
    }

    public TransitionListBuilder or() {
        ensureFlatMode("or()");
        if (flatConnector != null && flatConnector != LOGIC.OR) {
            throw new IllegalStateException(
                    "Connector already defined as " + flatConnector + ", cannot change to OR."
                            + "\nHow to fix  : Do not mix AND/OR connectors in the same flat expression."
            );
        }
        this.flatConnector = LOGIC.OR;
        return this;
    }

    private void ensureFlatMode(String method) {
        if (nestedMode) {
            throw new IllegalStateException(
                    "Cannot call " + method + " inside a nested expression.\n"
                            + "How to fix : use startAnd()/startOr()/endAnd()/endOr() for nested expressions, "
                            + "or do not mix flat and nested styles in the same 'from(...)' block."
            );
        }
    }

    public TransitionTableBuilder goTo(String state) {
        if (!nestedMode && flatTransitions.isEmpty()) {
            throw new IllegalStateException(
                    "No transitions defined for going to state: [" + state + "]\n"
                            + "How to fix  : Define at least one using when(<sensorName>)"
            );
        }
        if (nestedMode && rootExpression == null) {
            throw new IllegalStateException(
                    "No expression defined for going to state: [" + state + "]\n"
                            + "How to fix : use when()/startAnd()/startOr() to build an expression."
            );
        }

        Expression expr = buildExpression();
        sourceState.setExpression(expr);
        sourceState.setNext(parent.findState(state));

        return parent;
    }

    private Expression buildExpression() {
        if (nestedMode) {
            if (!expressionStack.isEmpty()) {
                throw new IllegalStateException(
                        "Some nested expressions were not closed.\n"
                                + "How to fix : call endAnd()/endOr() for each startAnd()/startOr()."
                );
            }
            if (rootExpression == null) {
                throw new IllegalStateException("No root expression built in nested mode.");
            }
            return rootExpression;
        }

        if (flatTransitions.size() == 1 && flatConnector == null) {
            return flatTransitions.get(0);
        }

        if (flatConnector == null) {
            throw new IllegalStateException(
                    "Multiple transitions defined without a connector (AND/OR)\n"
                            + "How to fix  : define a connector using and()/or()"
            );
        }

        LogicalExpression logical = new LogicalExpression();
        logical.setOperator(flatConnector);
        logical.setExpressions(new ArrayList<>(flatTransitions));
        return logical;
    }

    public TransitionBuilder when(String sensor) {
        return new TransitionBuilder(this).when(sensor);
    }

    Sensor findSensor(String sensorName) {
        return parent.findSensor(sensorName);
    }

    public TransitionTableBuilder error(int errorCode) {
        if (!nestedMode && flatTransitions.isEmpty()) {
            throw new IllegalStateException(
                    "No transitions defined for going to error state\n"
                            + "How to fix  : Define at least one using when(<sensorName>)"
            );
        }
        if (nestedMode && rootExpression == null) {
            throw new IllegalStateException(
                    "No expression defined for going to error state\n"
                            + "How to fix : use when()/startAnd()/startOr() to build an expression."
            );
        }

        Expression expr = buildExpression();
        sourceState.setExpression(expr);
        sourceState.setNext(parent.getErrorState(errorCode));

        return parent;
    }

    public TransitionListBuilder startAnd() {
        enterNestedMode();
        LogicalExpression andExpr = new LogicalExpression();
        andExpr.setOperator(LOGIC.AND);
        attachComposite(andExpr);
        expressionStack.push(andExpr);
        return this;
    }

    public TransitionListBuilder startOr() {
        enterNestedMode();
        LogicalExpression orExpr = new LogicalExpression();
        orExpr.setOperator(LOGIC.OR);
        attachComposite(orExpr);
        expressionStack.push(orExpr);
        return this;
    }

    public TransitionListBuilder endAnd() {
        if (expressionStack.isEmpty()) {
            throw new IllegalStateException("endAnd() called without a matching startAnd().");
        }
        LogicalExpression top = expressionStack.pop();
        if (top.getOperator() != LOGIC.AND) {
            throw new IllegalStateException(
                    "endAnd() does not match current block (" + top.getOperator() + ").\n"
                            + "How to fix : close blocks in the reverse order they were opened."
            );
        }
        return this;
    }

    public TransitionListBuilder endOr() {
        if (expressionStack.isEmpty()) {
            throw new IllegalStateException("endOr() called without a matching startOr().");
        }
        LogicalExpression top = expressionStack.pop();
        if (top.getOperator() != LOGIC.OR) {
            throw new IllegalStateException(
                    "endOr() does not match current block (" + top.getOperator() + ").\n"
                            + "How to fix : close blocks in the reverse order they were opened."
            );
        }
        return this;
    }

    private void enterNestedMode() {
        if (!flatTransitions.isEmpty() || flatConnector != null) {
            throw new IllegalStateException(
                    "Cannot start a nested expression after defining flat transitions.\n"
                            + "How to fix : use either flat style (when/and/or) OR nested style (startAnd/startOr), "
                            + "but not both in the same 'from(...)' block."
            );
        }
        nestedMode = true;
    }

    private void attachComposite(LogicalExpression composite) {
        if (rootExpression == null && expressionStack.isEmpty()) {
            rootExpression = composite;
        } else if (!expressionStack.isEmpty()) {
            expressionStack.peek().getExpressions().add(composite);
        } else {
            throw new IllegalStateException(
                    "Internal error: trying to attach a composite without an active parent.\n"
                            + "This usually means flat and nested styles were mixed incorrectly."
            );
        }
    }

    void addTransition(SignalTransition t) {
        if (nestedMode) {
            if (expressionStack.isEmpty()) {
                if (rootExpression == null) {
                    rootExpression = t;
                } else {
                    throw new IllegalStateException(
                            "Cannot attach another root-level condition in nested mode without a composite.\n"
                                    + "How to fix : wrap your conditions in startAnd()/startOr()."
                    );
                }
            } else {
                expressionStack.peek().getExpressions().add(t);
            }
        } else {
            this.flatTransitions.add(t);
        }
    }
}