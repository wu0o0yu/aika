package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.visitor.Visitor;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

public class SameBNSynapse<I extends Neuron<?>> extends BindingNeuronSynapse<I> {

    public SameBNSynapse(I input, BindingNeuron output) {
        super(input, output);
    }

    public SameBNSynapse(I input, BindingNeuron output, boolean isRecurrent) {
        super(input, output);

        this.isRecurrent = isRecurrent;
    }

    @Override
    public BindingNeuronSynapse instantiateTemplate(I input, BindingNeuron output) {
        assert input.getTemplateGroup().contains(getInput());

        BindingNeuronSynapse s = new SameBNSynapse(input, output);
        initFromTemplate(s);
        return s;
    }

    public boolean checkTemplatePropagate(Visitor v, Activation act) {
        if (v.getTargetDir() == INPUT)
            return !act.getNeuron().isInputNeuron() && isRecurrent;
        else if (v.getTargetDir() == OUTPUT)
            return getOutput().computeBiasLB(act) >= 0.4;

        throw new IllegalStateException();
    }
}
