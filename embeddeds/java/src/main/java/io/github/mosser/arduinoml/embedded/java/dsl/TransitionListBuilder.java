package io.github.mosser.arduinoml.embedded.java.dsl;

import io.github.mosser.arduinoml.kernel.behavioral.LOGIC;
import io.github.mosser.arduinoml.kernel.behavioral.Transition;
import io.github.mosser.arduinoml.kernel.behavioral.TransitionList;
import io.github.mosser.arduinoml.kernel.structural.Sensor;

import java.util.ArrayList;
import java.util.List;

public class TransitionListBuilder {

    private TransitionTableBuilder parent;

    private TransitionList local;

    private List<Transition> transitionList = new ArrayList<>();

    TransitionListBuilder(TransitionTableBuilder parent, String source) {
        this.parent = parent;
        this.local = new TransitionList();
        parent.findState(source).setTransitionList(local);
    }


    public TransitionListBuilder and(){
        if(local.getConnector() != null && local.getConnector() != LOGIC.AND){
            throw new IllegalStateException("Connector already defined as "+local.getConnector()+", cannot change to AND." +
                    "\nHow to fix  : Do not mix AND/OR connectors in the same transition list.");
        }
        this.local.setConnector(LOGIC.AND);
        return this;
    }

    public TransitionListBuilder or(){
        if(local.getConnector() != null && local.getConnector() != LOGIC.OR){
            throw new IllegalStateException("Connector already defined as "+local.getConnector()+", cannot change to OR." +
                    "\nHow to fix  : Do not mix AND/OR connectors in the same transition list.");
        }
        this.local.setConnector(LOGIC.OR);
        return this;
    }

    public TransitionTableBuilder goTo(String state) {
        local.setTransitions(transitionList);
        local.setNext(parent.findState(state));
        if(transitionList.isEmpty()){
            throw new IllegalStateException("No transitions defined for going to state: ["+state+"]\nHow to fix  : Define at least one using when(<sensorName>)");
        }
        if(local.getConnector() == null && transitionList.size() > 1){
            throw new IllegalStateException("Multiple transitions defined without a connector (AND/OR) for going to state: ["+state+"]\nHow to fix  : define a connector using startAnd()/startOr()");
        }
        return parent;
    }

    public TransitionBuilder when(String sensor) {
        return new TransitionBuilder(this).when(sensor);
    }

    Sensor findSensor(String sensorName) {
        return parent.findSensor(sensorName);
    }

    void addTransition(Transition t) {
        this.transitionList.add(t);
    }
}