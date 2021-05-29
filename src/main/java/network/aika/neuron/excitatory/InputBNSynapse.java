package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.visitor.Visitor;

import static network.aika.neuron.activation.direction.Direction.OUTPUT;

public class InputBNSynapse<I extends Neuron<?>> extends BindingNeuronSynapse<I> {

    public InputBNSynapse(I input, BindingNeuron output) {
        super(input, output);
    }


    public boolean checkTemplatePropagate(Visitor v, Activation act) {
        return v.getTargetDir() == OUTPUT &&
                getOutput().computeBiasLB(act) >= 0.4;
    }

    @Override
    public BindingNeuronSynapse instantiateTemplate(I input, BindingNeuron output) {
        assert input.getTemplateGroup().contains(getInput());

        BindingNeuronSynapse s = new InputBNSynapse(input, output);
        initFromTemplate(s);
        return s;
    }
}
