package network.aika;

import network.aika.neuron.Neuron;
import network.aika.neuron.Templates;
import network.aika.neuron.activation.Reference;
import network.aika.neuron.excitatory.*;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.inhibitory.InhibitorySynapse;
import network.aika.text.TextModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class DerDieDasTest {

    CharBasedTraining charBasedTrainings = new CharBasedTraining();

    Map<Character, InhibitoryNeuron> inhibNeurons = new TreeMap<>();

    @BeforeEach
    public void init() {
        charBasedTrainings.init();
    }

    public void initToken(Reference ref, String token) {
        TextModel m = charBasedTrainings.getModel();
        Templates t = m.getTemplates();

        PatternNeuron out = t.SAME_PATTERN_TEMPLATE.instantiateTemplate();
        out.setTokenLabel(token);
        out.setLabel("P-" + token);
        out.setBias(0.1);

        BindingNeuron prevPPN = null;

        for(int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);

            PatternNeuron inN = m.lookupToken(ref, "" + c);
            BindingNeuron ppN = t.SAME_BINDING_TEMPLATE.instantiateTemplate();
            ppN.setLabel("TP-" + c + "-(" + token + ")");

            initPP(ref, c, inN, ppN, prevPPN, out);

            {
                PatternSynapse s = t.PATTERN_SYNAPSE_TEMPLATE.instantiateTemplate(ppN, out);

                s.linkInput();
                s.linkOutput();
                s.addWeight(0.1);
                out.addConjunctiveBias(-0.1, false);
            }

            prevPPN = ppN;
        }
    }

    public void initPP(Reference ref, Character c, PatternNeuron inN, BindingNeuron ppN, BindingNeuron prevPP, PatternNeuron out) {
        TextModel m = charBasedTrainings.getModel();
        Templates t = m.getTemplates();

        InhibitoryNeuron inhibN = inhibNeurons.computeIfAbsent(c,
                ch ->
                {
                    InhibitoryNeuron n = t.INHIBITORY_TEMPLATE.instantiateTemplate();
                    n.setLabel("I-" + ch);
                    return n;
                }
        );

        {
            InhibitorySynapse s = new InhibitorySynapse(ppN, inhibN, null);

            s.linkInput();
            s.addWeight(0.1);
        }

        {
            BindingNeuronSynapse s = t.NEGATIVE_SYNAPSE_TEMPLATE.instantiateTemplate(inhibN, ppN);

            s.linkOutput();
            s.addWeight(-100.0);
        }

        {
            BindingNeuronSynapse s = t.SAME_PATTERN_SYNAPSE_TEMPLATE.instantiateTemplate(inN, ppN);

            s.linkInput();
            s.addWeight(0.1);
            ppN.addConjunctiveBias(-0.1, false);
        }

        if(prevPP != null) {
            BindingNeuronSynapse s = t.RELATED_INPUT_SYNAPSE_FROM_B_TEMPLATE.instantiateTemplate(prevPP, ppN);

            s.linkOutput();
            s.addWeight(0.1);
            ppN.addConjunctiveBias(-0.1, false);
        }

        BindingNeuron nextPP = lookupPPPT(inN);
        if(nextPP != null) {
            BindingNeuronSynapse s = t.RELATED_INPUT_SYNAPSE_FROM_B_TEMPLATE.instantiateTemplate(nextPP, ppN);

            s.linkOutput();
            s.addWeight(0.1);
            ppN.addConjunctiveBias(-0.1, false);
        }

        {
            BindingNeuronSynapse s = t.RECURRENT_SAME_PATTERN_SYNAPSE_TEMPLATE.instantiateTemplate(out, ppN);

            s.linkOutput();
            s.addWeight(0.1);
            ppN.addConjunctiveBias(-0.1, true);
        }
        ppN.setBias(0.1);
    }

    public BindingNeuron lookupPPPT(PatternNeuron pn) {
        return (BindingNeuron) pn.getOutputSynapses()
                .map(s -> s.getOutput())
                .filter(n -> isPTNeuron(n))
                .findAny()
                .orElse(null);
    }

    private boolean isPTNeuron(Neuron<?> n) {
        return n.getOutputSynapses()
                .map(s -> s.getOutput())
                .anyMatch(in -> in == charBasedTrainings.getModel().getPrevTokenInhib());
    }

    @Test
    public void train() throws InterruptedException {
        Random r = new Random(0);
        String[] trainData = {"der", "die", "das"};
        for(int i = 0; i < 1000; i++) {
            if(i == 100) {
                initToken(null, "der");
                initToken(null, "die");
                initToken(null, "das");
            }

            if(i == 110) {
                System.out.println(charBasedTrainings.getModel().statToString());
            }

            charBasedTrainings.train(trainData[r.nextInt(3)]);

            if(i == 110) {
                System.out.println(charBasedTrainings.getModel().statToString());
            }
        }
    }
}
