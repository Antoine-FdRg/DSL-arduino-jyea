package io.github.mosser.arduinoml.kernel.behavioral;
import io.github.mosser.arduinoml.kernel.structural.Brick;

public class BrickValueRef implements MessagePart {
    private Brick brick;
    public Brick getBrick() { return brick; }
    public void setBrick(Brick brick) { this.brick = brick; }
}
