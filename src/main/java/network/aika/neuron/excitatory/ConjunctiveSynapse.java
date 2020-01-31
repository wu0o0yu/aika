package network.aika.neuron.excitatory;


import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.TNeuron;
import network.aika.neuron.TSynapse;

public abstract class ConjunctiveSynapse<I extends TNeuron, O extends ConjunctiveNeuron> extends TSynapse<I, O> {


    public ConjunctiveSynapse() {
        super();
    }

    public ConjunctiveSynapse(Neuron input, Neuron output, boolean propagate, int lastCount) {
        super(input, output, propagate, lastCount);
    }


    protected void addLinkInternal(INeuron in, INeuron out) {
        out.addInputSynapse(this);
    }


    protected void removeLinkInternal(INeuron in, INeuron out) {
        out.removeInputSynapse(this);
    }
}
