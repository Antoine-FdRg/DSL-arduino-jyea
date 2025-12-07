package io.github.mosser.arduinoml.kernel.behavioral;
import io.github.mosser.arduinoml.kernel.structural.Brick;

import io.github.mosser.arduinoml.kernel.generator.Visitor;
public class BrickValueRef implements MessagePart {
    private Brick brick;
    public Brick getBrick() { return brick; }
    public void setBrick(Brick brick) { this.brick = brick; }
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
