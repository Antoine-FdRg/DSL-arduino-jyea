package io.github.mosser.arduinoml.kernel.behavioral;

import io.github.mosser.arduinoml.kernel.generator.Visitor;

import java.util.ArrayList;
import java.util.List;

public class LogicalExpression implements Expression {

    private LOGIC operator;
    private List<Expression> expressions = new ArrayList<>();

    public LOGIC getOperator() {
        return operator;
    }

    public void setOperator(LOGIC operator) {
        this.operator = operator;
    }

    public List<Expression> getExpressions() {
        return expressions;
    }

    public void setExpressions(List<Expression> expressions) {
        this.expressions = expressions;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}