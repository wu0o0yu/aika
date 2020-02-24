package network;


import network.aika.Config;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.ExcitatoryNeuron;
import network.aika.neuron.excitatory.pattern.PatternNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartNeuron;
import network.aika.neuron.excitatory.patternpart.PatternPartSynapse;
import network.aika.neuron.excitatory.pattern.PatternSynapse;
import network.aika.neuron.excitatory.patternpart.PositiveRecurrentSynapse;
import network.aika.neuron.excitatory.patternpart.PrimarySynapse;
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
    ExcitatoryNeuron relN;


    @Before
    public void init() {
        model = new Model();

        relN = new PatternPartNeuron(model, "Char-Relation");
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
                            .setRangeCoverage(1.0)
            );

            if(lastAct != null) {
                relN.addInput(doc,
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

        System.out.println(doc.activationsToString());

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


    @Test
    public void testTrainingDer() throws IOException {
        PatternNeuron derN = initDer();

        train("der ");

/*        for(String word: Util.loadExamplesAsWords(new File("/Users/lukas.molzberger/aika-ws/maerchen"))) {
            train( word + " ");
        }
*/
        model.dumpModel();
        System.out.println();
    }


    public PatternNeuron initDer() {
        PatternPartNeuron eD = new PatternPartNeuron(model, "TP-d");
        PatternPartNeuron eE = new PatternPartNeuron(model, "TP-e");
        PatternPartNeuron eR = new PatternPartNeuron(model, "TP-r");

        PatternNeuron derN = new PatternNeuron(model, "P-der");


        Neuron.init(eD, 1.0,
                new PrimarySynapse.Builder()
                        .setNeuron(lookupChar('d'))
                        .setWeight(10.0),
                new PositiveRecurrentSynapse.Builder()
                        .setNeuron(derN)
                        .setWeight(10.0)
        );

        Neuron.init(eE, 1.0,
                new PrimarySynapse.Builder()
                        .setNeuron(lookupChar('e'))
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setNeuron(eD)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setNeuron(relN)
                        .setWeight(10.0),
                new PositiveRecurrentSynapse.Builder()
                        .setNeuron(derN)
                        .setWeight(10.0)
        );

        Neuron.init(eR, 1.0,
                new PrimarySynapse.Builder()
                        .setNeuron(lookupChar('r'))
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setNeuron(eE)
                        .setWeight(10.0),
                new PatternPartSynapse.Builder()
                        .setNeuron(relN)
                        .setWeight(10.0),
                new PositiveRecurrentSynapse.Builder()
                        .setNeuron(derN)
                        .setWeight(10.0)
        );

        Neuron.init(derN, 1.0,
                new PatternSynapse.Builder()
                        .setNeuron(eD)
                        .setWeight(10.0),
                new PatternSynapse.Builder()
                        .setNeuron(eE)
                        .setWeight(10.0),
                new PatternSynapse.Builder()
                        .setNeuron(eR)
                        .setWeight(10.0)
        );

        return derN;
    }

}
