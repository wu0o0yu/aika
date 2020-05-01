package network.aika.neuron.activation.linker;

import network.aika.neuron.INeuron;
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

    public void follow(Mode m, Activation act, LNode from, Activation startAct, Linker.CollectResults c) {
        Link existingLink = lookupExistingLink(act, from, startAct);
        if(existingLink != null) {
            return;
        }

        Synapse ts = lookupTargetSynapse(from, startAct, act.getNeuron());

        if(ts != null && m == Mode.LINKING && matchSynapse(ts)) {
            c.collect(act, ts);
        } else if(ts == null && m == Mode.SYNAPSE_INDUCTION) {
            INeuron<?>[] ioN = lookupIONeuron(from, startAct, act.getNeuron());
            c.collect(act, ioN[1].createSynapse(ioN[0].getProvider(), patternScope, isRecurrent, isNegative));
        }
    }

    public boolean matchSynapse(Synapse ts) {
        if(patternScope != null && patternScope != ts.getPatternScope()) {
            return false;
        }

        if(isRecurrent != null && isRecurrent.booleanValue() != ts.isRecurrent()) {
            return false;
        }

        if(isNegative != null && isNegative.booleanValue() != ts.isNegative()) {
            return false;
        }
        return true;
    }

    public Synapse lookupTargetSynapse(LNode from, Activation startAct, Neuron n) {
        if(from == input) {
            return n.getOutputSynapse(startAct.getNeuron(), patternScope);
        } else {
            return n.getInputSynapse(startAct.getNeuron(), patternScope);
        }
    }

    public INeuron[] lookupIONeuron(LNode from, Activation startAct, Neuron n) {
        if(from == input) {
            return new INeuron[]{n.get(), startAct.getINeuron()};
        } else {
            return new INeuron[]{startAct.getINeuron(), n.get()};
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
