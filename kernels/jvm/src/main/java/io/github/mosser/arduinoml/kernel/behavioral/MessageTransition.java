package io.github.mosser.arduinoml.kernel.behavioral;

import io.github.mosser.arduinoml.kernel.generator.Visitor;

public class MessageTransition extends BooleanExpression {
    private String pattern;
    private boolean matchAny;

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
