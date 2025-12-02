package io.github.mosser.arduinoml.embedded.java.dsl;

import io.github.mosser.arduinoml.kernel.behavioral.SerialTransition;

public class SerialTransitionBuilder {

    private TransitionListBuilder parent;
    private SerialTransition local;

    SerialTransitionBuilder(TransitionListBuilder parent) {
        this.parent = parent;
        this.local = new SerialTransition();
    }

    public SerialTransitionBuilder whenSerial(String sensorName) {
        local.setSensor(parent.findSerialSensor(sensorName));
        return this;
    }

    public TransitionListBuilder receives(String pattern) {
        // Enlever les guillemets échappés si présents pour stocker juste le pattern
        if (pattern.startsWith("\"") && pattern.endsWith("\"")) {
            pattern = pattern.substring(1, pattern.length() - 1);
        }
        local.setPattern(pattern);
        parent.addTransition(local);
        return parent;
    }

    public TransitionListBuilder receivesAny() {
        local.setMatchAny(true);
        parent.addTransition(local);
        return parent;
    }

}
