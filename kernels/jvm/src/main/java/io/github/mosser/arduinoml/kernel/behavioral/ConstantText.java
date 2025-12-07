package io.github.mosser.arduinoml.kernel.behavioral;

import io.github.mosser.arduinoml.kernel.generator.Visitor;
public class ConstantText implements MessagePart {
    private String value;
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
