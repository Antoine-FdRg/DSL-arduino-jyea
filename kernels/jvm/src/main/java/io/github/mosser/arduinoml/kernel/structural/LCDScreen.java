package io.github.mosser.arduinoml.kernel.structural;

import io.github.mosser.arduinoml.kernel.generator.Visitor;

public class LCDScreen extends Brick {
    private int columns;
    private int rows;
    private int rs, enable, d4, d5, d6, d7;

    public int getColumns() {
        return columns;
    }
    public void setColumns(int columns) {
        this.columns = columns;
    }
    public int getRows() {
        return rows;
    }
    public void setRows(int rows) {
        this.rows = rows;
    }
    public int getRsPin() {
        return rs;
    }
    public void setRsPin(int rs) {
        this.rs = rs;
    }
    public int getEnablePin() {
        return enable;
    }
    public void setEnablePin(int enable) {
        this.enable = enable;
    }
    public int getD4Pin() {
        return d4;
    }
    public void setD4Pin(int d4) {
        this.d4 = d4;
    }
    public int getD5Pin() {
        return d5;
    }
    public void setD5Pin(int d5) {
        this.d5 = d5;
    }
    public int getD6Pin() {
        return d6;
    }
    public void setD6Pin(int d6) {
        this.d6 = d6;
    }
    public int getD7Pin() {
        return d7;
    }
    public void setD7Pin(int d7) {
        this.d7 = d7;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
