package io.github.mosser.arduinoml.kernel.behavioral;

import io.github.mosser.arduinoml.kernel.generator.Visitor;
import io.github.mosser.arduinoml.kernel.structural.SerialSensor;

public class SerialTransition extends Transition {

    private SerialSensor sensor;
    private String pattern;
    private boolean matchAny;

    public SerialSensor getSensor() {
        return sensor;
    }

    public void setSensor(SerialSensor sensor) {
        this.sensor = sensor;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
        this.matchAny = false;
    }

    public boolean isMatchAny() {
        return matchAny;
    }

    public void setMatchAny(boolean matchAny) {
        this.matchAny = matchAny;
        if (matchAny) {
            this.pattern = null;
        }
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
