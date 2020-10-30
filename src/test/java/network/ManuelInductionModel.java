package network;

import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
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

    Map<PatternNeuron, InhibitoryNeuron> inhibNeurons = new TreeMap<>(Comparator.comparing(n -> n.getId()));

    public ManuelInductionModel(TextModel model) {
        this.model = model;
    }

    public void initToken(Document doc) {
        String patternLabel = doc.getContent();

        PatternPartNeuron prevPPN = null;

        int i = 0;
        for(String t: doc.getContent().split(" ")) {
            int j = i + t.length();
            Activation tokenAct = doc.processToken(model, i, j, t);

            PatternNeuron inputTokenPattern = (PatternNeuron) tokenAct.getNeuron();

           // PatternPartNeuron ppN = new PatternPartNeuron(model, "TP-" + t + "-(" + patternLabel + ")", false);
            Activation tpAct = PatternPartNeuron.induce(tokenAct);
            PatternPartNeuron ppN = (PatternPartNeuron) tpAct.getNeuron();

            initPP(tokenAct.getReference(), inputTokenPattern, ppN, prevPPN, out);

            PatternNeuron.induce(tpAct);

            prevPPN = ppN;
            i = j + 1;
        }
    }

    public void initPP(Reference ref, PatternNeuron inputTokenPattern, PatternPartNeuron ppN, PatternPartNeuron prevPP, PatternNeuron out) {
        InhibitoryNeuron inhibN = inhibNeurons.computeIfAbsent(inputTokenPattern,
                ch ->
                {
                    InhibitoryNeuron n = new InhibitoryNeuron(model, "I-" + ch, false);
                    n.initInstance(ref);
                    return n;
                }
        );

        {
            InhibitorySynapse s = new InhibitorySynapse(ppN, inhibN);
            s.initInstance(ref);

            s.linkInput();
        }

        {
            ExcitatorySynapse s = new ExcitatorySynapse(inhibN, ppN, true, true, false, false);
            s.initInstance(ref);

            s.linkOutput();
            s.addWeight(-100.0);
        }

        {
            ExcitatorySynapse s = new ExcitatorySynapse(inputTokenPattern, ppN, false, false, true, false);
            s.initInstance(ref);

            s.linkInput();
        }

        if(prevPP != null) {
            ExcitatorySynapse s = new ExcitatorySynapse(prevPP, ppN, false, false, true, false);
            s.initInstance(ref);

            s.linkOutput();
        }

        PatternPartNeuron nextPP = lookupPPPT(inputTokenPattern);
        if(nextPP != null) {
            ExcitatorySynapse s = new ExcitatorySynapse(nextPP, ppN, false, false, true, false);
            s.initInstance(ref);

            s.linkOutput();
        }

        {
            ExcitatorySynapse s = new ExcitatorySynapse(out, ppN, false, true, false, false);
            s.initInstance(ref);

            s.linkOutput();
        }
    }

    public PatternPartNeuron lookupPPPT(PatternNeuron pn) {
        return (PatternPartNeuron) pn.getOutputSynapses()
                .map(s -> s.getOutput())
                .filter(n -> isPTNeuron(n))
                .findAny()
                .orElse(null);
    }

    private boolean isPTNeuron(Neuron<?> n) {
        return n.getOutputSynapses()
                .map(s -> s.getOutput())
                .anyMatch(in -> in == model.getPrevTokenInhib());
    }
}
