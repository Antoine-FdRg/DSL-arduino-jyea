package io.github.mosser.arduinoml.embedded.java.dsl;

import io.github.mosser.arduinoml.kernel.behavioral.BooleanExpression;
import io.github.mosser.arduinoml.kernel.behavioral.ConditionList;
import io.github.mosser.arduinoml.kernel.behavioral.LOGIC;
import io.github.mosser.arduinoml.kernel.structural.Sensor;

public class ConditionListBuilder implements IBooleanExpression {

    private final TransitionBuilder parent;
    private final ConditionList list;

    public ConditionListBuilder(TransitionBuilder parent, LOGIC connector) {
        this.parent = parent;
        this.list = new ConditionList();
        this.list.setConnector(connector);

        BooleanExpression previous = parent.getBooleanExpression();
        if (previous != null) {
            this.list.getExpressions().add(previous);
        }
    }

    @Override
    public Sensor findSensor(String name) {
        return parent.findSensor(name);
    }

    @Override
    public TransitionBuilder saveExpression(BooleanExpression be) {
        this.list.getExpressions().add(be);
        return parent.saveExpression(list);
    }

    public SignalTransitionBuilder when(String sensor) {
        return new SignalTransitionBuilder(this, findSensor(sensor));
    }
}
