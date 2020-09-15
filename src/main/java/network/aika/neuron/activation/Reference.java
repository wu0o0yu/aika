package network.aika.neuron.activation;

import network.aika.Thought;

public interface Reference {

    int getBegin();

    int getEnd();

    double length();

    Reference add(Reference ir);

    Thought getThought();
}
