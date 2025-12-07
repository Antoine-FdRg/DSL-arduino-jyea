package io.github.mosser.arduinoml.kernel.generator;

import io.github.mosser.arduinoml.kernel.behavioral.*;
import io.github.mosser.arduinoml.kernel.structural.*;
import io.github.mosser.arduinoml.kernel.App;

import java.util.HashMap;
import java.util.Map;

public abstract class Visitor<T> {

	public abstract void visit(App app);

	public abstract void visit(State state);

	public abstract void visit(SignalTransition transition);

	public abstract void visit(TimeTransition transition);

	public abstract void visit(ConditionList transitionList);
	public abstract void visit(Transition transition);

	public abstract void visit(SetAction setAction);

	public abstract void visit(Actuator actuator);

	public abstract void visit(Sensor sensor);

	public abstract void visit(LCDScreen lcd);
	public abstract void visit(LCDAction lcdAction);
	public abstract void visit(SendAction action);
    public abstract void visit(MessageTransition transition);
	public abstract void visit(ConstantText constantText);
	public abstract void visit(BrickValueRef brickValueRef);

	/***********************
	 ** Helper mechanisms **
	 ***********************/

	protected Map<String, Object> context = new HashMap<>();

	protected T result;

	public T getResult() {
		return result;
	}

}
