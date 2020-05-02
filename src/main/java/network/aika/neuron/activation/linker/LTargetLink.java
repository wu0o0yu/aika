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
    Boolean isPropagate;

    public LTargetLink(LNode input, LNode output, PatternScope patternScope, String label, Boolean isRecurrent, Boolean isNegative, Boolean isPropagate) {
        super(input, output, patternScope, label);
        this.isRecurrent = isRecurrent;
        this.isNegative = isNegative;
        this.isPropagate = isPropagate;
    }

    public void follow(Mode m, Activation act, LNode from, Activation startAct) {
        Link existingLink = lookupExistingLink(act, from, startAct);
        if(existingLink != null) {
            return;
        }

        Activation iAct = from == input ? act : startAct;
        Activation oAct = from == input ? startAct : act;

        Neuron in = iAct.getNeuron();
        Neuron on = oAct.getNeuron();

        Synapse s = on.getInputSynapse(in, patternScope);

        if(m == Mode.LINKING && !matchSynapse(s)) {
            return;
        }

        if(s == null && m == Mode.SYNAPSE_INDUCTION) {
            s = on.get().createSynapse(in, patternScope, isRecurrent, isNegative);
        }

        if(s != null) {
            act.getDocument().getLinker().queue.add(Link.link(s, iAct, oAct));
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

        if(isPropagate != null && isPropagate.booleanValue() != ts.isPropagate()) {
            return false;
        }
        return true;
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
