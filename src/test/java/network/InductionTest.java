package network;

import network.aika.Config;
import network.aika.neuron.Templates;
import network.aika.neuron.activation.Link;
import network.aika.neuron.excitatory.PatternPartSynapse;
import network.aika.neuron.excitatory.PatternPartNeuron;
import network.aika.text.Document;

import network.aika.Model;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.text.TextModel;
import network.aika.text.TextReference;
import org.junit.jupiter.api.Test;

import static network.aika.neuron.Templates.*;

public class InductionTest {

    @Test
    public void testInduceFromMaturePattern() {
        Model m = new TextModel();
        Templates t = new Templates(m);

        PatternNeuron in = t.INPUT_PATTERN_TEMPLATE.instantiateTemplate();
        in.setTokenLabel("A");
        in.setInputNeuron(true);
        in.setLabel("IN");

        in.setFrequency(12);

        in.getSampleSpace().setN(200);
        m.setN(200);

        Document doc = new Document("");

        Activation act = new Activation(doc, in);
        act.initInput(new TextReference(doc, 0, 1));

        doc.process(m);

        System.out.println(doc);
    }

    @Test
    public void initialGradientTest() {
        Model m = new TextModel();
        m.setConfig(
                new Config()
                        .setLearnRate(-0.1)
        );

        Templates t = new Templates(m);

        PatternNeuron inA = t.INPUT_PATTERN_TEMPLATE.instantiateTemplate();
        inA.setTokenLabel("A");
        inA.setInputNeuron(true);
        inA.setLabel("IN-A");
        PatternNeuron inB = t.INPUT_PATTERN_TEMPLATE.instantiateTemplate();
        inB.setTokenLabel("B");
        inB.setInputNeuron(true);
        inB.setLabel("IN-B");
        PatternPartNeuron targetN = t.PATTERN_PART_TEMPLATE.instantiateTemplate();
        targetN.setLabel("OUT-Target");

        targetN.setBias(0.0);
        targetN.setDirectConjunctiveBias(0.0);
        targetN.setRecurrentConjunctiveBias(0.0);

        PatternPartSynapse sA = t.PRIMARY_INPUT_SYNAPSE_TEMPLATE.instantiateTemplate(inA, targetN);

        sA.linkInput();
        sA.linkOutput();
        sA.setWeight(0.1);
        targetN.addConjunctiveBias(-0.1, false);

        PatternPartSynapse sB = t.PRIMARY_INPUT_SYNAPSE_TEMPLATE.instantiateTemplate(inB, targetN);

        sB.linkInput();
        sB.linkOutput();
        sB.setWeight(0.0);


        m.setN(100);
        inA.setFrequency(10.0);
        inB.setFrequency(10.0);

        targetN.getSampleSpace().setN(100);
        targetN.setFrequency(0.0);

        System.out.println(targetN.statToString());
        System.out.println();

        // -----------------------------------------

        Document doc = new Document("");

        Activation actA = new Activation(doc, inA);
        Activation actB = new Activation(doc, inB);
        Activation actTarget = new Activation(doc, targetN);

        actA.setReference(new TextReference(doc, 0, 1));
        actA.setValue(1.0);

        actB.setReference(new TextReference(doc, 0, 1));
        actB.setValue(1.0);

        Link.link(sA, actA, actTarget, false);
        Link.link(sB, actB, actTarget, false);

        actTarget.initSelfGradient();
 //       actTarget.computeInitialLinkGradients();
        actTarget.processGradient();

        System.out.println(actTarget.gradientsToString());
    }

    @Test
    public void inductionTest() {
        TextModel model = new TextModel();
        model.setConfig(
                new Config()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
        );

        String phrase = "der Hund";

        Document doc = new Document(phrase);
        System.out.println("  " + phrase);

        Activation actDer = doc.processToken(model, null, 0, 4, "der");
        Activation actHund = doc.processToken(model, actDer.getReference(), 4, 8, "Hund");

        model.setN(1000);
        actDer.getNeuron().setFrequency(50);
        actDer.getNeuron().getSampleSpace().setN(1000);
        actHund.getNeuron().setFrequency(10);
        actHund.getNeuron().getSampleSpace().setN(1000);

        doc.process(model);

        System.out.println(doc);
        System.out.println(doc.gradientsToString());

        System.out.println(); // doc.activationsToString()
    }
}
