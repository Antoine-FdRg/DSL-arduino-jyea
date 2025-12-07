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

    private boolean hasSerialCommunication(App app) {
    boolean hasSerialMonitor = app.isUsingSerialMonitor();

    boolean hasSendAction = app.getStates().stream()
            .flatMap(state -> state.getActions().stream())
            .anyMatch(a -> a instanceof SendAction);

    return hasSerialMonitor || hasSendAction;
}


	private String escape(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	@Override
	public void visit(App app) {
		//first pass, create global vars
		context.put("pass", PASS.ONE);
		boolean hasSerial = hasSerialCommunication(app);
		context.put("hasSerial", hasSerial);
		w("// Wiring code generated from an ArduinoML model\n");
		w(String.format("// Application name: %s\n", app.getName()));

        if (hasSerial) {
            w("// Serial communication: 9600 baud (Standard Arduino Uno)\n");
        }
        w("\n");

		w("long debounce = 200;\n");
        w("long stateEnteredTime = 0;\n");
        w("int lastState = -1;\n");
		if (hasSerialCommunication(app)) {
			w("bool notPrint = true;\n");
		}
		w("enum STATE {");
		String sep ="";
		for(State state: app.getStates()){
			w(sep);
			state.accept(this);
			sep=", ";
		}
		w("};\n\n");
		if (app.getInitial() != null) {
			w("STATE currentState = " + app.getInitial().getName()+";\n");
		}

		for(Brick brick: app.getBricks()){
			brick.accept(this);
		}

		//second pass, setup and loop
		context.put("pass",PASS.TWO);
		w("\nvoid setup() {\n");

        if (hasSerialCommunication(app)) {
            w("    Serial.begin(9600);\n");
            w("    while(!Serial) { ; } // Wait for serial port\n");
        }

		for(Brick brick: app.getBricks()){
			brick.accept(this);
		}
		if (app.isUsingErrorState()) {
			w("    pinMode(12, OUTPUT); // Onboard LED for error blinking\n");
		}
		w("}\n");

        w("\nvoid loop() {\n");

        w("    if ((int)currentState != lastState) {\n");
        w("        stateEnteredTime = millis();\n");
        w("        lastState = (int)currentState;\n");
        w("    }\n\n");

        if (hasSerialCommunication(app)) {
            w("    String serialInput = \"\";\n");
            w("    if (Serial.available() > 0) {\n");
            w("        serialInput = Serial.readStringUntil('\\n');\n");
            w("        serialInput.trim();\n");
            w("    }\n\n");
        }

        w("    switch (currentState) {\n");

		for(State state: app.getStates()){
			state.accept(this);
		}
		w("    }\n" +
			"}");
		generateErrorBlink(app.isUsingErrorState());
	}

	private void generateErrorBlink(boolean useErrorState) {
		if (!useErrorState) {
			return;
		}
		w("\n\n" +
				"long blinkDuration = 200;\n" +
				"long pauseDuration = 900;\n" +
				"long currentBlinkNumber = 0;\n" +
				"boolean currentBlinkState = false;\n" +
				"boolean pauseBlink = false;\n" +
				"long currentBlinkDuration = 0;\n" +
				"\n" +
				"void errorBlink(long errorCode) {\n" +
				"    if (pauseBlink) {\n" +
				"        if (millis() - currentBlinkDuration > pauseDuration) {\n" +
				"            pauseBlink = false;\n" +
				"        }\n" +
				"        return;\n" +
				"    }\n" +
				"    if (millis() - currentBlinkDuration > blinkDuration) {\n" +
				"        currentBlinkDuration = millis();\n" +
				"        if (currentBlinkState) {\n" +
				"            digitalWrite(12, LOW);\n" +
				"            currentBlinkNumber++;\n" +
				"            if (currentBlinkNumber == errorCode) {\n" +
				"                currentBlinkNumber = 0;\n" +
				"                pauseBlink = true;\n" +
				"                currentBlinkDuration = millis();\n" +
				"            }\n" +
				"        } else {\n" +
				"            digitalWrite(12, HIGH);\n" +
				"        }\n" +
				"        currentBlinkState = !currentBlinkState;\n" +
				"    }\n" +
				"}\n");
	}

	@Override
	public void visit(Actuator actuator) {
		if(context.get("pass") == PASS.ONE) {
			return;
		}
		if(context.get("pass") == PASS.TWO) {
			w(String.format("    pinMode(%d, OUTPUT); // %s [Actuator]\n", actuator.getPin(), actuator.getName()));
			return;
		}
	}


	@Override
	public void visit(Sensor sensor) {
		if(context.get("pass") == PASS.ONE) {
			w(String.format("\nbool %sBounceGuard = false;\n", sensor.getName()));
			w(String.format("long %sLastDebounceTime = 0;\n", sensor.getName()));
		} else if(context.get("pass") == PASS.TWO) {
			w(String.format("    pinMode(%d, INPUT); // %s [Sensor]\n", sensor.getPin(), sensor.getName()));
			return;
		}
	}

	@Override
	public void visit(LCDScreen lcd) {
		if (context.get("pass") == PASS.ONE) {
			w("\n#include <LiquidCrystal.h>\n");
			w(String.format("LiquidCrystal %s(%d, %d, %d, %d, %d, %d, %d);\n",
					lcd.getName(),
					lcd.getRsPin(),
					lcd.getEnablePin(),
					lcd.getD4Pin(),
					lcd.getD5Pin(),
					lcd.getD6Pin(),
					lcd.getD7Pin(),
					lcd.getD8Pin()
			));
			return;
		}
		if (context.get("pass") == PASS.TWO) {
			w(String.format("    %s.begin(16, 2);\n", lcd.getName()));
		}
	}

	@Override
	public void visit(LCDAction lcdAction) {
		if (context.get("pass") == PASS.ONE) return;

		if (context.get("pass") == PASS.TWO) {

			LCDScreen screen = lcdAction.getScreen();
			String name = screen.getName();

			w(String.format("            %s.setCursor(0, 0);\n", name));
			w(String.format("            %s.print(\"                \");  // clear line 0\n", name));
			w(String.format("            %s.setCursor(0, 1);\n", name));
			w(String.format("            %s.print(\"                \");  // clear line 1\n", name));

			for (MessagePart part : lcdAction.getMessage()) {

				if (part instanceof ConstantText) {
					ConstantText t = (ConstantText) part;

					w(String.format("            %s.setCursor(0, 0);\n", name));
					w(String.format("            %s.print(\"%s\");\n", name, escape(t.getValue())));
				}
			}

			for (MessagePart part : lcdAction.getMessage()) {
				if (part instanceof BrickValueRef) {
					BrickValueRef ref = (BrickValueRef) part;
					Brick brick = ref.getBrick();

					w(String.format("            %s.setCursor(0, 1);\n", name));

					if (brick instanceof Sensor) {
						Sensor s = (Sensor) brick;
						w(String.format(
								"            %s.print((digitalRead(%d) == HIGH ? \"HIGH\" : \"LOW \"));\n",
								name, s.getPin()
						));
					}

					if (brick instanceof Actuator) {
						Actuator a = (Actuator) brick;
						w(String.format(
								"            %s.print((digitalRead(%d) == HIGH ? \"ON  \" : \"OFF \"));\n",
								name, a.getPin()
						));
					}
				}
			}
		}
	}

	@Override
    public void visit(SendAction action) {
        String message = action.getSerialMessage();

        if (message.startsWith("\"") && message.endsWith("\"")) {
            message = message.substring(1, message.length() - 1);
        }

        w("            if (notPrint) {\n");
        w("                Serial.println(\"" + message + "\");\n");
        w("                notPrint = false;\n");
        w("            }\n");
    }

    @Override
    public void visit(MessageTransition transition) {
        if (transition.isMatchAny()) {
            w("( serialInput.length() > 0)");
        } else {
            // Le pattern est stocké sans guillemets, il faut les ajouter pour la génération
            w("( serialInput == \"" + transition.getPattern() + "\" )");
        }
    }

    @Override
public void visit(State state) {
    if (context.get("pass") == PASS.ONE) {
        w(state.getName());
        return;
    }
    if (context.get("pass") == PASS.TWO) {
        w("        case " + state.getName() + ":\n");

        if (state.getName().startsWith("error_")) {
            w(String.format("            errorBlink(%s);\n", state.getName().substring(6)));
            w("            break;\n");
        } else {
            for (Action action : state.getActions()) {
                action.accept(this); 
            }

            state.getTransition().accept(this);

            w("\n            break;\n");
        }
    }
}


	@Override
	public void visit(SignalTransition transition) {
		// SignalTransition debounce update is handled in TransitionList visitor
		// to avoid duplication
	}

    @Override
    public void visit(TimeTransition transition) {
        // TimeTransition handling will be managed in TransitionList visitor
    }

@Override
public void visit(Transition transition) {
    // On part de la condition de la transition
    BooleanExpression expr = transition.getCondition();
    if (expr == null) {
        return;
    }

    // On force expr à être traitée comme un ConditionList
    ConditionList conditionList;
    if (expr instanceof ConditionList) {
        conditionList = (ConditionList) expr;
    } else {
        // On enveloppe une condition simple dans un ConditionList "AND"
        conditionList = new ConditionList();
        conditionList.setConnector(LOGIC.AND);
        conditionList.getExpressions().add(expr);
    }

    if (conditionList.getExpressions().isEmpty()) {
        return;
    }

    // On récupère les différentes formes de conditions
    List<SignalTransition> signalTransitions = conditionList.getExpressions().stream()
            .filter(SignalTransition.class::isInstance)
            .map(SignalTransition.class::cast)
            .collect(Collectors.toList());

    List<MessageTransition> serialTransitions = conditionList.getExpressions().stream()
            .filter(MessageTransition.class::isInstance)
            .map(MessageTransition.class::cast)
            .collect(Collectors.toList());

    List<TimeTransition> temporalTransitions = conditionList.getExpressions().stream()
            .filter(TimeTransition.class::isInstance)
            .map(TimeTransition.class::cast)
            .collect(Collectors.toList());

    // Gestion du debounce pour les capteurs
    List<String> sensorsName = signalTransitions.stream()
            .map(t -> t.getSensor().getName())
            .distinct()
            .collect(Collectors.toList());
    for (String name : sensorsName) {
        w(String.format("            %sBounceGuard = millis() - %sLastDebounceTime > debounce;\n",
                name, name));
    }

    // Construction de la condition (signaux + série)
    List<String> parts = signalTransitions.stream()
            .map(t -> String.format("(digitalRead(%d) == %s && %sBounceGuard)",
                    t.getSensor().getPin(),
                    t.getValue(),
                    t.getSensor().getName()))
            .collect(Collectors.toList());

    for (MessageTransition t : serialTransitions) {
        if (t.isMatchAny()) {
            parts.add("(serialInput.length() > 0)");
        } else {
            parts.add("(serialInput == \"" + t.getPattern() + "\")");
        }
    }

    String connector = conditionList.getConnector() == LOGIC.OR ? " || " : " && ";
    String condition = parts.size() > 1
            ? "(" + String.join(" " + connector + " ", parts) + ")"
            : (parts.isEmpty() ? "false" : parts.get(0));

    // IF principal pour signaux / série
    w(String.format("            if (%s) {\n", condition));

    for (String name : sensorsName) {
        w(String.format("                %sLastDebounceTime = millis();\n", name));
    }
    for (SignalTransition sTransition : signalTransitions) {
        sTransition.accept(this); // (aujourd’hui tu ne fais rien ici, c’est ok)
    }

    // Changement d'état : on utilise transition.getNext()
    if (transition.getNext() != null) {
        w("                currentState = " + transition.getNext().getName() + ";\n");
    }
    if ((Boolean) context.get("hasSerial")) {
        w("                notPrint = true;\n");
    }
    w("            }\n");

    // Transitions temporelles (after X ms)
    if (!temporalTransitions.isEmpty()) {
        for (TimeTransition tempTransition : temporalTransitions) {
            int delay = tempTransition.getDelay();
            w(String.format("            if (millis() - stateEnteredTime > %d) {\n", delay));
            if (transition.getNext() != null) {
                w("                currentState = " + transition.getNext().getName() + ";\n");
            }
            if ((Boolean) context.get("hasSerial")) {
                w("                notPrint = true;\n");
            }
            w("            }\n");
        }
    }
}



	@Override
	public void visit(SetAction setAction) {
		if(context.get("pass") == PASS.ONE) {
			return;
		}
		if(context.get("pass") == PASS.TWO) {
			w(String.format("            digitalWrite(%d, %s);\n", setAction.getActuator().getPin(), setAction.getValue()));
		}
	}

	@Override
	public void visit(ConditionList transitionList) {
		// Handled in Transition visitor
	}

	@Override
public void visit(ConstantText constantText) {
    // Rien à faire ici normalement

}

@Override
public void visit(BrickValueRef brickValueRef) {
    // pareil normalement
}
	

}
