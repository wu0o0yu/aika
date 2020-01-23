package network;


import network.aika.Config;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import network.aika.neuron.pattern.PatternNeuron;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.Map;
import java.util.TreeMap;


/**
 *
 * @author Lukas Molzberger
 */
public class SyllableExperiment {

    Model model;

    Map<Character, PatternNeuron> inputLetters = new TreeMap<>();
    ExcitatoryNeuron relNeuron;


    @Before
    public void init() {
        model = new Model();

        relNeuron = new ExcitatoryNeuron(model, "Char-Relation");
    }


    public PatternNeuron lookupChar(Character c) {
        PatternNeuron n = inputLetters.get(c);
        if(n == null) {
            n = new PatternNeuron(model, "" + c);
            inputLetters.put(c, n);
        }
        return n;
    }


    private void train(String word) {
        Document doc = new Document(model, word);
        System.out.println("DocId:" + doc.getId() + "  " + word);

        Activation lastAct = null;
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.charAt(i);

            Activation currentAct = lookupChar(c).addInput(doc,
                    new Activation.Builder()
                            .setInputTimestamp(i)
                            .setFired(0)
                            .setValue(1.0)
            );

            if(lastAct != null) {
                relNeuron.addInput(doc,
                        new Activation.Builder()
                            .setInputTimestamp(i)
                            .setFired(0)
                            .setValue(1.0)
                            .addInputLink(lastAct)
                            .addInputLink(currentAct)
                );
            }

            lastAct = currentAct;
        }

        doc.train(
                new Config()
                        .setLearnRate(0.025)
                        .setMetaThreshold(0.3)
                        .setMaturityThreshold(10)
        );
    }


    @Test
    public void testTraining() throws IOException {
        for(String word: Util.loadExamplesAsWords(new File("/Users/lukas.molzberger/aika-ws/maerchen"))) {
            train( word + " ");
        }

        model.dumpModel();
        System.out.println();
    }

}
