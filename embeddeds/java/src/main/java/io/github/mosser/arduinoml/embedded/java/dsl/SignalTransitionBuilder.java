package io.github.mosser.arduinoml.embedded.java.dsl;

import io.github.mosser.arduinoml.kernel.behavioral.SignalTransition;
import io.github.mosser.arduinoml.kernel.structural.SIGNAL;
import io.github.mosser.arduinoml.kernel.structural.Sensor;

public class SignalTransitionBuilder {
    
    private IBooleanExpression parent;
    private Sensor sensor;

    private SignalTransition local = new SignalTransition();

    SignalTransitionBuilder(IBooleanExpression parent, Sensor sensor) {
        this.parent = parent;
        this.sensor = sensor;
    }

    public IBooleanExpression isHigh() {
        local.setValue(SIGNAL.HIGH);
        parent.saveExpression(local);
        return parent;
    }

    public IBooleanExpression isLow() {
        local.setValue(SIGNAL.LOW);
        parent.saveExpression(local);
        return parent;
    }
}
