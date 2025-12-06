package io.github.mosser.arduinoml.embedded.java.dsl;

import io.github.mosser.arduinoml.kernel.behavioral.BooleanExpression;
import io.github.mosser.arduinoml.kernel.behavioral.SignalTransition;
import io.github.mosser.arduinoml.kernel.structural.SIGNAL;
import io.github.mosser.arduinoml.kernel.structural.Sensor;

public class SignalTransitionBuilder {

    private final IBooleanExpression parent;
    private final Sensor sensor;

    public SignalTransitionBuilder(IBooleanExpression parent, Sensor sensor) {
        this.parent = parent;
        this.sensor = sensor;
    }

    private BooleanExpression build(SIGNAL value) {
        SignalTransition st = new SignalTransition();
        st.setSensor(sensor);
        st.setValue(value);
        return st;
    }

    public TransitionBuilder isHigh() {
        return parent.saveExpression(build(SIGNAL.HIGH));
    }

    public TransitionBuilder isLow() {
        return parent.saveExpression(build(SIGNAL.LOW));
    }
}
