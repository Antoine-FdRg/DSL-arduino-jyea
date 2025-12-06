package io.github.mosser.arduinoml.kernel.behavioral;

import io.github.mosser.arduinoml.kernel.generator.Visitor;
import io.github.mosser.arduinoml.kernel.structural.LCDScreen;
import java.util.List;

public class LCDAction extends Action {

    private LCDScreen screen;
    private List<MessagePart> message;

    public LCDScreen getScreen() { return screen; }
    public void setScreen(LCDScreen screen) { this.screen = screen; }

    public List<MessagePart> getMessage() { return message; }
    public void setMessage(List<MessagePart> message) { this.message = message; }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}