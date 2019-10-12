package network.aika.training.neuroninduction;

import network.aika.Document;
import network.aika.neuron.Neuron;
import network.aika.training.Config;
import network.aika.training.MetaModel;
import network.aika.training.TDocument;
import network.aika.training.excitatory.ExcitatoryNeuron;
import network.aika.training.input.InputNeuron;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static network.aika.neuron.INeuron.Type.INPUT;

public class MinimalExperiment {


    MetaModel model;

    String letters = " abcdefghijklmnopqrstuvwxyzäöüß";

    Map<Character, Neuron> inputLetters = new TreeMap<>();

    @Before
    public void init() {
        model = new MetaModel();

        TDocument doc = new TDocument(model, "");

        for(int i = 0; i < letters.length(); i++) {
            char c = letters.charAt(i);

            inputLetters.put(c, new InputNeuron(model, "" + c).getProvider());
        }

        doc.clearActivations();
    }


    private void train(String word, int j) {
        System.out.println(word);

        TDocument doc = new TDocument(model, word);

        for(int i = 0; i < doc.length(); i++) {
            char c = doc.charAt(i);
            inputLetters.get(c).addInput(doc, i, i + 1);
        }

        doc.process();

        System.out.println(doc.activationsToString());

        Config c = new Config()
                .setLearnRate(0.1)
                .setMaturityThreshold(10);

        if(j > c.getMaturityThreshold()) {
            doc.train(c);
        }

        doc.clearActivations();
    }


    @Test
    public void testTraining() {
        Document.CLEANUP_INTERVAL = 5000;

        String[] examples = new String[] {
                "der ",
                "die ",
                "das "
        };

        for(int i = 0; i < 40; i++) {
            for (String ex : examples) {
                train(ex, i);
            }
        }

        model.dumpModel();
        System.out.println();
    }
}
