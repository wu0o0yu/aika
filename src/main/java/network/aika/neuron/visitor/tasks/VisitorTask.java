package network.aika.neuron.visitor.tasks;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.visitor.ActVisitor;

public interface VisitorTask {

    void processTask(ActVisitor v);

    void neuronTransition(ActVisitor v, Activation act);

    void synapseTransition(ActVisitor v, Synapse synapse, Link l);
}
