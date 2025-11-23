package io.github.mosser.arduinoml.embedded.java.dsl;


import io.github.mosser.arduinoml.kernel.behavioral.SignalTransition;
import io.github.mosser.arduinoml.kernel.structural.SIGNAL;

public class TransitionBuilder {


    private TransitionListBuilder parent;

    private SignalTransition local;

    TransitionBuilder(TransitionListBuilder parent) {
        this.parent = parent;
        this.local = new SignalTransition();
    }


    public TransitionBuilder when(String sensor) {
        local.setSensor(parent.findSensor(sensor));
        return this;
    }

    public TransitionListBuilder isHigh() {
        local.setValue(SIGNAL.HIGH);
        parent.addTransition(local);
        return parent;
    }

    public TransitionListBuilder isLow() {
        local.setValue(SIGNAL.LOW);
        parent.addTransition(local);
        return parent;
    }
}
