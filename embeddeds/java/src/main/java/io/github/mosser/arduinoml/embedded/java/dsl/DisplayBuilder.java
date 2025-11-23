package io.github.mosser.arduinoml.embedded.java.dsl;

import io.github.mosser.arduinoml.kernel.behavioral.LCDAction;
import io.github.mosser.arduinoml.kernel.behavioral.ConstantText;
import io.github.mosser.arduinoml.kernel.behavioral.BrickValueRef;
import io.github.mosser.arduinoml.kernel.behavioral.MessagePart;
import io.github.mosser.arduinoml.kernel.structural.Actuator;
import io.github.mosser.arduinoml.kernel.structural.Brick;
import io.github.mosser.arduinoml.kernel.structural.LCDScreen;
import io.github.mosser.arduinoml.kernel.structural.Sensor;

import java.util.ArrayList;
import java.util.Optional;

public class DisplayBuilder {

    final StateBuilder parent;
    final LCDAction local = new LCDAction();

    DisplayBuilder(StateBuilder parent, String lcdName) {
        this.parent = parent;

        Optional<LCDScreen> optLcd = parent.parent.findLCD(lcdName);
        LCDScreen screen = optLcd.orElseThrow(
                () -> new IllegalArgumentException("Illegal LCD: [" + lcdName + "]")
        );

        local.setScreen(screen);
        local.setMessage(new ArrayList<MessagePart>());
    }

    public DisplayBuilder text(String value) {
        ConstantText t = new ConstantText();
        t.setValue(value);
        local.getMessage().add(t);
        return this;
    }

    public DisplayBuilder valueOf(String brickName) {
        Optional<Brick> optBrick = parent.parent.findBrick(brickName);
        Brick brick = optBrick.orElseThrow(
                () -> new IllegalArgumentException("Illegal brick: [" + brickName + "]")
        );

        BrickValueRef ref = new BrickValueRef();
        ref.setBrick(brick);
        local.getMessage().add(ref);
        return this;
    }

    public StateBuilder endDisplay() {
        validateLCDAction(local, parent.local.getName());
        parent.local.getActions().add(local);
        return parent;
    }

    // STATIC VALIDATION of LCD messages :
    // max 32 characters and only ASCII printable characters
    private void validateLCDAction(LCDAction action, String stateName) {
        final int MAX = 16 * 2;   // 32 characters for 16x2 screen
        int total = 0;
        for (MessagePart part : action.getMessage()) {
            if (part instanceof ConstantText) {
                ConstantText text = (ConstantText) part;
                String v = text.getValue();

                if (!v.matches("[ -~]*")) {
                    throw new IllegalArgumentException(
                            "LCD text contains unsupported characters in state '" +
                                    stateName + "': \"" + v + "\" " +
                                    "(only ASCII printable characters allowed)"
                    );
                }
                total += v.length();
            } else if (part instanceof BrickValueRef) {
                BrickValueRef ref = (BrickValueRef) part;
                Brick b = ref.getBrick();
                if (b instanceof Sensor) {
                    total += 4; // "HIGH" or "LOW"
                } else if (b instanceof Actuator) {
                    total += 3; // "ON" or "OFF"
                } else {
                    total += 4; // Fallback
                }
            }
        }
        if (total > MAX) {
            throw new IllegalArgumentException(
                    "LCD message too long in state '" + stateName +
                            "': " + total + " characters (max is " + MAX + ")."
            );
        }
    }
}
