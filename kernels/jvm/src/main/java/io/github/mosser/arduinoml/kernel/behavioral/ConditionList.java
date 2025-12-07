package io.github.mosser.arduinoml.kernel.behavioral;

import io.github.mosser.arduinoml.kernel.generator.Visitable;
import io.github.mosser.arduinoml.kernel.generator.Visitor;

import java.util.ArrayList;
import java.util.List;

public class ConditionList extends BooleanExpression{
    protected LOGIC connector;
    private List<BooleanExpression> expressions = new ArrayList<>();

    public List<BooleanExpression> getExpressions() {
        return expressions;
    }
    public void setExpressions(List<BooleanExpression> expressions) {
            this.expressions = expressions;
        }
        
    public LOGIC getConnector() {
        return connector;
    }

    public void setConnector(LOGIC connector) {
        this.connector = connector;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
