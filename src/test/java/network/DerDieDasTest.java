package network;

import network.aika.neuron.Neuron;
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

    public void initToken(String token) {
        TextModel m = charBasedTrainings.getModel();
        PatternNeuron out = new PatternNeuron(m, token, "P-" + token, false);
        out.setBias(0.1);

        PatternPartNeuron prevPPN = null;

        for(int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);

            PatternNeuron inN = m.lookupToken("" + c);
            PatternPartNeuron ppN = new PatternPartNeuron(m, "TP-" + c + "-(" + token + ")", false);

            initPP(c, inN, ppN, prevPPN, out);

            {
                ExcitatorySynapse s = new ExcitatorySynapse(ppN, out);

                s.linkInput();
                s.linkOutput();
                s.addWeight(0.1);
                out.addConjunctiveBias(-0.1, false);
            }

            prevPPN = ppN;
        }
    }

    public void initPP(Character c, PatternNeuron inN, PatternPartNeuron ppN, PatternPartNeuron prevPP, PatternNeuron out) {
        InhibitoryNeuron inhibN = inhibNeurons.computeIfAbsent(c,
                ch ->
                        new InhibitoryNeuron(charBasedTrainings.getModel(), "I-" + ch, false)
        );

        {
            InhibitorySynapse s = new InhibitorySynapse(ppN, inhibN);

            s.linkInput();
            s.addWeight(0.1);
        }

        {
            NegativeRecurrentSynapse s = new NegativeRecurrentSynapse(inhibN, ppN);

            s.linkOutput();
            s.addWeight(-100.0);
        }

        {
            ExcitatorySynapse s = new ExcitatorySynapse(inN, ppN);

            s.linkInput();
            s.addWeight(0.1);
            ppN.addConjunctiveBias(-0.1, false);
        }

        if(prevPP != null) {
            ExcitatorySynapse s = new ExcitatorySynapse(prevPP, ppN);

            s.linkOutput();
            s.addWeight(0.1);
            ppN.addConjunctiveBias(-0.1, false);
        }

        if(prevPP != null) {
            ExcitatorySynapse s = new ExcitatorySynapse(lookupPPPT(inN), ppN);

            s.linkOutput();
            s.addWeight(0.1);
            ppN.addConjunctiveBias(-0.1, false);
        }

        {
            PositiveRecurrentSynapse s = new PositiveRecurrentSynapse(out, ppN);

            s.linkOutput();
            s.addWeight(0.1);
            ppN.addConjunctiveBias(-0.1, true);
        }
        ppN.setBias(0.1);
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
                .anyMatch(in -> in == charBasedTrainings.getModel().getPrevTokenInhib());
    }

    @Test
    public void train() {
        Random r = new Random(0);
        String[] trainData = {"der", "die", "das"};
        for(int i = 0; i < 1000; i++) {
            if(i == 100) {
                initToken("der");
                initToken("die");
                initToken("das");
            }

            if(i == 110) {
                Neuron.ADJUST_GRADIENT = true;
                System.out.println(charBasedTrainings.getModel().statToString());
            }

            charBasedTrainings.train(trainData[r.nextInt(3)]);

            if(i == 110) {
                System.out.println(charBasedTrainings.getModel().statToString());
            }
        }
    }
}
