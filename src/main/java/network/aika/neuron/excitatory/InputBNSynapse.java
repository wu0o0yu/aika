package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.visitor.Visitor;

import static network.aika.neuron.activation.direction.Direction.OUTPUT;

public class InputBNSynapse<I extends Neuron<?>> extends BindingNeuronSynapse<I> {

    public boolean checkTemplatePropagate(Visitor v, Activation act) {
        return v.getTargetDir() == OUTPUT &&
                getOutput().computeBiasLB(act) >= 0.4;
    }
}
