package network.aika.neuron.activation.linker;

import network.aika.neuron.Neuron;
import network.aika.neuron.PatternScope;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;


public class LTargetLink extends LLink {

    Boolean isRecurrent;
    Boolean isNegative;

    public LTargetLink(LNode input, LNode output, PatternScope patternScope, String label, Boolean isRecurrent, Boolean isNegative) {
        super(input, output, patternScope, label);
        this.isRecurrent = isRecurrent;
        this.isNegative = isNegative;
    }

    public void follow(Activation act, LNode from, Activation startAct, Linker.CollectResults c) {
        Link existingLink = lookupExistingLink(act, from, startAct);
        if(existingLink != null) {
            return;
        }

        Synapse ts = lookupTargetSynapse(from, startAct, act.getNeuron());

        if(patternScope != null && patternScope != ts.getPatternScope()) {
            return;
        }

        if(isRecurrent != null && isRecurrent.booleanValue() != ts.isRecurrent()) {
            return;
        }

        if(isNegative != null && isNegative.booleanValue() != ts.isNegative()) {
            return;
        }

        c.collect(act, ts);
    }

    public Synapse lookupTargetSynapse(LNode from, Activation startAct, Neuron n) {
        if(from == input) {
            return n.getOutputSynapse(startAct.getNeuron(), patternScope);
        } else {
            return n.getInputSynapse(startAct.getNeuron(), patternScope);
        }
    }

    public Link lookupExistingLink(Activation act, LNode from, Activation startAct) {
        if(from == output) {
            return startAct.outputLinks.get(act);
        } else {
            return act.outputLinks.get(startAct);
        }
    }

    @Override
    public String getTypeStr() {
        return "T";
    }
}
