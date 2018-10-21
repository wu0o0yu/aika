package network.aika.network;


import network.aika.Document;
import network.aika.Model;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.INeuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.range.Range;
import network.aika.neuron.relation.Relation;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static network.aika.neuron.relation.AncestorRelation.Type.COMMON_ANCESTOR;
import static network.aika.neuron.relation.AncestorRelation.Type.CONTAINED_IN;
import static network.aika.neuron.range.Position.Operator.*;


public class RelationsTest {


    @Test
    public void testRangeRelation() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");


        INeuron outC = Neuron.init(m.createNeuron("C"),
                5.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setBias(-10.0)
                        .setRangeOutput(Range.Output.DIRECT),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setBias(-10.0)
                        .setRangeOutput(Range.Output.DIRECT),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRangeRelation(Range.Relation.EQUALS)
        ).get();


        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inA.addInput(doc, 0, 1);
        inB.addInput(doc, 0, 1);

        Activation outC1 = outC.getActivation(doc, new Range(doc, 0, 1), false);

        System.out.println(doc.activationsToString(false, false, true));

        Assert.assertNotNull(outC1);
    }


    @Test
    public void testInstanceRelation() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");

        INeuron inB = Neuron.init(m.createNeuron("B"),
                5.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setIdentity(true)
                        .setBias(-10.0)
                        .setRangeOutput(Range.Output.DIRECT)
        ).get();

        INeuron outC = Neuron.init(m.createNeuron("C"),
                5.0,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setBias(-10.0)
                        .setRangeOutput(Range.Output.DIRECT),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB.provider)
                        .setWeight(10.0)
                        .setRecurrent(false)
                        .setBias(-10.0)
                        .setRangeOutput(Range.Output.DIRECT),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setAncestorRelation(CONTAINED_IN)
        ).get();


        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inA.addInput(doc, 0, 1);

        Activation outC1 = outC.getActivation(doc, new Range(doc, 0, 1), false);

        System.out.println(doc.activationsToString(false, false, true));

        Assert.assertNotNull(outC1);
    }


    @Test
    public void testABCPattern() {
        Model m = new Model();

        Neuron inA = m.createNeuron("A");
        Neuron inB = m.createNeuron("B");
        Neuron inC = m.createNeuron("C");

        INeuron outD = Neuron.init(m.createNeuron("D"),
                0.001,
                INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0)
                        .setRangeOutput(Range.Mapping.BEGIN, Range.Mapping.NONE),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0)
                        .setRangeOutput(Range.Output.NONE),
                new Synapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inC)
                        .setWeight(1.0)
                        .setRecurrent(false)
                        .setBias(-1.0)
                        .setRangeOutput(Range.Mapping.NONE, Range.Mapping.END),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRangeRelation(Range.Relation.END_TO_BEGIN_EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(2)
                        .setRangeRelation(Range.Relation.END_TO_BEGIN_EQUALS)
        ).get();

        Document doc = m.createDocument("aaaaaaaaaa", 0);

        inA.addInput(doc, 0, 1);
        inB.addInput(doc, 1, 2);
        inC.addInput(doc, 2, 3);

        Activation outD1 = outD.getActivation(doc, new Range(doc, 0, 3), false);

        Assert.assertNotNull(outD1);
    }


    @Test
    public void testHuettenheim() {
        Model m = new Model();

        HashMap<Character, Neuron> chars = new HashMap<>();
        for (char c = 'a'; c <= 'z'; c++) {
            Neuron rec = m.createNeuron("IN-" + c);
            chars.put(c, rec);
        }

        String word = "Huettenheim";


        List<Neuron.Builder> inputs = new ArrayList<>();
        for (int i = 0; i < word.length(); i++) {
            char c = word.toLowerCase().charAt(i);

            Neuron rec = chars.get(c);
            if (rec != null) {
                boolean begin = i == 0;
                boolean end = i + 1 == word.length();

                inputs.add(new Synapse.Builder()
                        .setSynapseId(i)
                        .setNeuron(rec)
                        .setWeight(begin || end ? 2.0 : 1.0)
                        .setRecurrent(false)
                        .setBias(begin || end ? -2.0 : -1.0)
                        .setRangeOutput(begin ? Range.Mapping.BEGIN : Range.Mapping.NONE, end ? Range.Mapping.END : Range.Mapping.NONE)
                );

                if(!end) {
                    inputs.add(new Relation.Builder()
                                    .setFrom(i)
                                    .setTo(i + 1)
                                    .setRangeRelation(Range.Relation.END_TO_BEGIN_EQUALS)
                    );
                } else {
                    inputs.add(new Relation.Builder()
                                    .setFrom(i)
                                    .setTo(0)
                                    .setRangeRelation(Range.Relation.create(NONE, NONE, NONE, GREATER_THAN))
                    );
                }
            }
        }

        Neuron n = Neuron.init(m.createNeuron("PATTERN"), 0.5, INeuron.Type.EXCITATORY, inputs.toArray(new Neuron.Builder[inputs.size()]));

        System.out.println(n.get().node.get().logicToString());

        Document doc = m.createDocument("abc Huettenheim cba", 0);

        for (int i = 0; i < doc.length(); i++) {
            char c = doc.getContent().toLowerCase().charAt(i);

            Neuron inputNeuron = chars.get(c);
            if (inputNeuron != null) {
                inputNeuron.addInput(doc, i, i + 1);
            }
        }

        System.out.println(doc.activationsToString(false, true, true));

        assert n.get().getActivations(doc, false).size() >= 1;
    }



    @Test
    public void testInstanceRelation1() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A");

        Neuron nB = Neuron.init(m.createNeuron("B"), 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setIdentity(true)
                        .setRangeOutput(Range.Output.DIRECT)
        );

        Neuron nC = Neuron.init(m.createNeuron("C"), 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setIdentity(true)
                        .setRangeOutput(Range.Output.DIRECT)
        );

        Neuron nD = Neuron.init(m.createNeuron("D"), 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(nB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(Range.Output.DIRECT)
                        .setSynapseId(0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(nC)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(Range.Output.NONE)
                        .setSynapseId(1),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setAncestorRelation(COMMON_ANCESTOR)
        );


        inA.addInput(doc, 0, 1);

        doc.process();

        Assert.assertFalse(nD.getActivations(doc, true).isEmpty());
    }


    @Test
    public void testInstanceRelation2() {
        Model m = new Model();
        Document doc = m.createDocument("aaaaaaaaaa", 0);

        Neuron inA = m.createNeuron("A");

        Neuron inB = m.createNeuron("B");


        Neuron nC = Neuron.init(m.createNeuron("C"), 5.0, INeuron.Type.EXCITATORY,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(Range.Output.DIRECT)
                        .setSynapseId(0),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(10.0)
                        .setBias(-10.0)
                        .setRecurrent(false)
                        .setRangeOutput(Range.Output.NONE)
                        .setSynapseId(1),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setAncestorRelation(COMMON_ANCESTOR)
        );


        inA.addInput(doc, 0, 1);
        inB.addInput(doc, 1, 2);

        doc.process();

        Assert.assertTrue(nC.getActivations(doc, true).isEmpty());

    }
}
