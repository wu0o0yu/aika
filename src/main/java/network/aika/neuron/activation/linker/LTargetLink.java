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
        Activation iAct = from == input ? act : startAct;
        Neuron in = iAct.getNeuron();

        LNode to = getTo(from);
        Activation oAct = startAct.lNode == to ? startAct : null;

        if(oAct != null) {
            if(iAct.outputLinks.get(oAct) != null) {
                return;
            }

            Neuron on = oAct.getNeuron();
            Synapse s = in.getOutputSynapse(on, patternScope);

            if(s == null) {
                if(m != Mode.INDUCTION) return;
                s = createSynapse(in, on);
            }

            Link.link(s, iAct, oAct);
        } else {
            if(m == Mode.LINKING) {
                in.getActiveOutputSynapses().stream()
                        .filter(s -> checkSynapse(s))
                        .forEach(s -> {
                            Activation oa = to.follow(m, s.getOutput(), null, this, startAct);
                            Link.link(s, iAct, oa);
                        });
            } else if(m == Mode.INDUCTION) {
                boolean exists = !iAct.outputLinks.values().stream()
                        .filter(l -> checkSynapse(l.getSynapse()))
                        .filter(l -> to.checkNeuron(l.getOutput().getINeuron()))
                        .findAny()
                        .isEmpty();

                if(!exists) {
                    oAct = to.follow(m, null, null, this, startAct);
                    Synapse s = createSynapse(in, oAct.getNeuron());
                    Link.link(s, iAct, oAct);
                }
            }
        }
    }

    public Synapse createSynapse(Neuron in, Neuron on) {
        try {
            Synapse s = synapseClass.getConstructor().newInstance();
            s.setInput(in);
            s.setOutput(on);

            s.link();
            return s;
        } catch (Exception e) {
        }
        return null;
    }

    protected boolean checkSynapse(Synapse s) {
        super.checkSynapse(s);

        if(isRecurrent != null && isRecurrent.booleanValue() != s.isRecurrent()) {
            return false;
        }

        if(isNegative != null && isNegative.booleanValue() != s.isNegative()) {
            return false;
        }

        if(isPropagate != null && isPropagate.booleanValue() != s.isPropagate()) {
            return false;
        }

        return true;
    }

    @Override
    public String getTypeStr() {
        return "T";
    }
}
