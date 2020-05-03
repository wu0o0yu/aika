package network.aika.neuron.activation.linker;

import network.aika.neuron.Neuron;
import network.aika.neuron.PatternScope;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;


public class LTargetLink<S extends Synapse> extends LLink<S> {

    Boolean isRecurrent;
    Boolean isNegative;
    Boolean isPropagate;

    public LTargetLink(LNode input, LNode output, PatternScope patternScope, Class<S> synapseClass, String label, Boolean isRecurrent, Boolean isNegative, Boolean isPropagate) {
        super(input, output, patternScope, synapseClass, label);
        this.isRecurrent = isRecurrent;
        this.isNegative = isNegative;
        this.isPropagate = isPropagate;
    }

    public void follow(Mode m, Activation act, LNode from, Activation startAct) {
        Link existingLink = lookupExistingLink(act, from, startAct);
        if(existingLink != null) {
            return;
        }

        LNode to = getTo(from);

        Activation iAct = from == input ? act : startAct;
        Activation oAct = from == input ? startAct : act;

        Neuron in = iAct.getNeuron();
        Neuron on = oAct != null ? oAct.getNeuron() : null;

        Synapse s = on != null ? on.getInputSynapse(in, patternScope) : null;

        if(m == Mode.LINKING && (s == null || !matchSynapse(s))) {
            return;
        }

        if(s == null && m == Mode.INDUCTION) {
            try {
                s = synapseClass.getConstructor().newInstance();
                s.setInput(in);
                s.setOutput(on);
            } catch (Exception e) {
            }
        }

        if(oAct == null) {
            oAct = to.follow(m, on != null ? on.get() : null, oAct, this, startAct);
        }

        Link l = Link.link(s, iAct, oAct);
        act.getDocument().getLinker().addToQueue(l);
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
