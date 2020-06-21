package network.aika.text;

import network.aika.Config;
import network.aika.Model;
import network.aika.Thought;
import network.aika.neuron.Neuron;
import network.aika.neuron.NeuronProvider;
import network.aika.neuron.activation.*;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;
import network.aika.neuron.inhibitory.InhibitoryNeuron;

/**
 * The {@code Document} class represents a single document which may be either used for processing a text or as
 * training input. A document consists of the raw text, the interpretations and the activations.
 *
 * @author Lukas Molzberger
 */
public class Document extends Thought {

    private final StringBuilder content;

    public Document(String content) {
        this(content, null);
    }

    public Document(String content, Config trainingConfig) {
        super(trainingConfig);
        this.content = new StringBuilder(content);
    }

    public void append(String txt) {
        content.append(txt);
    }

    public char charAt(int i) {
        return content.charAt(i);
    }

    public String getContent() {
        return content.toString();
    }

    public int length() {
        return content.length();
    }

    public String toString() {
        return content.toString();
    }

    private String getText(Integer begin, Integer end) {
        if(begin != null && end != null) {
            return content.substring(
                    Math.max(0, Math.min(begin, length())),
                    Math.max(0, Math.min(end, length()))
            );
        } else {
            return "";
        }
    }

    public static int[] getRange(Activation act) {
        return null;
    }

    public static String getText(Activation act) {
        int[] range = getRange(act);
        return ((Document)act.getThought()).getText(range[0], range[1]);
    }

    public Activation addInput(Neuron n, int begin, int end) {
        Activation act = new Activation(this, n);
        act.setReference(new GroundReference(begin, end));

        act.setValue(1.0);

        /*
        act.setFired(new Fired(input.inputTimestamp, input.fired));
        act.setRangeCoverage(input.rangeCoverage);

        input.getInputLinks()
                .stream()
                .map(iAct -> new Link(
                                getNeuron().getInputSynapse(iAct.getNeuronProvider()),
                                iAct,
                                this
                        )
                )
                .forEach(l -> addLink(l));
*/
        act.propagateInput();
        return act;
    }

    public Activation addInput(NeuronProvider n, int begin, int end) {
        return addInput(n.get(), begin, end);
    }


    public Activation addTokenActivation(TextModel m, Cursor c, String tokenLabel) {
        PatternNeuron tokenN = m.lookupTokenNeuron(tokenLabel);

        Activation tokenPatternAct = new Activation(this, tokenN);
        tokenPatternAct.setValue(1.0);
        tokenPatternAct.setFired(new Fired(0, 0)); // TODO
        tokenPatternAct.propagateInput();

        Activation prevRelAct = null;
        if(c.previousPatternAct != null) {
            PatternPartNeuron prevTokenN = m.;
            prevRelAct = new Activation(this, prevTokenN);

            prevRelAct.setValue(1.0);
            prevRelAct.setFired(new Fired(0, 0)); // TODO

            prevRelAct.addLink(
                    new Link(
                            prevRelAct.getNeuron().getInputSynapse(inhibNTAct.getNeuronProvider()),
                            inhibNTAct,
                            prevRelAct
                    )
            );

            prevRelAct.propagateInput();
        }

        Activation prevRelAct = n[1].propagate(this,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(0)
                        .setFired(0)
                        .addInputLink(tokenPatternAct)
        );

        Activation nextRelAct = n[2].propagate(this,
                new Activation.Builder()
                        .setValue(1.0)
                        .setInputTimestamp(0)
                        .setFired(0)
                        .addInputLink(previousPatternAct)
        );

        Activation inhibNTAct = lookupInhibRelAct(m.getNextTokenInhib(), nextRelAct);
        Activation inhibPTAct = lookupInhibRelAct(m.getPrevTokenInhib(), prevRelAct);

        prevRelAct.addLink(
                new Link(
                        prevRelAct.getNeuron().getInputSynapse(inhibNTAct.getNeuronProvider()),
                        inhibNTAct,
                        prevRelAct
                )
        );
        prevRelAct.linkForward();

        nextRelAct.addLink(
                new Link(
                        prevRelAct.getNeuron().getInputSynapse(inhibNTAct.getNeuronProvider()),
                        inhibPTAct,
                        nextRelAct
                )
        );
        nextRelAct.linkForward();

        processActivations();

        return tokenPatternAct;
    }

    private Activation lookupInhibRelAct(InhibitoryNeuron inhibN, Activation relPPAct) {
        if(relPPAct == null) {
            return null;
        }
        return relPPAct.getOutputLinks(inhibN.getProvider())
                .findAny()
                .map(l -> l.getOutput())
                .orElse(null);
    }


    public static class GroundReference implements Reference {
        private int begin;
        private int end;

        public GroundReference(int begin, int end) {
            this.begin = begin;
            this.end = end;
        }

        public int getBegin() {
            return begin;
        }

        public int getEnd() {
            return end;
        }
    }
}
