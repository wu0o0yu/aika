package network.aika.neuron.inhibitory;


import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;

public class PrimaryInhibitorySynapse extends InhibitorySynapse {


    public PrimaryInhibitorySynapse() {
        super();
    }

    public PrimaryInhibitorySynapse(Neuron<?> input, InhibitoryNeuron output, Synapse template) {
        super(input, output, template);
    }

    @Override
    public Synapse instantiateTemplate(Neuron<?> input, InhibitoryNeuron output) {
        if(getInput() != input.getTemplate()) {
            return null;
        }

        return new PrimaryInhibitorySynapse(input, output, this);
    }

    public void transition(Visitor v, Activation nextAct, boolean create) {
        Visitor nv = v.prepareNextStep();
        nv.incrementPathLength();

        nv.scope = v.scope.getNext(v.downUpDir);

        nv.follow(nextAct);
    }
}
