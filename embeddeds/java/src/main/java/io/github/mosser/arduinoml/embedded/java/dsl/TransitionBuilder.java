package io.github.mosser.arduinoml.embedded.java.dsl;

import io.github.mosser.arduinoml.kernel.behavioral.MessageTransition;
import io.github.mosser.arduinoml.kernel.behavioral.Transition;
import io.github.mosser.arduinoml.kernel.structural.Sensor;
import io.github.mosser.arduinoml.kernel.behavioral.BooleanExpression;

public class TransitionBuilder {

    private TransitionTableBuilder parent;

    private Transition local;

    private BooleanExpression booleanExpression;


    TransitionBuilder(TransitionTableBuilder parent, String source) {
        this.parent = parent;
        this.local = new Transition();
        parent.findState(source).setTransition(local);
    }

    public TransitionTableBuilder goTo(String state) {
        local.setCondition(booleanExpression);
        local.setNext(parent.findState(state));
        if(booleanExpression == null){
            throw new IllegalStateException("No condition defined for going to state: ["+state+"]\nHow to fix  : Define at least one using when(<sensorName>) or waitFor(delayMs)");
        }
        /* 
        if(local.getConnector() == null && transitionList.size() > 1){
            throw new IllegalStateException("Multiple transitions defined without a connector (AND/OR) for going to state: ["+state+"]\nHow to fix  : define a connector using and()/or()");
        }
            */
        return parent;
    }

   
    Sensor findSensor(String sensorName) {
        return parent.findSensor(sensorName);
    }

    void setBooleanExpression(BooleanExpression be) {
        this.booleanExpression = be;
    }

    public TransitionTableBuilder getParent() {
        return parent;
    }

    public Transition getLocal() {
        return  local;
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
        /* 
        if(local.getConnector() == null && transitionList.size() > 1){
            throw new IllegalStateException("Multiple transitions defined without a connector (AND/OR) for going to error state\nHow to fix  : define a connector using startAnd()/startOr()");
        }*/
        return parent;
    }

    
 

}