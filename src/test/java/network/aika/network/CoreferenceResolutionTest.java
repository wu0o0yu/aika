package network.aika.network;

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.INeuron;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static network.aika.neuron.INeuron.Type.INHIBITORY;
import static network.aika.neuron.activation.Range.Relation.EQUALS;
import static network.aika.neuron.activation.Range.Relation.NONE;


public class CoreferenceResolutionTest {

    String[][] pronouns = new String[][] {
            {"he", "him", "his"},
            {"she", "her", "hers"}
    };

    String[][] names = new String[][] {
            {"john", "robert", "richard", "mark"},
            {"linda", "lisa", "susan"}
    };


    Model m;

    Neuron maleNameN;
    Neuron malePronounN;
    Neuron femaleNameN;
    Neuron femalePronounN;

    Map<String, Neuron> dictionary = new TreeMap<>();


    @Before
    public void init() {
        m = new Model();

        maleNameN = m.createNeuron("C-Male Name");
        Neuron.init(maleNameN, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT, INHIBITORY);

        malePronounN = m.createNeuron("C-Male Pronoun");
        Neuron.init(malePronounN, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT, INHIBITORY);

        femaleNameN = m.createNeuron("C-Female Name");
        Neuron.init(femaleNameN, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT, INHIBITORY);

        femalePronounN = m.createNeuron("C-Female Pronoun");
        Neuron.init(femalePronounN, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT, INHIBITORY);

        addWords(pronouns[0], malePronounN);
        addWords(pronouns[1], femalePronounN);
        addWords(names[0], maleNameN);
        addWords(names[1], femalePronounN);


        Neuron maleCoref = m.createNeuron("Male Coreference");
        Neuron femaleCoref = m.createNeuron("Female Coreference");

        Neuron.init(maleCoref, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(malePronounN)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true, true),
                new Synapse.Builder()
                        .setNeuron(maleNameN)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeMatch(NONE)

        );
        Neuron.init(femaleCoref, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setNeuron(femalePronounN)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeMatch(EQUALS)
                        .setRangeOutput(true, true),
                new Synapse.Builder()
                        .setNeuron(femaleNameN)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeMatch(NONE)

        );
    }


    void addWords(String[] words, Neuron classN) {
        for(String word: words) {
            Neuron wordN = m.createNeuron("W-" + word);

            dictionary.put(word, wordN);

            if(classN != null) {
                classN.addSynapse(
                        new Synapse.Builder()
                                .setNeuron(wordN)
                                .setWeight(1.0)
                                .setRangeMatch(EQUALS)
                                .setRangeOutput(true)
                                .setRelativeRid(0)
                );
            }
        }
    }


    public Document parse(String txt) {
        Document doc = m.createDocument(txt);

        int i = 0;
        for(String word: txt.split(" ")) {
            int j = i + word.length();
            Neuron wn = dictionary.get(word);
            if(wn != null) {
                wn.addInput(doc, i, j);
            }

            i = j + 1;
        }

        doc.process();

        return doc;
    }


    @Test
    public void testCoref() {
        String txt = "john and lisa went to a sea park he saw dolphins lisa met richard he also visited the seapark";

        Document doc = parse(txt);

        System.out.println(doc.activationsToString(true, true, true));
    }

}
