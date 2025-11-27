package io.github.mosser.arduinoml.kernel.behavioral;

import io.github.mosser.arduinoml.kernel.generator.Visitable;
import io.github.mosser.arduinoml.kernel.generator.Visitor;

import java.util.ArrayList;
import java.util.List;

public class TransitionList implements Visitable {
    protected List<Transition> transitions = new ArrayList<>();
    protected LOGIC connector;
    protected State next;

    public State getNext() {
        return next;
    }

    public void setNext(State next) {
        this.next = next;
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<Transition> transitions) {
        this.transitions = transitions;
    }

    public LOGIC getConnector() {
        return connector;
    }

    public void setConnector(LOGIC connector) {
        this.connector = connector;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
