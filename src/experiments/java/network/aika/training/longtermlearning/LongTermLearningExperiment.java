package network.aika.training.longtermlearning;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.training.*;
import network.aika.training.excitatory.ExcitatoryNeuron;
import network.aika.training.excitatory.ExcitatorySynapse;
import network.aika.training.inhibitory.InhibitoryNeuron;
import network.aika.training.inhibitory.InhibitorySynapse;
import network.aika.training.input.InputNeuron;
import network.aika.neuron.relation.PositionRelation;
import network.aika.neuron.relation.Relation;
import org.junit.Test;

import static network.aika.neuron.INeuron.Type.INPUT;
import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.activation.Activation.BEGIN;
import static network.aika.neuron.activation.Activation.END;
import static network.aika.neuron.relation.Relation.END_TO_BEGIN_EQUALS;
import static network.aika.neuron.relation.Relation.EQUALS;
import static network.aika.neuron.INeuron.Type.EXCITATORY;

public class LongTermLearningExperiment {


    @Test
    public void testStrongSynapseWithVotesAgainstIt() {
        MetaModel model = new MetaModel();

        Neuron inA = new InputNeuron(model, "A").getProvider();
        Neuron inB = new InputNeuron(model, "B").getProvider();
        Neuron inC = new InputNeuron(model, "C").getProvider();

        Neuron testNeuron = new ExcitatoryNeuron(model, "Test").getProvider();

        Neuron.init(testNeuron,
                3.0,
                new ExcitatorySynapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(5.0),
                new ExcitatorySynapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(1.0),
                new ExcitatorySynapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inC)
                        .setWeight(1.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
                );


        model.charCounter = 100;

        setFrequency(inA, 29);
        setFrequency(inB, 29);
        setFrequency(inC, 29);
        setFrequency(testNeuron, 10);

        ((TSynapse)testNeuron.getSynapseById(0)).setFrequency(9, 20, 1, 70);
        ((TSynapse)testNeuron.getSynapseById(1)).setFrequency(9, 20, 1, 70);
        ((TSynapse)testNeuron.getSynapseById(2)).setFrequency(9, 20, 1, 70);


        TDocument doc = new TDocument(model, "Bla");
        inA.addInput(doc, 0, 3);

        doc.process();

        doc.trainLTL(
                new Config()
                .setLearnRate(0.1)
        );

        System.out.println(doc.activationsToString());

        model.dumpModel();

        System.out.println();
    }


    @Test
    public void testCoveredSynapsesWithOneVoteAgainst() {
        MetaModel model = new MetaModel();

        Neuron inA = new InputNeuron(model, "A").getProvider();
        Neuron inB = new InputNeuron(model, "B").getProvider();
        Neuron inC = new InputNeuron(model, "C").getProvider();
        Neuron inD = new InputNeuron(model, "D").getProvider();

        Neuron testNeuron = new ExcitatoryNeuron(model, "Test").getProvider();

        Neuron.init(testNeuron,
                2.1,
                new ExcitatorySynapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inA)
                        .setWeight(2.0),
                new ExcitatorySynapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inB)
                        .setWeight(2.0),
                new ExcitatorySynapse.Builder()
                        .setSynapseId(2)
                        .setNeuron(inC)
                        .setWeight(2.0),
                new ExcitatorySynapse.Builder()
                        .setSynapseId(3)
                        .setNeuron(inD)
                        .setWeight(2.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS),
                new Relation.Builder()
                        .setFrom(3)
                        .setTo(OUTPUT)
                        .setRelation(EQUALS)
        );


        model.charCounter = 100;

        setFrequency(inA, 29);
        setFrequency(inB, 29);
        setFrequency(inC, 29);
        setFrequency(inD, 29);
        setFrequency(testNeuron, 5);

        ((TSynapse)testNeuron.getSynapseById(0)).setFrequency( 4, 20, 1, 75);
        ((TSynapse)testNeuron.getSynapseById(1)).setFrequency(4, 20, 1, 75);
        ((TSynapse)testNeuron.getSynapseById(2)).setFrequency(4, 20, 1, 75);
        ((TSynapse)testNeuron.getSynapseById(3)).setFrequency(4, 20, 1, 75);


        TDocument doc = new TDocument(model, "Bla");
        inB.addInput(doc, 0, 3);
        inC.addInput(doc, 0, 3);
        inD.addInput(doc, 0, 3);

        doc.process();

        doc.trainLTL(
                new Config()
                        .setLearnRate(0.1)
        );

        System.out.println(doc.activationsToString());

        model.dumpModel();

        System.out.println();
    }


    @Test
    public void testNegativeInhibitorySynapseTraining() {
        MetaModel model = new MetaModel();

        InputNeuron inDer = new InputNeuron(model, "W-der");
        InputNeuron inHund = new InputNeuron(model, "W-Hund");

        ExcitatoryNeuron phraseDerHund = new ExcitatoryNeuron(model, "P-der Hund");

        Neuron.init(phraseDerHund.getProvider(), 2.0,
                new ExcitatorySynapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inDer)
                        .setWeight(5.0),
                new ExcitatorySynapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(inHund)
                        .setWeight(5.0),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(1)
                        .setRelation(END_TO_BEGIN_EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(OUTPUT)
                        .setRelation(
                                new Relation[]{
                                        new PositionRelation.Equals(BEGIN, 0),
                                        new PositionRelation.Equals(END, 1)
                                }
                        ),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(OUTPUT)
                        .setRelation(
                                new Relation[]{
                                        new PositionRelation.Equals(BEGIN, 2),
                                        new PositionRelation.Equals(END, 3)
                                }
                        )
        );


        InhibitoryNeuron in = new InhibitoryNeuron(model, "I-Phrase");
        int synId = in.getNewSynapseId();

        Neuron.init(in.getProvider(), 0.0,
                new InhibitorySynapse.Builder()
                        .setSynapseId(synId)
                        .setNeuron(phraseDerHund)
                        .setWeight(1.0),
                new Relation.Builder()
                        .setFrom(synId)
                        .setTo(OUTPUT)
                        .setRelation(
                                new Relation[]{
                                        new PositionRelation.Equals(2, BEGIN),
                                        new PositionRelation.Equals(3, END)
                                }
                        )
        );


        for(int i = 0; i < 10; i++) {
            TDocument doc = new TDocument(model, "der Hund");

            inDer.getProvider().addInput(doc, 0, 4);
            inHund.getProvider().addInput(doc, 4, 8);

            doc.process();

            doc.generateSynapses();

            doc.count();

            doc.trainLTL(
                    new Config()
                            .setLearnRate(0.1)
            );

            doc.clearActivations();
        }

        model.dumpModel();
    }


    private static void setFrequency(Neuron n, double f) {
        TNeuron ne = (TNeuron) n.get();

        ne.posFrequency = f;
    }

}
