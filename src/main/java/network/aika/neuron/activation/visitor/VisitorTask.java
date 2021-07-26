package network.aika.neuron.activation.visitor;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Link;

public interface VisitorTask {

    void processTask(ActVisitor v);

    void transition(ActVisitor v, Synapse synapse, Link l);
}
