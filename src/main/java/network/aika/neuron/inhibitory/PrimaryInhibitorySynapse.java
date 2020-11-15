package network.aika.neuron.inhibitory;


import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Visitor;

public class PrimaryInhibitorySynapse extends InhibitorySynapse {


    public PrimaryInhibitorySynapse() {
        super();
    }

    public PrimaryInhibitorySynapse(Neuron<?> input, InhibitoryNeuron output) {
        super(input, output);
    }

    @Override
    public boolean inductionRequired(Class<? extends Neuron> type) {
        return type == InhibitoryNeuron.class;
    }

    public void transition(Visitor v, Activation nextAct) {
        Visitor nv = v.prepareNextStep();
        nv.incrementPathLength();

        nv.scope = v.scope.getNext(v.downUpDir);

        nv.follow(nextAct);;
    }
}
