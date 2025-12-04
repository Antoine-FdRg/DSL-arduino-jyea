package io.github.mosser.arduinoml.embedded.java.dsl;

import io.github.mosser.arduinoml.kernel.behavioral.BooleanExpression;
import io.github.mosser.arduinoml.kernel.behavioral.ConditionList;
import io.github.mosser.arduinoml.kernel.behavioral.LOGIC;
import io.github.mosser.arduinoml.kernel.behavioral.MessageTransition;
import io.github.mosser.arduinoml.kernel.behavioral.TimeTransition;
import io.github.mosser.arduinoml.kernel.structural.Sensor;

import java.util.ArrayList;
import java.util.List;

public class ConditionListBuilder implements IBooleanExpression {

    private IBooleanExpression parent;

    private ConditionList local = new ConditionList();

    private List<BooleanExpression> conditions = new ArrayList<>();

    ConditionListBuilder(IBooleanExpression parent, LOGIC connector) {
        this.parent = parent;
        local.setConnector(connector);
    }

    public IBooleanExpression endConditions() {
        local.setExpressions(conditions);
        return parent;
    }

    public ConditionListBuilder saveExpression(BooleanExpression be) {
        conditions.add(be);
        return this;
    }

    public Sensor findSensor(String sensorName) {
        return parent.findSensor(sensorName);
    }

    public SignalTransitionBuilder when(String sensor) {
        return new SignalTransitionBuilder(this, parent.findSensor(sensor));

    }

    public ConditionListBuilder startAndConditionList() {
        return new ConditionListBuilder(this, LOGIC.AND);
    }

    public ConditionListBuilder startOrConditionList() {
        return new ConditionListBuilder(this, LOGIC.OR);
    }

    public IBooleanExpression waitFor(int delayInMs) {
        this.saveExpression(new TimeTransition(delayInMs));
        return this;
    }

    public IBooleanExpression receives(String pattern) {
        MessageTransition messageTransition = new MessageTransition();
        // Enlever les guillemets échappés si présents pour stocker juste le pattern
        if (pattern.startsWith("\"") && pattern.endsWith("\"")) {
            pattern = pattern.substring(1, pattern.length() - 1);
        }
        messageTransition.setPattern(pattern);
        this.saveExpression(messageTransition);
        return this;
    }

    public IBooleanExpression receivesAny() {
        MessageTransition messagetransition = new MessageTransition();
        messagetransition.setMatchAny(true);
        this.saveExpression(messagetransition);
        return this;
    }

}
