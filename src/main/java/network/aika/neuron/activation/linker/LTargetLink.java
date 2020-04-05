package network.aika.neuron.activation.linker;

import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;


public class LTargetLink extends LLink {


    public LTargetLink(LNode input, LNode output) {
        super(input, output);
    }


    public void follow(Activation act, LNode from, long v, Linker.CollectResults c) {

    }

    private boolean checkTargetSynapse(Synapse s, LNode from) {

    }
}
