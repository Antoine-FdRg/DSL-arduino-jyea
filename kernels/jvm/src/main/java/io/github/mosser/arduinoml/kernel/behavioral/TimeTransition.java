package io.github.mosser.arduinoml.kernel.behavioral;

import io.github.mosser.arduinoml.kernel.generator.Visitor;

/**
 * Represents a temporal transition triggered after a specific delay in milliseconds.
 * This transition type is triggered automatically after entering a state,
 * without requiring any sensor input.
 */
public class TimeTransition extends Transition {

    private int delayInMS;


    public int getDelay() {
        return delayInMS;
    }

    public void setDelay(int newDelay) {
        this.delayInMS = newDelay;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
