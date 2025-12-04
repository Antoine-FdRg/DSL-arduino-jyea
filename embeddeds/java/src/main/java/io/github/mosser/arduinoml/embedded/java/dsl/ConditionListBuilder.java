package io.github.mosser.arduinoml.embedded.java.dsl;

import io.github.mosser.arduinoml.kernel.behavioral.BooleanExpression;
import io.github.mosser.arduinoml.kernel.behavioral.ConditionList;
import io.github.mosser.arduinoml.kernel.behavioral.LOGIC;

import java.util.ArrayList;
import java.util.List;

public class ConditionListBuilder extends BooleanExpressionBuilder{

    private BooleanExpressionBuilder booleanExpressionBuilderParent;
    private LOGIC connector;

    private ConditionList local = new ConditionList();

    private List<BooleanExpression> conditions = new ArrayList<>();

    ConditionListBuilder(TransitionBuilder parent, BooleanExpressionBuilder booleanExpressionBuilderParent, LOGIC connector) {
        super(parent);
        this.booleanExpressionBuilderParent = booleanExpressionBuilderParent;
        local.setConnector(connector);
    }

    public BooleanExpressionBuilder endConditions() {
        local.setExpressions(conditions);
        return booleanExpressionBuilderParent;
    }

    @Override
    public ConditionListBuilder saveExpression(BooleanExpression be) {
        conditions.add(be);
        return this;
    }



}
