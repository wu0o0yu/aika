package org.aika.training;

import org.aika.ActivationFunction;
import org.aika.Document;
import org.aika.Model;
import org.aika.neuron.INeuron;
import org.aika.neuron.Neuron;
import org.aika.neuron.Synapse;
import org.aika.neuron.activation.Activation;
import org.aika.neuron.activation.Range;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.aika.neuron.activation.Range.Operator.*;
import static org.aika.neuron.activation.Range.Relation.OVERLAPS;

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

        parseWord(doc, 0, 5, 0, "Alan");
        parseWord(doc, 5, 12, 1, "Smithee");

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


        MetaNetwork.initMetaNeuron(model, phraseMetaN, 4.0, 6.0,
                new Synapse.Builder()
                        .setNeuron(keyPhraseHint)
                        .setWeight(40.0)
                        .setBias(-40.0)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true),
                new MetaSynapse.Builder() // First word of the phrase
                        .setMetaWeight(20.0)
                        .setMetaBias(-20.0)
                        .setMetaRelativeRid(true)
                        .setNeuron(wordSuppr)
                        .setWeight(20.0)
                        .setBias(-20.0)
                        .setRelativeRid(0)
                        .setRangeMatch(EQUALS, GREATER_THAN_EQUAL)
                        .setRangeOutput(false),
                new MetaSynapse.Builder() // Last word of the phrase
                        .setMetaWeight(20.0)
                        .setMetaBias(-20.0)
                        .setMetaRelativeRid(true)
                        .setNeuron(wordSuppr)
                        .setWeight(20.0)
                        .setBias(-20.0)
                        .setRelativeRid(null)
                        .setRangeMatch(LESS_THAN_EQUAL, EQUALS)
                        .setRangeOutput(false),
                new MetaSynapse.Builder() // Words in the middle
                        .setMetaWeight(10.0)
                        .setMetaBias(-10.0)
                        .setMetaRelativeRid(true)
                        .setNeuron(wordSuppr)
                        .setWeight(0.0)
                        .setBias(0.0)
                        .setRelativeRid(null)
                        .setRangeMatch(LESS_THAN, GREATER_THAN)
                        .setRangeOutput(false),
                new MetaSynapse.Builder()
                        .setMetaWeight(-100.0)
                        .setMetaBias(0.0)
                        .setNeuron(phraseSuppr)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRelativeRid(null)
                        .setRangeMatch(OVERLAPS)
        );

        MetaNetwork.initMetaNeuron(model, entityMetaN, 5.0, 10.0,
                new Synapse.Builder()
                        .setNeuron(keyPhraseHint)
                        .setWeight(40.0)
                        .setBias(-40.0)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true),
                new MetaSynapse.Builder()
                        .setMetaWeight(40.0)
                        .setMetaBias(-40.0)
                        .setNeuron(phraseSuppr)
                        .setWeight(40.0)
                        .setBias(-40.0)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true),
                new MetaSynapse.Builder()
                        .setMetaWeight(-100.0)
                        .setMetaBias(0.0)
                        .setNeuron(entitySuppr)
                        .setWeight(-100.0)
                        .setBias(0.0)
                        .setRecurrent(true)
                        .setRelativeRid(null)
                        .setRangeMatch(OVERLAPS)
        );


        phraseSuppr.addSynapse(
                new MetaSynapse.Builder()
                        .setMetaWeight(1.0)
                        .setMetaBias(0.0)
                        .setNeuron(phraseMetaN)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRelativeRid(0)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true)
        );


        Neuron.init(wordSuppr, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT_KEY, INeuron.Type.INHIBITORY);

        Neuron.init(phraseSuppr, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT_KEY, INeuron.Type.INHIBITORY);

        Neuron.init(entitySuppr, 0.0, ActivationFunction.RECTIFIED_LINEAR_UNIT_KEY, INeuron.Type.INHIBITORY,
                new MetaSynapse.Builder()
                        .setMetaWeight(1.0)
                        .setMetaBias(0.0)
                        .setNeuron(entityMetaN)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRelativeRid(0)
                        .setRangeMatch(Range.Relation.EQUALS)
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
                        .setNeuron(n)
                        .setWeight(1.0)
                        .setBias(0.0)
                        .setRelativeRid(0)
                        .setRangeMatch(Range.Relation.EQUALS)
                        .setRangeOutput(true)
        );

        return n;
    }


    public void parseWord(Document doc, int begin, int end, int wordCount, String w) {
        Neuron inputNeuron = lookupWordNeuron("W-" + w.toLowerCase());
        if (inputNeuron != null) {
            inputNeuron.addInput(doc, begin, end, wordCount);
        }

        if (Character.isUpperCase(w.charAt(0))) {
            upperCaseN.addInput(doc, begin, end, wordCount);
        }
    }
}
