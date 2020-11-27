package network.aika.neuron.excitatory;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;

public abstract class ExcitatorySynapse<I extends Neuron<?>, O extends ExcitatoryNeuron<?>> extends Synapse<I, O> {

    public ExcitatorySynapse() {
        super();
    }

    public ExcitatorySynapse(I input, O output, Synapse template) {
        super(input, output, template);
    }
}
