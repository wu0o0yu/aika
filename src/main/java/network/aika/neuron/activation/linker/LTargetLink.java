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

                try {
                    s = synapseClass.getConstructor().newInstance();
                    s.setInput(in);
                    s.setOutput(on);
                } catch (Exception e) {
                }
            }

            Link l = Link.link(s, iAct, oAct);
            act.getDocument().getLinker().addToQueue(l);
        } else {
            if(m == Mode.LINKING) {
                in.getActiveOutputSynapses().stream()
                        .filter(s -> checkSynapse(s))
                        .forEach(s -> {
                            Activation oa = to.follow(m, s.getOutput(), null, this, startAct);

                            Link l = Link.link(s, iAct, oa);
                            act.getDocument().getLinker().addToQueue(l);
                        });
            } else if(m == Mode.INDUCTION) {
                boolean exists = !iAct.outputLinks.values().stream()
                        .filter(l -> checkSynapse(l.getSynapse()))
                        .filter(l -> to.checkNeuron(l.getOutput().getINeuron()))
                        .findAny()
                        .isEmpty();

                if(!exists) {
                    Synapse s = null;
                    try {
                        s = synapseClass.getConstructor().newInstance();
                        s.setInput(in);
                    } catch (Exception e) {
                    }

                    Activation oa = to.follow(m, s.getOutput(), null, this, startAct);

                    if(s.getOutput() == null) {
                        s.setOutput(oa.getNeuron());
                        s.link();
                    }

                    Link l = Link.link(s, iAct, oa);
                    act.getDocument().getLinker().addToQueue(l);
                }
            }
        }
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
