package network.aika.training.syllables;


import network.aika.neuron.Neuron;
import network.aika.neuron.activation.search.SearchNode;
import network.aika.training.Config;
import network.aika.training.MetaModel;
import network.aika.training.TDocument;
import network.aika.training.excitatory.ExcitatoryNeuron;
import network.aika.training.meta.MetaNeuron;
import network.aika.training.meta.MetaSynapse;
import network.aika.training.utils.Parser;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static network.aika.neuron.relation.Relation.*;


public class ExtractSyllablesExperiment {

    public static String LETTERS = "abcdefghijklmnopqrstuvwxyzäöüß";

    public MetaModel model;

    public MetaNeuron letter;
    public MetaNeuron syllable;

    public Map<Character, ExcitatoryNeuron> inputLetters = new HashMap<>();


    @Before
    public void init() {
        model = new MetaModel();

        letter = model.createMetaNeuron("LETTER");
        syllable = model.createMetaNeuron("SYLLABLE");


        TDocument initDoc = new TDocument(model, "");
        for (char c: LETTERS.toCharArray()) {
            inputLetters.put(c, letter.createMetaNeuronTargetFromLabel(initDoc, "" + c, null));
        }
        initDoc.clearActivations();

        model.initMetaNeuron(syllable, 0.3, 1.0, EQUALS,
                new MetaSynapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(letter.getInhibitoryNeuron())
                        .setWeight(5.0)
        );
    }


    public void processCharacters(TDocument doc) {
        for(int pos = 0; pos < doc.length(); pos++) {
            char c = doc.charAt(pos);

            Neuron inputLetter = inputLetters.get(Character.toLowerCase(c)).getProvider();
            if(inputLetter != null) {
                inputLetter.addInput(doc, pos, pos + 1);
            }
        }
    }


    @Test
    public void extractSyllables() throws IOException {
        SearchNode.OPTIMIZE_SEARCH = false;
        SearchNode.COMPUTE_SOFT_MAX = true;

        List<String> inputs = Parser.loadExamplesAsWords(new File(System.getProperty("data.dir")));

        for(int round = 0; round < 1; round++) {
            for (String txt : inputs) {
                System.out.print(txt + "   ");
                {
                    TDocument doc = new TDocument(model, txt.toLowerCase());

                    processCharacters(doc);

                    doc.process();

//                    System.out.println(doc.activationsToString());
                    doc.train(
                            new Config()
                                    .setLearnRate(0.5)
                    );

                    doc.clearActivations();
                }
            }
        }
    }
}
