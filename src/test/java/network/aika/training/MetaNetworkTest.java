package network.aika.training;

import network.aika.ActivationFunction;
import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Range;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static network.aika.neuron.activation.Range.Operator.*;
import static network.aika.neuron.activation.Range.Relation.*;

public class MetaNetworkTest {


    Model model;

    public Neuron keyPhraseHint;
    public Neuron upperCaseN;
    public Neuron documentN;
    public Neuron wordSuppr;
    public Neuron phraseSuppr;
    public Neuron entitySuppr;
    public Neuron phraseMetaN;
    public Neuron entityMetaN;

    public Map<String, Neuron> neurons = new TreeMap<>();


    @Test
    public void testKeyPhraseRecognition() {
        String text = "Alan Smithee";

        Document doc = model.createDocument(text);

        parseWord(doc, 0, 5, "Alan");
        parseWord(doc, 5, 12, "Smithee");

        keyPhraseHint.addInput(doc, 0, 12);

        doc.process();
        MetaNetwork.train(doc);

        System.out.println(doc.activationsToString(true, true, true));

        Neuron wordAlan = neurons.get("W-alan");
        Neuron phraseAlanSmithee = selectOutputNeuron(wordAlan, "PHRASE");
        Neuron entityAlanSmithee = selectOutputNeuron(phraseAlanSmithee, "ENTITY");

        Assert.assertNotNull(entityAlanSmithee);

        System.out.println("Phrase Neuron Input Synapses:");
        for(Synapse s: phraseAlanSmithee.inMemoryInputSynapses.values()) {
            System.out.println(s.toString());
        }
        System.out.println();

        System.out.println("Entity Neuron Input Synapses:");
        for(Synapse s: entityAlanSmithee.inMemoryInputSynapses.values()) {
            System.out.println(s.toString());
        }
    }


    private Neuron selectOutputNeuron(Neuron n, String labelPrefix) {
        for(Synapse s: n.inMemoryOutputSynapses.values()) {
            if(s.output.getLabel().startsWith(labelPrefix)) {
                return s.output;
            }
        }
        return null;
    }


    @Before
    public void init() {
        model = new Model();

        keyPhraseHint = model.createNeuron("KEY-PHRASE-HINT");
        upperCaseN = model.createNeuron("UPPER CASE");
        documentN = model.createNeuron("DOCUMENT");

        wordSuppr = model.createNeuron("S-WORD");
        phraseSuppr = model.createNeuron("S-PHRASE");
        entitySuppr = model.createNeuron("S-ENTITY");

        phraseMetaN = model.createNeuron("M-PHRASE");
        entityMetaN = model.createNeuron("M-ENTITY");


        MetaNetwork.initMetaNeuron(phraseMetaN, 4.0, 6.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(keyPhraseHint)
                        .setWeight(40.0)
                        .setBias(-40.0)
                        .setRangeOutput(true),
                new MetaSynapse.Builder() // First word of the phrase
                        .setMetaWeight(20.0)
                        .setMetaBias(-20.0)
                        .setSynapseId(1)
                        .setNeuron(wordSuppr)
                        .setWeight(20.0)
                        .setBias(-20.0)
                        .addRangeRelation(BEGIN_EQUALS, 0)
                        .setRangeOutput(true, false),
                new MetaSynapse.Builder() // Words in the middle
                        .setMetaWeight(10.0)
                        .setMetaBias(-10.0)
                        .setSynapseId(3)
                        .setNeuron(wordSuppr)
                        .setWeight(0.0)
                        .setBias(0.0)
                        .addRangeRelation(BEGIN_TO_END_EQUALS, 0)
                        .setRangeOutput(false),
                new MetaSynapse.Builder() // Last word of the phrase
                        .setMetaWeight(20.0)
                        .setMetaBias(-20.0)
                        .setSynapseId(2)
                        .setNeuron(wordSuppr)
                        .setWeight(20.0)
                        .setBias(-20.0)
                        .addRangeRelation(BEGIN_TO_END_EQUALS, 1)
                        .addRangeRelation(END_EQUALS, 0)
                        .setRangeOutput(false, true),
                new MetaSynapse.Builder()
                        .setMetaWeight(-100.0)
                        .setMetaBias(0.0)
                        .setSynapseId(4)
                        .setNeuron(phraseSuppr)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(OVERLAPS, 1)
                        .addRangeRelation(OVERLAPS, 2)
                        .addRangeRelation(OVERLAPS, 3)
        );

        MetaNetwork.initMetaNeuron(entityMetaN, 5.0, 10.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(keyPhraseHint)
                        .setWeight(40.0)
                        .setBias(-40.0)
                        .setRangeOutput(true),
                new MetaSynapse.Builder()
                        .setMetaWeight(40.0)
                        .setMetaBias(-40.0)
                        .setSynapseId(1)
                        .setNeuron(phraseSuppr)
                        .setWeight(40.0)
                        .setBias(-40.0)
                        .setRangeOutput(true),
                new MetaSynapse.Builder()
                        .setMetaWeight(-100.0)
                        .setMetaBias(0.0)
                        .setSynapseId(2)
                        .setNeuron(entitySuppr)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .addRangeRelation(OVERLAPS, 1)
        );


        phraseSuppr.addSynapse(
                new MetaSynapse.Builder()
                        .setMetaWeight(1.0)
                        .setMetaBias(0.0)
                        .setSynapseId(0)
                        .setNeuron(phraseMetaN)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRangeOutput(true)
        );


        Neuron.init(wordSuppr, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT, INeuron.Type.INHIBITORY);

        Neuron.init(phraseSuppr, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT, INeuron.Type.INHIBITORY);

        Neuron.init(entitySuppr, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT, INeuron.Type.INHIBITORY,
                new MetaSynapse.Builder()
                        .setMetaWeight(1.0)
                        .setMetaBias(0.0)
                        .setSynapseId(0)
                        .setNeuron(entityMetaN)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRangeOutput(true)
        );
    }


    public Neuron lookupWordNeuron(String word) {
        return lookupNeuron(wordSuppr, word);
    }


    private Neuron lookupNeuron(Neuron suppr, String key) {
        Neuron n = neurons.get(key);
        if(n != null) {
            return n;
        }

        n = model.createNeuron(key);
        neurons.put(key, n);

        suppr.addSynapse(
                new Synapse.Builder()
                        .setSynapseId(neurons.size())
                        .setNeuron(n)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRangeOutput(true)
        );

        return n;
    }


    public void parseWord(Document doc, int begin, int end, String w) {
        Neuron inputNeuron = lookupWordNeuron("W-" + w.toLowerCase());
        if (inputNeuron != null) {
            inputNeuron.addInput(doc, begin, end);
        }

        if (Character.isUpperCase(w.charAt(0))) {
            upperCaseN.addInput(doc, begin, end);
        }
    }
}
