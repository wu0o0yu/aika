package network.aika.neuron.activation.linker;

import network.aika.neuron.INeuron;
import network.aika.neuron.Neuron;
import network.aika.neuron.PatternScope;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;


public class LTargetLink extends LLink {


    public LTargetLink(LNode input, LNode output, PatternScope patternScope, String label) {
        super(input, output, patternScope, label);
    }

    public void follow(Activation act, LNode from, Activation startAct, long v, Linker.CollectResults c) {
        Link existingLink = lookupExistingLink(act, from, startAct);
        if(existingLink != null) {
            return;
        }

        Synapse ts = lookupTargetSynapse(from, startAct, act.getNeuron());

        if(patternScope != null && patternScope != ts.getPatternScope()) {
            return;
        }

        c.collect(act, ts);
    }

    public Synapse lookupTargetSynapse(LNode from, Activation startAct, Neuron n) {
        if(from == input) {
            return n.getOutputSynapse(startAct.getNeuron());
        } else {
            return n.getInputSynapse(startAct.getNeuron());
        }
    }

    public Link lookupExistingLink(Activation act, LNode from, Activation startAct) {
        if(from == output) {
            return startAct.outputLinks.get(act);
        } else {
            return act.outputLinks.get(startAct);
        }
    }
}
