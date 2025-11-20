package io.github.mosser.arduinoml.kernel.behavioral;

import io.github.mosser.arduinoml.kernel.generator.Visitable;
import io.github.mosser.arduinoml.kernel.generator.Visitor;

public abstract class Transition implements Visitable {

	@Override
	public abstract void accept(Visitor visitor);
}
