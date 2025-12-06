package io.github.mosser.arduinoml.kernel.behavioral;

import io.github.mosser.arduinoml.kernel.generator.Visitable;
import io.github.mosser.arduinoml.kernel.generator.Visitor;
import io.github.mosser.arduinoml.kernel.structural.LCDScreen;

public class SendAction extends Action {

    private String serialMessage;

    private LCDScreen lcd;
    private LCDAction lcdMessage;

    public boolean isSerialSend() { return lcd == null; }
    public boolean isLcdDisplay() { return lcd != null; }

    public String getSerialMessage() { return serialMessage; }
    public void setSerialMessage(String serialMessage) { this.serialMessage = serialMessage; }

    public LCDScreen getLcd() { return lcd; }
    public void setLcd(LCDScreen lcd) { this.lcd = lcd; }

    public LCDAction getLcdMessage() { return lcdMessage; }
    public void setLcdMessage(LCDAction lcdMessage) { this.lcdMessage = lcdMessage; }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}


