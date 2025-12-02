package io.github.mosser.arduinoml.kernel.behavioral;

public class ConstantText implements MessagePart {
    private String value;
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
