package network;

import network.aika.Config;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Link;
import network.aika.neuron.excitatory.ExcitatorySynapse;
import network.aika.neuron.excitatory.PatternPartNeuron;
import network.aika.text.Document;

import network.aika.Model;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.text.TextModel;
import org.junit.jupiter.api.Test;

public class InductionTest {

    @Test
    public void testInduceFromMaturePattern() {
        Model m = new TextModel();
        PatternNeuron in = new PatternNeuron(m, "A", "IN", true);
        in.setFrequency(12);

        Document doc = new Document("",
                new Config()
        );

        Activation act = new Activation(doc, in);
        act.setValue(1.0);
        act.setReference(doc.new TextReference(0, 1));

        act.propagateInput();

        doc.train(m);

        System.out.println(doc.activationsToString());
    }

    @Test
    public void initialGradientTest() {
        Model m = new TextModel();
        PatternNeuron inA = new PatternNeuron(m, "A", "IN-A", true);
        PatternNeuron inB = new PatternNeuron(m, "B", "IN-B", true);
        PatternPartNeuron targetN = new PatternPartNeuron(m, "OUT-Target", false);

        targetN.setBias(0.0);
        targetN.setDirectConjunctiveBias(0.0);
        targetN.setRecurrentConjunctiveBias(0.0);

        ExcitatorySynapse sA = new ExcitatorySynapse(inA, targetN, false, false, true, false);

        sA.linkInput();
        sA.linkOutput();
        sA.setWeight(0.1);
        targetN.addConjunctiveBias(-0.1, false);

        ExcitatorySynapse sB = new ExcitatorySynapse(inB, targetN, false, false, true, false);

        sB.linkInput();
        sB.linkOutput();
        sB.setWeight(0.0);


        m.setN(100);
        inA.setFrequency(10.0);
        inB.setFrequency(10.0);

        targetN.getInstances().setOffset(100 - 1);
        targetN.setFrequency(0.0);

        System.out.println(targetN.statToString());
        System.out.println();

        // -----------------------------------------

        Document doc = new Document("",
                new Config()
                        .setLearnRate(-0.1)
        );

        Activation actA = new Activation(doc, inA);
        Activation actB = new Activation(doc, inB);
        Activation actTarget = new Activation(doc, targetN);

        actA.setReference(doc.new TextReference(0, 1));
        actA.setValue(1.0);

        actB.setReference(doc.new TextReference(0, 1));
        actB.setValue(1.0);

        Link.link(sA, actA, actTarget, false);
        Link.link(sB, actB, actTarget, false);

        actTarget.updateGradient();
        actTarget.processGradient();

        System.out.println(actTarget.gradientsToString());
    }


    @Test
    public void inductionTest() {
        TextModel model = new TextModel();

        String phrase = "der Hund";

        Document doc = new Document(phrase,
                new Config()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
        );
        System.out.println("  " + phrase);

        Activation actDer = doc.processToken(model, 0, 4, "der");
        Activation actHund = doc.processToken(model, 4, 8, "Hund");

        model.setN(1000);
        actDer.getNeuron().setFrequency(50);
        actDer.getNeuron().getInstances().setOffset(0);
        actHund.getNeuron().setFrequency(10);
        actHund.getNeuron().getInstances().setOffset(0);

        doc.process();

        System.out.println(doc.activationsToString());

        doc.train(model);

        System.out.println(doc.gradientsToString());

        System.out.println(); // doc.activationsToString()
    }
}
