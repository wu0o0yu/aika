package network.aika;

import network.aika.neuron.Synapse;
import network.aika.neuron.Templates;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.BindingNeuron;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.text.TokenActivation;
import org.junit.jupiter.api.Test;

public class InductionTest {

    @Test
    public void testInduceFromMaturePattern() {
        TextModel m = new TextModel();
        Templates t = new Templates(m);

        PatternNeuron in = t.INPUT_PATTERN_TEMPLATE.instantiateTemplate(true);
        in.setTokenLabel("A");
        in.setInputNeuron(true);
        in.setLabel("IN");

        in.setFrequency(12);

        in.getSampleSpace().setN(200);
        m.setN(200);

        Document doc = new Document(m, "");

        doc.addToken(in, 0, 1);

        doc.process();
        doc.updateModel();

        System.out.println(doc);
    }

    @Test
    public void initialGradientTest() {
        TextModel m = new TextModel();

        Templates t = new Templates(m);

        PatternNeuron inA = t.INPUT_PATTERN_TEMPLATE.instantiateTemplate(true);
        inA.setTokenLabel("A");
        inA.setInputNeuron(true);
        inA.setLabel("IN-A");
        PatternNeuron inB = t.INPUT_PATTERN_TEMPLATE.instantiateTemplate(true);
        inB.setTokenLabel("B");
        inB.setInputNeuron(true);
        inB.setLabel("IN-B");
        BindingNeuron targetN = t.SAME_BINDING_TEMPLATE.instantiateTemplate(true);
        targetN.setLabel("OUT-Target");

        targetN.addBias(0.0);
        targetN.addConjunctiveBias(0.0);

        Synapse sA = t.PRIMARY_INPUT_SYNAPSE_TEMPLATE.instantiateTemplate(inA, targetN);

        sA.linkInput();
        sA.linkOutput();
        sA.getWeight().setInitialValue(0.1);
        targetN.addConjunctiveBias(-0.1);

        Synapse sB = t.PRIMARY_INPUT_SYNAPSE_TEMPLATE.instantiateTemplate(inB, targetN);

        sB.linkInput();
        sB.linkOutput();
        sB.getWeight().setInitialValue(0.0);


        m.setN(100);
        inA.setFrequency(10.0);
        inB.setFrequency(10.0);

        targetN.getSampleSpace().setN(100);
        targetN.setFrequency(0.0);

        System.out.println();

        // -----------------------------------------

        Document doc = new Document(m, "");

        Activation actA = inA.createActivation(doc);
        Activation actB = inB.createActivation(doc);
        Activation actTarget = targetN.createActivation(doc);

        actA.setInputValue(1.0);

        actB.setInputValue(1.0);

        actTarget.addLink(sA, actA, false);
        actTarget.addLink(sB, actB, false);

        actTarget.updateEntropyGradient();
 //       actTarget.computeInitialLinkGradients();

        System.out.println(actTarget.gradientsToString());
    }

    @Test
    public void inductionTest() throws InterruptedException {
        TextModel model = new TextModel();

        String phrase = "der Hund";

        Document doc = new Document(model, phrase);
        doc.setConfig(
                Util.getTestConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setEnableTraining(true)
        );
        System.out.println("  " + phrase);

        TokenActivation actDer = doc.addToken("der", 0, 4);
        TokenActivation actHund = doc.addToken("Hund", 4, 8);
        TokenActivation.addRelation(actDer, actHund);

        model.setN(1000);
        actDer.getNeuron().setFrequency(50);
        actDer.getNeuron().getSampleSpace().setN(1000);
        actHund.getNeuron().setFrequency(10);
        actHund.getNeuron().getSampleSpace().setN(1000);

        doc.process();
        doc.updateModel();

        System.out.println(doc);
        System.out.println(doc.gradientsToString());

        System.out.println(); // doc.activationsToString()
    }
}
