package network.aika.training.textgeneration;

import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.search.SearchNode;
import network.aika.neuron.relation.MultiRelation;
import network.aika.neuron.relation.PositionRelation;
import network.aika.training.MetaModel;
import network.aika.training.TDocument;
import network.aika.training.excitatory.ExcitatoryNeuron;
import network.aika.training.inhibitory.InhibitoryNeuron;
import network.aika.training.input.InputNeuron;
import network.aika.training.meta.MetaNeuron;
import network.aika.training.meta.MetaSynapse;
import network.aika.neuron.relation.Relation;
import network.aika.training.relation.WeightedRelation;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;
import java.util.TreeMap;

import static network.aika.neuron.INeuron.Type.*;
import static network.aika.neuron.Synapse.OUTPUT;
import static network.aika.neuron.activation.Activation.BEGIN;
import static network.aika.neuron.activation.Activation.END;
import static network.aika.neuron.relation.AncestorRelation.IS_DESCENDANT_OF;
import static network.aika.neuron.relation.Relation.*;

public class MirrorNeuronExperiment {

    public MetaModel model;

    public MetaNeuron inputLetter;
    public MetaNeuron outputLetter;

    public Map<Character, Neuron> inputLetters = new TreeMap<>();

    public MetaNeuron wordInputNeuron;
    public MetaNeuron wordTriggerNeuron;
    public MetaNeuron wordOutputNeuron;
    public MetaNeuron letterOutputNeuron;
    public Neuron outputTrigger;
    public Neuron trainExampleFrame;
    public Neuron outputFrame;


    @Before
    public void init() {
        model = new MetaModel();

        inputLetter = model.createMetaNeuron("InputLetter");
        outputLetter = model.createMetaNeuron("OutputLetter");
        wordInputNeuron = model.createMetaNeuron("WordInputNeuron");
        wordTriggerNeuron = model.createMetaNeuron("WordTriggerNeuron"); // LIMITED_RECTIFIED_LINEAR_UNIT
        wordOutputNeuron = model.createMetaNeuron("WordOutputNeuron");
        letterOutputNeuron = model.createMetaNeuron("LetterOutputNeuron");
        outputTrigger = new InputNeuron(model, "OutputTrigger").getProvider();
        trainExampleFrame = new InputNeuron(model, "TrainExampleFrame").getProvider();
        outputFrame = new InhibitoryNeuron(model, "OutputFrame").getProvider();

        model.initMetaNeuron(inputLetter,
                0.0,
                -10.0,
                new MultiRelation(
                        new WeightedRelation(new PositionRelation.Equals(BEGIN, BEGIN), 1.0),
                        new WeightedRelation(new PositionRelation.Equals(END, END), 1.0)
                )
        );

        String letters = "abcdefghijklmnopqrstuvwxyz";

        model.initMetaNeuron(wordInputNeuron,
                5.0,
                -10.0,
                new MultiRelation(
                        new WeightedRelation(new PositionRelation.Equals(BEGIN, BEGIN), 1.0),
                        new WeightedRelation(new PositionRelation.Equals(END, END), 1.0)
                ),
                new MetaSynapse.Builder() // First word of the phrase
                        .setIsMetaVariable(true)
                        .setSynapseId(0)
                        .setNeuron(inputLetter.getInhibitoryNeuron())
                        .setWeight(10.0)
                        .setIdentity(true),
                new MetaSynapse.Builder()
                        .setIsMetaVariable(true)
                        .setSynapseId(1)
                        .setNeuron(inputLetter.getInhibitoryNeuron())
                        .setWeight(10.0)
                        .setIdentity(true),
                new MetaSynapse.Builder()
                        .setIsMetaVariable(true)
                        .setSynapseId(2)
                        .setNeuron(inputLetter.getInhibitoryNeuron())
                        .setWeight(10.0)
                        .setIdentity(true),
                new MetaSynapse.Builder()
                        .setIsMetaVariable(true)
                        .setSynapseId(3)
                        .setNeuron(inputLetter.getInhibitoryNeuron())
                        .setWeight(10.0)
                        .setIdentity(true),
                new MetaSynapse.Builder()
                        .setIsMetaVariable(false)
                        .setSynapseId(4)
                        .setNeuron(wordInputNeuron.getInhibitoryNeuron())
                        .setWeight(-100.0)
                        .setRecurrent(true),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(new WeightedRelation(BEGIN_TO_END_EQUALS, 1.0)),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(1)
                        .setRelation(new WeightedRelation(BEGIN_TO_END_EQUALS, 1.0)),
                new Relation.Builder()
                        .setFrom(3)
                        .setTo(2)
                        .setRelation(new WeightedRelation(BEGIN_TO_END_EQUALS, 1.0)),
                new Relation.Builder()
                        .setFrom(4)
                        .setTo(OUTPUT)
                        .setRelation(new MultiRelation(
                                new WeightedRelation(new PositionRelation.LessThan(BEGIN, END, false), 1.0),
                                new WeightedRelation(new PositionRelation.GreaterThan(END, BEGIN, false, Integer.MAX_VALUE), 1.0)
                        )),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(new WeightedRelation(BEGIN_EQUALS, 1.0)),
                new Relation.Builder()
                        .setFrom(3)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(new WeightedRelation(END_EQUALS, 1.0))
        );

        model.initMetaNeuron(wordTriggerNeuron,
                0.0,
                -10.0,
                new MultiRelation(
                        new WeightedRelation(new PositionRelation.Equals(BEGIN, BEGIN), 1.0),
                        new WeightedRelation(new PositionRelation.Equals(END, END), 1.0)
                )
        );

        model.initMetaNeuron(wordOutputNeuron,
                5.0,
                -10.0,
                new MultiRelation(
                        new WeightedRelation(new PositionRelation.Equals(BEGIN, BEGIN), 1.0),
                        new WeightedRelation(new PositionRelation.Equals(END, END), 1.0)
                ),
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(wordInputNeuron.getInhibitoryNeuron())
                        .setWeight(10.0)
                        .setIdentity(true),
                new MetaSynapse.Builder()
                        .setIsMetaVariable(false)
                        .setSynapseId(1)
                        .setNeuron(outputFrame)
                        .setWeight(10.0),
                new MetaSynapse.Builder()
                        .setIsMetaVariable(true)
                        .setSynapseId(2)
                        .setNeuron(wordTriggerNeuron.getInhibitoryNeuron())
                        .setWeight(10.0),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(new MultiRelation(
                                new WeightedRelation(new PositionRelation.Equals(BEGIN, BEGIN), 1.0),
                                new WeightedRelation(new PositionRelation.Equals(END, END), 1.0)
                        )),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(1)
                        .setRelation(new WeightedRelation(ANY, 1.0)),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(new MultiRelation(
                                new WeightedRelation(new PositionRelation.Equals(BEGIN, BEGIN), 1.0),
                                new WeightedRelation(new PositionRelation.Equals(END, END), 1.0)
                        ))
        );

        model.initMetaNeuron(letterOutputNeuron,
                0.0,
                -5.0,
                new MultiRelation(
                        new WeightedRelation(new PositionRelation.Equals(BEGIN, BEGIN), 1.0),
                        new WeightedRelation(new PositionRelation.Equals(END, END), 1.0)
                ),
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(inputLetter.getInhibitoryNeuron())
                        .setWeight(10.0)
//                        .setBias(-10.0)
                        .setIdentity(true),
                new MetaSynapse.Builder()
                        .setIsMetaVariable(true)
                        .setSynapseId(1)
                        .setNeuron(wordOutputNeuron.getInhibitoryNeuron())
                        .setWeight(4.0)
//                        .setBias(0.0)
                        .setIdentity(true),
                new MetaSynapse.Builder()
                        .setIsMetaVariable(true)
                        .setSynapseId(2)
                        .setNeuron(wordOutputNeuron.getInhibitoryNeuron())
                        .setWeight(4.0)
//                        .setBias(0.0)
                        .setIdentity(true),
                new MetaSynapse.Builder()
                        .setIsMetaVariable(true)
                        .setSynapseId(3)
                        .setNeuron(letterOutputNeuron.getInhibitoryNeuron())
                        .setWeight(4.0)
//                        .setBias(0.0)
                        .setIdentity(true),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(new WeightedRelation(BEGIN_EQUALS, 1.0)),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(0)
                        .setRelation(new WeightedRelation(END_EQUALS, 1.0)),
                new Relation.Builder()
                        .setFrom(3)
                        .setTo(0)
                        .setRelation(new WeightedRelation(END_TO_BEGIN_EQUALS, 1.0)),
                new Relation.Builder()
                        .setFrom(3)
                        .setTo(2)
                        .setRelation(IS_DESCENDANT_OF),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(new MultiRelation(
                                new WeightedRelation(new PositionRelation.Equals(BEGIN, BEGIN), 1.0),
                                new WeightedRelation(new PositionRelation.Equals(END, END), 1.0)
                        )),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(new WeightedRelation(BEGIN_EQUALS, 1.0)),
                new Relation.Builder()
                        .setFrom(2)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(new WeightedRelation(END_EQUALS, 1.0)),
                new Relation.Builder()
                        .setFrom(3)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(new WeightedRelation(END_TO_BEGIN_EQUALS, 1.0))
        );


        model.initMetaNeuron(outputLetter,
                0.0,
                -5.0,
                new MultiRelation(
                        new WeightedRelation(new PositionRelation.Equals(BEGIN, BEGIN), 1.0),
                        new WeightedRelation(new PositionRelation.Equals(END, END), 1.0)
                ),
                new MetaSynapse.Builder()
                        .setIsMetaVariable(true)
                        .setSynapseId(0)
                        .setNeuron(letterOutputNeuron.getInhibitoryNeuron())
                        .setWeight(5.0)
//                        .setBias(0.0)
                        .setIdentity(true),
                new MetaSynapse.Builder()
                        .setIsMetaVariable(true)
                        .setSynapseId(1)
                        .setNeuron(inputLetter.getInhibitoryNeuron())
                        .setWeight(10.0)
//                        .setBias(-10.0)
                        .setIdentity(true),
                new Relation.Builder()
                        .setFrom(1)
                        .setTo(0)
                        .setRelation(new MultiRelation(
                                new WeightedRelation(new PositionRelation.Equals(BEGIN, BEGIN), 1.0),
                                new WeightedRelation(new PositionRelation.Equals(END, END), 1.0)
                        )),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(new MultiRelation(
                                new WeightedRelation(new PositionRelation.Equals(BEGIN, BEGIN), 1.0),
                                new WeightedRelation(new PositionRelation.Equals(END, END), 1.0)
                        ))
        );

        outputLetter.setOutputMetaNeuron(true);


        Neuron.init(outputFrame,
                0.0,
                new Synapse.Builder()
                        .setSynapseId(0)
                        .setNeuron(outputTrigger)
                        .setWeight(5.0)
                        .setIdentity(true),
                new Synapse.Builder()
                        .setSynapseId(1)
                        .setNeuron(trainExampleFrame)
                        .setWeight(5.0)
                        .setIdentity(true),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(END_TO_BEGIN_EQUALS),
                new Relation.Builder()
                        .setFrom(0)
                        .setTo(Synapse.OUTPUT)
                        .setRelation(new MultiRelation(
                                new PositionRelation.Equals(BEGIN, BEGIN),
                                new PositionRelation.Equals(END, END)
                        ))
        );


        TDocument doc = new TDocument(model, "");

        for(int i = 0; i < letters.length(); i++) {
            char c = letters.charAt(i);

            inputLetters.put(c, inputLetter.createMetaNeuronTargetFromLabel(doc, "" + c, null).getProvider());
        }

        doc.clearActivations();
    }


    @Ignore
    @Test
    public void testMirrorNeurons() {
        SearchNode.COMPUTE_SOFT_MAX = true;
        SearchNode.OPTIMIZE_SEARCH = false;

        System.out.println("Train: ");
        TDocument trainDoc = new TDocument(model, "haus");


        for(int i = 0; i < trainDoc.length(); i++) {
            char c = trainDoc.charAt(i);
            inputLetters.get(c).addInput(trainDoc, i, i + 1);
        }

        trainExampleFrame.addInput(trainDoc, 0, 4);

        ExcitatoryNeuron hausTrigger = wordTriggerNeuron.createMetaNeuronTargetFromLabel(trainDoc, "Haus-Trigger", null);
        hausTrigger.getProvider().addInput(trainDoc, 0, 0);

        trainDoc.process();

        trainDoc.trainMeta(0.3);

        System.out.println(trainDoc.activationsToString());

        trainDoc.clearActivations();

        TDocument testDoc = new TDocument(model, "");
        outputTrigger.addInput(testDoc, 0, 0);
        hausTrigger.getProvider().addInput(testDoc, 0, 0);

        testDoc.process();

        System.out.println(testDoc.activationsToString());

        String outputText = testDoc.generateOutputText();

        System.out.println("Output Text:" + outputText);

        System.out.println(testDoc.activationsToString());

        testDoc.clearActivations();

        Assert.assertEquals("haus", outputText);
    }
}
