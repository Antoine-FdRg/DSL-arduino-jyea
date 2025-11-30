package io.github.mosser.arduinoml.embedded.java.dsl;


import io.github.mosser.arduinoml.kernel.behavioral.SendAction;
import io.github.mosser.arduinoml.kernel.behavioral.State;

public class StateBuilder {

    AppBuilder parent;
    State local = new State();

    StateBuilder(AppBuilder parent, String name) {
        this.parent = parent;
        local.setName(name);
    }

    public InstructionBuilder setting(String sensorName) {
        return new InstructionBuilder(this, sensorName);
    }

    public StateBuilder sending(String message) {
        SendAction action = new SendAction();
        action.setMessage(message);
        local.getSendActions().add(action);
        return this;
    }

    public StateBuilder initial() { parent.theApp.setInitial(this.local); return this; }

    public AppBuilder endState() { parent.theApp.getStates().add(this.local); return parent; }

}