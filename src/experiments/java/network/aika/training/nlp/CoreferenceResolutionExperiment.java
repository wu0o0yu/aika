package network.aika.training.nlp;

import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.link.Link;
import network.aika.neuron.relation.Relation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static network.aika.ActivationFunction.RECTIFIED_LINEAR_UNIT;
import static network.aika.neuron.INeuron.Type.*;
import static network.aika.neuron.relation.Relation.EQUALS;
import static network.aika.neuron.relation.Relation.ANY;


public class CoreferenceResolutionExperiment {

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
    Neuron maleCoRef;
    Neuron femaleCoRef;

    Map<String, Neuron> dictionary = new TreeMap<>();


    @Before
    public void init() {
        m = new Model();

        maleNameN = m.createNeuron("C-Male Name", INHIBITORY, RECTIFIED_LINEAR_UNIT);
        malePronounN = m.createNeuron("C-Male Pronoun", INHIBITORY, RECTIFIED_LINEAR_UNIT);
        femaleNameN = m.createNeuron("C-Female Name", INHIBITORY, RECTIFIED_LINEAR_UNIT);

        femalePronounN = m.createNeuron("C-Female Pronoun", INHIBITORY, RECTIFIED_LINEAR_UNIT);

        addWords(pronouns[0], malePronounN);
        addWords(pronouns[1], femalePronounN);
        addWords(names[0], maleNameN);
        addWords(names[1], femaleNameN);


        maleCoRef = m.createNeuron("Male Coreference", EXCITATORY);
        femaleCoRef = m.createNeuron("Female Coreference", EXCITATORY);

        Neuron corefInhib = m.createNeuron("Coref Inhib", INHIBITORY, RECTIFIED_LINEAR_UNIT);

        Neuron.init(maleCoRef,
                5.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(malePronounN)
                        .setWeight(30.0)
                        .setIdentity(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(maleNameN)
                        .setWeight(30.0)
//                        .setDistanceFunction(DistanceFunction.DEGRADING)
                        .setIdentity(true),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(corefInhib)
                        .setWeight(-100.0)
                        .setRecurrent(true),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(ANY),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(0)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(EQUALS)
        );
        Neuron.init(femaleCoRef,
                5.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(femalePronounN)
                        .setWeight(30.0)
                        .setIdentity(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(femaleNameN)
                        .setWeight(30.0)
//                        .setDistanceFunction(DistanceFunction.DEGRADING)
                        .setIdentity(true),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(corefInhib)
                        .setWeight(-100.0)
                        .setRecurrent(true),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(ANY),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(0)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(EQUALS)
        );

        Neuron.init(corefInhib,
                0.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(maleCoRef)
                        .setWeight(1.0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(femaleCoRef)
                        .setWeight(1.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(EQUALS)
        );
    }


    void addWords(String[] words, Neuron classN) {
        for(String word: words) {
            Neuron wordN = m.createNeuron("W-" + word, INPUT);

            dictionary.put(word, wordN);

            if(classN != null) {
                int synId = wordN.getNewSynapseId();
                Neuron.init(classN,
                        new Synapse.Builder()
                                .setSynapseId(synId)
                                .setNeuron(wordN)
                                .setWeight(1.0),
                        new Relation.Builder()
                                .setFrom(synId)
                                .setTo(Synapse.OUTPUT)
                                .setRelation(EQUALS)
                );
            }
        }
    }


    public Document parse(String txt) {
        Document doc = new Document(m, txt);

        int i = 0;
        for(String word: txt.split(" ")) {
            int j = i + word.length();
            Neuron wn = dictionary.get(word);
            if(wn != null) {
                wn.addInput(doc, i, j + 1);
            }

            i = j + 1;
        }

        doc.process();

        return doc;
    }


    @Ignore
    @Test
    public void testCoref() {
        String txt = "john richard robert susan he";

        Document doc = parse(txt);

        System.out.println(doc.activationsToString());

        boolean found = false;
        for(Link l: maleCoRef
                .getActivation(doc, 26, 29, true)
                .getInputLinks()
                .collect(Collectors.toList())
                ) {
            if(l.getInput().getText().equalsIgnoreCase("robert ")) found = true;

            Assert.assertFalse(l.getInput().getText().equalsIgnoreCase("john "));
            Assert.assertFalse(l.getInput().getText().equalsIgnoreCase("richard "));
            Assert.assertFalse(l.getInput().getText().equalsIgnoreCase("susan "));
        }

        Assert.assertTrue(found);
    }


    @Test
    public void testCoref1() {
        String txt = "john went jogging and lisa went swimming . he met her afterwards .";

        Document doc = parse(txt);

        System.out.println(doc.activationsToString());
    }
}
