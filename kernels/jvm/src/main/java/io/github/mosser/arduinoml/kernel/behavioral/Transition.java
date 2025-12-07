package io.github.mosser.arduinoml.kernel.behavioral;

import io.github.mosser.arduinoml.kernel.generator.Visitable;
import io.github.mosser.arduinoml.kernel.generator.Visitor;

public class Transition implements Visitable {

	private BooleanExpression condition; 
    private State next;                  
    private Integer errorCode;           

    public BooleanExpression getCondition() { return condition; }
    public void setCondition(BooleanExpression condition) { this.condition = condition; }

    public State getNext() { return next; }
    public void setNext(State next) { this.next = next; }

    public Integer getErrorCode() { return errorCode; }
    public void setErrorCode(Integer errorCode) { this.errorCode = errorCode; }

    public boolean isErrorTransition() {
        return errorCode != null;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }
}
