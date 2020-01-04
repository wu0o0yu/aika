package network.aika.neuron;



public abstract class ConjunctiveSynapse<I extends TNeuron, O extends ConjunctiveNeuron> extends TSynapse<I, O>  {


    public ConjunctiveSynapse() {
        super();
    }

    public ConjunctiveSynapse(Neuron input, Neuron output, boolean recurrent, boolean propagate) {
        super(input, output, recurrent, propagate);
    }

    public ConjunctiveSynapse(Neuron input, Neuron output, boolean recurrent, boolean propagate, int lastCount) {
        super(input, output, recurrent, propagate, lastCount);
    }


    protected void addLinkInternal(INeuron in, INeuron out) {
        out.addInputSynapse(this);
    }


    protected void removeLinkInternal(INeuron in, INeuron out) {
        out.removeInputSynapse(this);
    }
}
