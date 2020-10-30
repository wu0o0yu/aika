package network;

import network.aika.Config;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.Reference;
import network.aika.neuron.excitatory.ExcitatorySynapse;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.excitatory.PatternPartNeuron;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import network.aika.text.Document;
import network.aika.text.TextModel;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class ManuelInductionModel {

    public TextModel model;

    public ManuelInductionModel(TextModel model) {
        this.model = model;
    }

    private String trimPrefix(String l) {
        return l.substring(l.indexOf("-") + 1);
    }

    public void initToken(Document doc) {
        doc.setTrainingConfig(new Config() {

                    public boolean checkPatternPartNeuronInduction(Neuron n) {
                        return n.isInputNeuron();
                    }

                    public boolean checkInhibitoryNeuronInduction(Neuron n) {
                        return n.isInputNeuron() || n instanceof PatternNeuron;
                    }

                    public boolean checkPatternNeuronInduction(Activation act) {
                        return true;
                    }

                    public boolean checkSynapseInduction(Link l) {
                        Neuron n = l.getOutput().getNeuron();
                        if(n instanceof InhibitoryNeuron) {
                            return !n.isInputNeuron() || n instanceof PatternNeuron;
                        }

                        return true;
                    }

                    public String getLabel(Activation iAct, Neuron n) {
                        if(n instanceof PatternPartNeuron) {
                            return "TP-" + trimPrefix(iAct.getDescriptionLabel());
                        } else if(n instanceof PatternNeuron) {
                            return "P-" + doc.getContent();
                        } else {
                            return "I-" + trimPrefix(iAct.getDescriptionLabel());
                        }
                    }
                }
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setEnableInduction(true)
        );

        int i = 0;
        for(String t: doc.getContent().split(" ")) {
            int j = i + t.length();
            doc.processToken(model, i, j, t);

            i = j + 1;
        }
    }

}
