package network.aika.training.syllables;


import network.aika.Document;
import network.aika.neuron.Neuron;
import network.aika.training.Config;
import network.aika.training.MetaModel;
import network.aika.training.TDocument;
import network.aika.training.input.InputNeuron;
import network.aika.training.utils.Parser;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.Map;
import java.util.TreeMap;

import static network.aika.neuron.INeuron.Type.INPUT;


/**
 *
 * @author Lukas Molzberger
 */
public class SyllableExperiment {

    MetaModel model;

    String letters = " abcdefghijklmnopqrstuvwxyzäöüß";

    Map<Character, Neuron> inputLetters = new TreeMap<>();

    @Before
    public void init() {
        model = new MetaModel();

        Document doc = new Document(model, "");

        for(int i = 0; i < letters.length(); i++) {
            char c = letters.charAt(i);

            inputLetters.put(c, new InputNeuron(model, "" + c).getProvider());
        }

        doc.clearActivations();
    }


    private void train(String word) {

        TDocument doc = new TDocument(model, word);
        System.out.println("DocId:" + doc.getId() + "  " + word);

        for(int i = 0; i < doc.length(); i++) {
            char c = doc.charAt(i);
            inputLetters.get(c).addInput(doc, i, i + 1);
        }

        doc.process();
        doc.train(
                new Config()
                        .setLearnRate(0.025)
                        .setMetaThreshold(0.3)
                        .setMaturityThreshold(10)
        );


        doc.clearActivations();
    }


    @Test
    public void testTraining() throws IOException {
        Document.CLEANUP_INTERVAL = 5000;



        for(String word: Parser.loadExamplesAsWords(new File(System.getProperty("data.dir")))) {
        for(String word: Parser.loadExamplesAsWords(new File("C:/ws/aika-training/training/src/test/resources/maerchen"))) {
            train( word + " ");
        }

        model.dumpModel();
        System.out.println();
    }

}
