package org.aika.network;

import org.aika.ActivationFunction;
import org.aika.Model;
import org.aika.neuron.INeuron;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;
import org.aika.neuron.activation.Range;
import org.junit.Before;

import java.util.Map;
import java.util.TreeMap;

import static org.aika.ActivationFunction.RECTIFIED_LINEAR_UNIT_KEY;
import static org.aika.neuron.activation.Range.Relation.EQUALS;
import static org.aika.neuron.activation.Range.Relation.NONE;

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
    void init() {
        m = new Model();

        maleNameN = m.createNeuron("C-Male Name");
        Neuron.init(maleNameN, 0.0, RECTIFIED_LINEAR_UNIT_KEY, INeuron.Type.INHIBITORY);

        malePronounN = m.createNeuron("C-Male Pronoun");
        Neuron.init(malePronounN, 0.0, RECTIFIED_LINEAR_UNIT_KEY, INeuron.Type.INHIBITORY);

        femaleNameN = m.createNeuron("C-Female Name");
        Neuron.init(femaleNameN, 0.0, RECTIFIED_LINEAR_UNIT_KEY, INeuron.Type.INHIBITORY);

        femalePronounN = m.createNeuron("C-Female Pronoun");
        Neuron.init(femalePronounN, 0.0, RECTIFIED_LINEAR_UNIT_KEY, INeuron.Type.INHIBITORY);

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

}
