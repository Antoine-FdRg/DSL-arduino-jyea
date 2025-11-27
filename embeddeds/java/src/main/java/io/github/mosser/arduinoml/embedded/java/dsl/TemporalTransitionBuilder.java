package io.github.mosser.arduinoml.embedded.java.dsl;

import io.github.mosser.arduinoml.kernel.behavioral.TimeTransition;

public class TemporalTransitionBuilder {

    private TransitionListBuilder parent;
    private TimeTransition local;

    TemporalTransitionBuilder(TransitionListBuilder parent, int delayInMs) {
        this.parent = parent;
        this.local = new TimeTransition();
        this.local.setDelay(delayInMs);
        parent.addTransition(local);
    }

    public TransitionTableBuilder goTo(String state) {
        // Finalize the transition list with the accumulated transitions
        parent.getLocal().setTransitions(parent.getAccumulatedTransitions());
        parent.getLocal().setNext(parent.getParent().findState(state));
        if (parent.getAccumulatedTransitions().isEmpty()) {
            throw new IllegalStateException("No transitions defined for going to state: [" + state + "]\nHow to fix: Define at least one using waitFor(delayMs)");
        }
        return parent.getParent();
    }
}

