package network.aika.neuron.activation.visitor;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;

public interface VisitorTask {

    void processTask(ActVisitor v);

    void neuronTransition(ActVisitor v, Activation act);

    void synapseTransition(ActVisitor v, Synapse synapse, Link l);
}
