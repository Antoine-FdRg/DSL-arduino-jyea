package io.github.mosser.arduinoml.embedded.java.dsl;


import io.github.mosser.arduinoml.kernel.behavioral.BooleanExpression;
import io.github.mosser.arduinoml.kernel.behavioral.LOGIC;
import io.github.mosser.arduinoml.kernel.behavioral.MessageTransition;
import io.github.mosser.arduinoml.kernel.behavioral.SignalTransition;
import io.github.mosser.arduinoml.kernel.behavioral.TimeTransition;
import io.github.mosser.arduinoml.kernel.structural.SIGNAL;
import io.github.mosser.arduinoml.embedded.java.dsl.SignalTransitionBuilder;

public class BooleanExpressionBuilder {

    private TransitionBuilder parent;

    BooleanExpressionBuilder(TransitionBuilder parent) {
        this.parent = parent;

    }

    public BooleanExpressionBuilder saveExpression(BooleanExpression be) {
        parent.setBooleanExpression(be);
        return this;
    }


    public SignalTransitionBuilder when(String sensor) {
        return new SignalTransitionBuilder(this, parent.findSensor(sensor));

    }

    public ConditionListBuilder startAndConditionList() {
    return new ConditionListBuilder(parent, this, LOGIC.AND);
    }
    public ConditionListBuilder startOrConditionList() {
    return new ConditionListBuilder(parent, this, LOGIC.OR);
    }

    public BooleanExpressionBuilder waitFor(int delayInMs) {
        this.saveExpression(new TimeTransition(delayInMs));
        return this;
    }
       public BooleanExpressionBuilder receives(String pattern) {
        MessageTransition messageTransition = new MessageTransition();
        // Enlever les guillemets échappés si présents pour stocker juste le pattern
        if (pattern.startsWith("\"") && pattern.endsWith("\"")) {
            pattern = pattern.substring(1, pattern.length() - 1);
        }
        messageTransition.setPattern(pattern);
        this.saveExpression(messageTransition);
        return this;
    }

    public BooleanExpressionBuilder receivesAny() {
        MessageTransition messagetransition = new MessageTransition();
        messagetransition.setMatchAny(true);
        this.saveExpression(messagetransition);
        return this;
    }

}
