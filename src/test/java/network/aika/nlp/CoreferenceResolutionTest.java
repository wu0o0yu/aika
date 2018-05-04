package network.aika.nlp;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Range;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static network.aika.ActivationFunction.RECTIFIED_LINEAR_UNIT;
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
        Neuron.init(maleNameN, 0.0, RECTIFIED_LINEAR_UNIT, INHIBITORY);

        malePronounN = m.createNeuron("C-Male Pronoun");
        Neuron.init(malePronounN, 0.0, RECTIFIED_LINEAR_UNIT, INHIBITORY);

        femaleNameN = m.createNeuron("C-Female Name");
        Neuron.init(femaleNameN, 0.0, RECTIFIED_LINEAR_UNIT, INHIBITORY);

        femalePronounN = m.createNeuron("C-Female Pronoun");
        Neuron.init(femalePronounN, 0.0, RECTIFIED_LINEAR_UNIT, INHIBITORY);

        addWords(pronouns[0], malePronounN);
        addWords(pronouns[1], femalePronounN);
        addWords(names[0], maleNameN);
        addWords(names[1], femaleNameN);


        Neuron maleCoref = m.createNeuron("Male Coreference");
        Neuron femaleCoref = m.createNeuron("Female Coreference");

        Neuron corefInhib = m.createNeuron("Coref Inhib");

        Neuron.init(maleCoref, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(malePronounN)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(true, true)
                        .setIdentity(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(maleNameN)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .addRangeRelation(Range.Relation.NONE, 0)
                        .setIdentity(true),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(corefInhib)
                        .setWeight(-50.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Range.Relation.EQUALS, 0)
        );
        Neuron.init(femaleCoref, 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(femalePronounN)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRangeOutput(true, true)
                        .setIdentity(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(femaleNameN)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .addRangeRelation(Range.Relation.NONE, 0)
                        .setIdentity(true),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(corefInhib)
                        .setWeight(-50.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(Range.Relation.EQUALS, 0)
        );

        Neuron.init(corefInhib, 0.0, RECTIFIED_LINEAR_UNIT, INeuron.Type.INHIBITORY,
                new Synapse.Builder()
                        .setNeuron(maleCoref)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRangeOutput(true),
                new Synapse.Builder()
                        .setNeuron(femaleCoref)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRangeOutput(true)
        );
    }


    void addWords(String[] words, Neuron classN) {
        int i = 0;
        for(String word: words) {
            Neuron wordN = m.createNeuron("W-" + word);

            dictionary.put(word, wordN);

            if(classN != null) {
                classN.addSynapse(
                        new Synapse.Builder()
                                .setSynapseId(i++)
                                .setNeuron(wordN)
                                .setWeight(1.0)
                                .setRangeOutput(true)
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
        String txt = "john went jogging and lisa went swimming . he met her afterwards .";

        Document doc = parse(txt);

        System.out.println(doc.activationsToString(true, true, true));
    }


    @Test
    public void testCoref1() {
        String txt = "john richard robert susan he";

        Document doc = parse(txt);

        System.out.println(doc.activationsToString(true, true, true));
    }

}
