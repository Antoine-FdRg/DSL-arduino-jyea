package io.github.mosser.arduinoml.kernel.generator;

import io.github.mosser.arduinoml.kernel.App;
import io.github.mosser.arduinoml.kernel.behavioral.*;
import io.github.mosser.arduinoml.kernel.structural.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Quick and dirty visitor to support the generation of Wiring code
 */
public class ToWiring extends Visitor<StringBuffer> {
	enum PASS {ONE, TWO}


	public ToWiring() {
		this.result = new StringBuffer();
	}

	private void w(String s) {
		result.append(String.format("%s",s));
	}

	@Override
	public void visit(App app) {
		//first pass, create global vars
		context.put("pass", PASS.ONE);
		w("// Wiring code generated from an ArduinoML model\n");
		w(String.format("// Application name: %s\n", app.getName())+"\n");

		w("long debounce = 200;\n");
		w("long stateEnteredTime = 0;\n");
		w("int lastState = -1;\n");
		w("\nenum STATE {");
		String sep ="";
		for(State state: app.getStates()){
			w(sep);
			state.accept(this);
			sep=", ";
		}
		w("};\n");
		if (app.getInitial() != null) {
			w("STATE currentState = " + app.getInitial().getName()+";\n");
		}

		for(Brick brick: app.getBricks()){
			brick.accept(this);
		}

		//second pass, setup and loop
		context.put("pass",PASS.TWO);
		w("\nvoid setup(){\n");
		for(Brick brick: app.getBricks()){
			brick.accept(this);
		}
		w("}\n");

		w("\nvoid loop() {\n");
		w("\tif((int)currentState != lastState) {\n");
		w("\t\tstateEnteredTime = millis();\n");
		w("\t\tlastState = (int)currentState;\n");
		w("\t}\n");
		w("\tswitch(currentState){\n");
		for(State state: app.getStates()){
			state.accept(this);
		}
		w("\t}\n" +
			"}");
	}

	@Override
	public void visit(Actuator actuator) {
		if(context.get("pass") == PASS.TWO) {
			w(String.format("  pinMode(%d, OUTPUT); // %s [Actuator]\n", actuator.getPin(), actuator.getName()));
		}
	}


	@Override
	public void visit(Sensor sensor) {
		if(context.get("pass") == PASS.ONE) {
			w(String.format("\nboolean %sBounceGuard = false;\n", sensor.getName()));
			w(String.format("long %sLastDebounceTime = 0;\n", sensor.getName()));
		} else if(context.get("pass") == PASS.TWO) {
			w(String.format("  pinMode(%d, INPUT);  // %s [Sensor]\n", sensor.getPin(), sensor.getName()));
		}
	}

	@Override
	public void visit(State state) {
		if(context.get("pass") == PASS.ONE){
			w(state.getName());
			return;
		}
		if(context.get("pass") == PASS.TWO) {
			w("\t\tcase " + state.getName() + ":\n");
			for (Action action : state.getActions()) {
				action.accept(this);
			}

            state.getTransitionList().accept(this);
        }

	}

	@Override
	public void visit(SignalTransition transition) {
		if(context.get("pass") == PASS.TWO) {
			String sensorName = transition.getSensor().getName();
			w(String.format("\t\t\t\t%sLastDebounceTime = millis();%n", sensorName));
		}
	}

	@Override
	public void visit(TimeTransition transition) {
		// TimeTransition handling will be managed in TransitionList visitor
	}

    @Override
    public void visit(TransitionList transitionList) {
        // Separate signal transitions from temporal transitions
        List<SignalTransition> signalTransitions = transitionList.getTransitions().stream()
                .filter(SignalTransition.class::isInstance)
                .map(SignalTransition.class::cast)
                .collect(Collectors.toList());

        List<TimeTransition> temporalTransitions = transitionList.getTransitions().stream()
                .filter(TimeTransition.class::isInstance)
                .map(TimeTransition.class::cast)
                .collect(Collectors.toList());

        // Handle signal-based transitions
        if (!signalTransitions.isEmpty()) {
            List<String> sensorsName = signalTransitions.stream()
                    .map(t -> t.getSensor().getName())
                    .collect(Collectors.toList());
            for (String name : sensorsName) {
                w(String.format("\t\t\t%sBounceGuard = millis() - %sLastDebounceTime > debounce;%n",
                        name, name));
            }

            List<String> parts = signalTransitions.stream()
                    .map(t -> String.format("(digitalRead(%d) == %s && %sBounceGuard)",
                            t.getSensor().getPin(),
                            t.getValue(),
                            t.getSensor().getName()))
                    .collect(Collectors.toList());

            String connector = transitionList.getConnector() == LOGIC.OR ? " || " : " && ";
            String condition = parts.size() > 1 ? "(" + String.join(connector, parts) + ")" : parts.get(0);
            w(String.format("\t\t\tif( %s ) {%n", condition));
            for (SignalTransition transition : signalTransitions) {
                transition.accept(this);
            }
            w("\t\t\t\tcurrentState = " + transitionList.getNext().getName() + ";\n");
            w("\t\t\t}\n");
        }

        // Handle temporal transitions
        if (!temporalTransitions.isEmpty()) {
            for (TimeTransition tempTransition : temporalTransitions) {
                int delay = tempTransition.getDelay();
                w(String.format("\t\t\tif( millis() - stateEnteredTime > %d ) {%n", delay));
                w("\t\t\t\tcurrentState = " + transitionList.getNext().getName() + ";\n");
                w("\t\t\t}\n");
            }
        }

        w("\t\t\tbreak;\n");
    }


	@Override
	public void visit(Action action) {
		if(context.get("pass") == PASS.TWO) {
			w(String.format("\t\t\tdigitalWrite(%d,%s);\n",action.getActuator().getPin(),action.getValue()));
		}
	}

}
