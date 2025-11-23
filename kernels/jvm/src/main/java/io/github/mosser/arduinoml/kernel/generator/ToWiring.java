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

	private String escape(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	@Override
	public void visit(App app) {
		//first pass, create global vars
		context.put("pass", PASS.ONE);
		w("// Wiring code generated from an ArduinoML model\n");
		w(String.format("// Application name: %s\n", app.getName())+"\n");

		w("long debounce = 200;\n");
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

		w("\nvoid loop() {\n" +
			"\tswitch(currentState){\n");
		for(State state: app.getStates()){
			state.accept(this);
		}
		w("\t}\n" +
			"}");
	}

	@Override
	public void visit(Actuator actuator) {
		if(context.get("pass") == PASS.ONE) {
			return;
		}
		if(context.get("pass") == PASS.TWO) {
			w(String.format("  pinMode(%d, OUTPUT); // %s [Actuator]\n", actuator.getPin(), actuator.getName()));
			return;
		}
	}


	@Override
	public void visit(Sensor sensor) {
		if(context.get("pass") == PASS.ONE) {
			w(String.format("\nboolean %sBounceGuard = false;\n", sensor.getName()));
			w(String.format("long %sLastDebounceTime = 0;\n", sensor.getName()));
			return;
		}
		if(context.get("pass") == PASS.TWO) {
			w(String.format("  pinMode(%d, INPUT);  // %s [Sensor]\n", sensor.getPin(), sensor.getName()));
			return;
		}
	}

	@Override
	public void visit(LCDScreen lcd) {
		if (context.get("pass") == PASS.ONE) {
			w("#include <LiquidCrystal.h>\n");
			w(String.format("LiquidCrystal %s(%d, %d, %d, %d, %d, %d);\n",
					lcd.getName(),
					lcd.getRsPin(),
					lcd.getEnablePin(),
					lcd.getD4Pin(),
					lcd.getD5Pin(),
					lcd.getD6Pin(),
					lcd.getD7Pin()
			));
			return;
		}
		if (context.get("pass") == PASS.TWO) {
			w(String.format("  %s.begin(16, 2); // LCD Screen\n", lcd.getName()));
		}
	}

	@Override
	public void visit(LCDAction lcdAction) {
		if (context.get("pass") == PASS.ONE) {
			return;
		}
		if (context.get("pass") == PASS.TWO) {

			LCDScreen screen = lcdAction.getScreen();
			String name = screen.getName();
			w(String.format("\t\t\t%s.setCursor(0, 0);\n", name));
			for (MessagePart part : lcdAction.getMessage()) {
				if (part instanceof ConstantText) {
					ConstantText t = (ConstantText) part;
					w(String.format("\t\t\t%s.print(\"%s\");\n",
							name, escape(t.getValue())));
				}

				if (part instanceof BrickValueRef) {
					BrickValueRef ref = (BrickValueRef) part;
					Brick brick = ref.getBrick();

					if (brick instanceof Sensor) {
						Sensor s = (Sensor) brick;
						w(String.format(
								"\t\t\t%s.print((digitalRead(%d) == HIGH ? \"HIGH\" : \"LOW\"));\n",
								name, s.getPin()
						));
					}

					if (brick instanceof Actuator) {
						Actuator a = (Actuator) brick;
						w(String.format(
								"\t\t\t%s.print((digitalRead(%d) == HIGH ? \"ON\" : \"OFF\"));\n",
								name, a.getPin()
						));
					}
				}
			}
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
		if(context.get("pass") == PASS.ONE) {
			return;
		}
		if(context.get("pass") == PASS.TWO) {
			String sensorName = transition.getSensor().getName();
			w(String.format("\t\t\t\t%sLastDebounceTime = millis();\n", sensorName));
		}
	}

    @Override
    public void visit(TransitionList transitionList) {
        List<String> sensorsName = transitionList.getTransitions().stream()
                .filter(SignalTransition.class::isInstance).map(
                        t -> ((SignalTransition) t).getSensor().getName()
                ).collect(Collectors.toList());
        for (String name : sensorsName) {
            w(String.format("\t\t\t%sBounceGuard = millis() - %sLastDebounceTime > debounce;\n",
                    name, name));
        }

        List<String> parts = transitionList.getTransitions().stream()
                .map(t -> String.format("(digitalRead(%d) == %s && %sBounceGuard)",
                        ((SignalTransition) t).getSensor().getPin(),
                        ((SignalTransition) t).getValue(),
                        ((SignalTransition) t).getSensor().getName()))
                .collect(Collectors.toList());

        String connector = transitionList.getConnector() == LOGIC.OR ? " || " : " && ";
        String condition = parts.size() > 1 ? "(" + String.join(connector, parts) + ")" : parts.get(0);
        w(String.format("\t\t\tif( %s ) {\n", condition));
        for (Transition transition : transitionList.getTransitions()) {
            transition.accept(this);
        }
        w("\t\t\t\tcurrentState = " + transitionList.getNext().getName() + ";\n");
        w("\t\t\t}\n");
		w("\t\t\tbreak;\n");
    }


	@Override
	public void visit(Action action) {
		if(context.get("pass") == PASS.ONE) {
			return;
		}
		if(context.get("pass") == PASS.TWO) {
			w(String.format("\t\t\tdigitalWrite(%d,%s);\n",action.getActuator().getPin(),action.getValue()));
			return;
		}
	}

}
