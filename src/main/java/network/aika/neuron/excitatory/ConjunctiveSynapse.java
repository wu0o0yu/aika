package network.aika.neuron.excitatory;


import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.TNeuron;

public abstract class ConjunctiveSynapse<I extends TNeuron, O extends ConjunctiveNeuron> extends Synapse<I, O> {


    public ConjunctiveSynapse() {
        super();
    }

    public ConjunctiveSynapse(Neuron input, Neuron output) {
        super(input, output);
    }


    protected void addLinkInternal(INeuron in, INeuron out) {
        out.addInputSynapse(this);
    }

    protected void removeLinkInternal(INeuron in, INeuron out) {
        out.removeInputSynapse(this);
    }
}
