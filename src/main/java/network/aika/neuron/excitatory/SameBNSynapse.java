package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.visitor.Visitor;

import static network.aika.neuron.activation.direction.Direction.INPUT;
import static network.aika.neuron.activation.direction.Direction.OUTPUT;

public class SameBNSynapse<I extends Neuron<?>> extends BindingNeuronSynapse<I> {

    public SameBNSynapse(I input, BindingNeuron output, Synapse template) {
        super(input, output, template);
    }

    public SameBNSynapse(I input, BindingNeuron output, Synapse template, boolean isRecurrent) {
        super(input, output, template);

        this.isRecurrent = isRecurrent;
    }

    @Override
    public BindingNeuronSynapse instantiateTemplate(I input, BindingNeuron output) {
        assert input.getTemplates().contains(getInput());

        BindingNeuronSynapse s = new SameBNSynapse(input, output, this);
        initFromTemplate(s);
        return s;
    }

    public boolean checkTemplatePropagate(Visitor v, Activation act) {
        if (v.targetDir == INPUT)
            return !act.getNeuron().isInputNeuron() && isRecurrent;
        else if (v.targetDir == OUTPUT)
            return getOutput().computeBiasLB(act) >= 0.4;

        throw new IllegalStateException();
    }
}
