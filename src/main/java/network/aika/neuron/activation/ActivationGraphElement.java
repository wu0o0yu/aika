package network.aika.neuron.activation;

import network.aika.Thought;

public interface ActivationGraphElement extends Comparable<ActivationGraphElement> {

    void onProcessEvent();

    void afterProcessEvent();

    Thought getThought();
}
