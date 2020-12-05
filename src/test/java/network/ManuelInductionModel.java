package network;

import network.aika.Config;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Direction;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.Visitor;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.excitatory.PatternPartNeuron;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.PrimaryInhibitorySynapse;
import network.aika.neuron.phase.activation.ActivationPhase;
import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.text.TextReference;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import static network.aika.neuron.Sign.POS;
import static network.aika.neuron.phase.activation.ActivationPhase.*;
import static network.aika.neuron.phase.activation.ActivationPhase.FINAL;

public class ManuelInductionModel {

    public TextModel model;

    public ManuelInductionModel(TextModel model) {
        this.model = model;
    }

    private String trimPrefix(String l) {
        return l.substring(l.indexOf("-") + 1);
    }

    public void initToken(Document doc, boolean activateInduction) {
        ActivationPhase[] phaseswT = new ActivationPhase[]{
                PREPARE_FINAL_LINKING,
                SOFTMAX,
                COUNTING,
                TRAINING,
                GRADIENTS,
                UPDATE_SYNAPSE_INPUT_LINKS,
                TEMPLATE,
                FINAL
        };
        ActivationPhase[] phaseswoT = new ActivationPhase[]{
                PREPARE_FINAL_LINKING,
                SOFTMAX,
                COUNTING,
                FINAL
        };

        ActivationPhase[] phasesInducedAct = new ActivationPhase[] {
                PREPARE_FINAL_LINKING,
                SOFTMAX,
                COUNTING,
//                TRAINING,
                INDUCTION,
                GRADIENTS,
                UPDATE_SYNAPSE_INPUT_LINKS,
                FINAL
        };

        doc.setConfig(new Config() {

                    public Set<ActivationPhase> getPhases(ActivationPhase startPhase) {
                        TreeSet<ActivationPhase> phases = new TreeSet<>(Comparator.comparing(p -> p.getRank()));
                        if(startPhase != TEMPLATE) {
                            phases.addAll(Arrays.asList(activateInduction ? phaseswT : phaseswoT));
                        } else {
                            phases.addAll(Arrays.asList(phasesInducedAct));
                        }

                        return phases;
                    }

                    public boolean checkNeuronInduction(Activation act) {
                        Neuron n = act.getNeuron();

                        if(n instanceof PatternPartNeuron && n.isInputNeuron()) {
                            return false;
                        }

                        if(n instanceof PatternNeuron && n.getSurprisal(POS) < 1.4) {
                            return false;
                        }

                        return true;
                    }

                    public boolean checkSynapseInduction(Link l, Visitor v) {
                        System.out.println(v);
                        System.out.println();

                        Neuron outN = l.getOutput().getNeuron();
                        Neuron inN = l.getInput().getNeuron();

                        if(outN instanceof InhibitoryNeuron) {
                            return l.getSynapse() instanceof PrimaryInhibitorySynapse || v.scope == Direction.SAME;
//                            return !outN.isInputNeuron() && inN instanceof PatternNeuron;
                        }

                        return true;
                    }

                    public String getLabel(Activation iAct, Neuron n) {
                        if(n instanceof PatternPartNeuron) {
                            return "PP-" + trimPrefix(iAct.getDescriptionLabel());
                        } else if (n instanceof PatternNeuron) {
                            return "P-" + doc.getContent();
                        } else {
                            return "I-" + trimPrefix(iAct.getDescriptionLabel());
                        }
                    }
                }
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setSurprisalInductionThreshold(0.0)
                        .setGradientInductionThreshold(0.0)
        );

        int i = 0;
        TextReference lastRef = null;
        for(String t: doc.getContent().split(" ")) {
            int j = i + t.length();
            lastRef = doc.processToken(model, lastRef, i, j, t).getReference();

            i = j + 1;
        }
    }

}
