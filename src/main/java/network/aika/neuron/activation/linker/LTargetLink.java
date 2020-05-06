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

             // TODO: synapse Induction

            }

            Link l = Link.link(s, iAct, oAct);
            act.getDocument().getLinker().addToQueue(l);
        } else {
            if(m == Mode.LINKING) {
                in.getActiveOutputSynapses().stream()
                        .filter(s -> checkSynapse(s))
                        .forEach(s -> {
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
                        });
            } else if(m == Mode.INDUCTION) {
                boolean exists = !iAct.outputLinks.values().stream()
                        .filter(l -> checkSynapse(l.getSynapse()))
                        .filter(l -> to.matchNeuron(l.getOutput()))
                        .findAny()
                        .isEmpty();


            }
        }


        if(m == Mode.LINKING && (s == null || !checkSynapse(s))) {
            return;
        }

        if(s == null && m == Mode.INDUCTION) {
            try {
                s = synapseClass.getConstructor().newInstance();
                s.setInput(in);
            } catch (Exception e) {
            }
        }

        oAct = to.follow(m, s.getOutput(), oAct, this, startAct);

        if(s.getOutput() == null) {
            s.setOutput(oAct.getNeuron());
            s.link();
        }

        Link l = Link.link(s, iAct, oAct);



    }

    protected boolean checkSynapse(Synapse ts) {
        if(synapseClass != null && synapseClass.equals(ts.getClass())) {
            return false;
        }

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

    @Override
    public String getTypeStr() {
        return "T";
    }
}
