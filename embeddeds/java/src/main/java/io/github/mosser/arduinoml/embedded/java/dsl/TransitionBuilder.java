package io.github.mosser.arduinoml.embedded.java.dsl;

import io.github.mosser.arduinoml.kernel.behavioral.LOGIC;
import io.github.mosser.arduinoml.kernel.behavioral.MessageTransition;
import io.github.mosser.arduinoml.kernel.behavioral.TimeTransition;
import io.github.mosser.arduinoml.kernel.behavioral.Transition;
import io.github.mosser.arduinoml.kernel.structural.Sensor;
import io.github.mosser.arduinoml.kernel.behavioral.BooleanExpression;

public class TransitionBuilder implements  IBooleanExpression {

    private TransitionTableBuilder parent;

    private Transition local;

    private BooleanExpression booleanExpression;


    TransitionBuilder(TransitionTableBuilder parent, String source) {
        this.parent = parent;
        this.local = new Transition();
        parent.findState(source).setTransition(local);
    }

    public TransitionBuilder saveExpression(BooleanExpression be) {
        this.booleanExpression = be;
        return this;
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

    public ConditionListBuilder and() {
        return startAndConditionList();
    }

    public ConditionListBuilder or() {
        return startOrConditionList();
    }

    public TransitionBuilder waitFor(int delayInMs) {
        this.saveExpression(new TimeTransition(delayInMs));
        return this;
    }
    public TransitionBuilder receives(String pattern) {
        MessageTransition messageTransition = new MessageTransition();
        // Enlever les guillemets échappés si présents pour stocker juste le pattern
        if (pattern.startsWith("\"") && pattern.endsWith("\"")) {
            pattern = pattern.substring(1, pattern.length() - 1);
        }
        messageTransition.setPattern(pattern);
        this.saveExpression(messageTransition);
        return this;
    }

    public TransitionBuilder receivesAny() {
        MessageTransition messagetransition = new MessageTransition();
        messagetransition.setMatchAny(true);
        this.saveExpression(messagetransition);
        return this;
    }


    public TransitionTableBuilder goTo(String state) {
        local.setCondition(booleanExpression);
        local.setNext(parent.findState(state));
        if(booleanExpression == null){
            throw new IllegalStateException("No condition defined for going to state: ["+state+"]\nHow to fix  : Define at least one using when(<sensorName>) or waitFor(delayMs)");
        }
        return parent;
    }

   
    public Sensor findSensor(String sensorName) {
        return parent.findSensor(sensorName);
    }

    public BooleanExpression getBooleanExpression() {
        return booleanExpression;
    }
    public TransitionTableBuilder error(int errorCode) {
        local.setCondition(booleanExpression);
        local.setNext(parent.getErrorState(errorCode));
        if(booleanExpression == null){
            throw new IllegalStateException("No condition defined for going to error state\nHow to fix  : Define at least one using when(<sensorName>)");
        }
        return parent;
    }

    
 

}