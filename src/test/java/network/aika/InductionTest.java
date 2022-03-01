package network.aika;

import network.aika.neuron.Synapse;
import network.aika.neuron.Templates;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.conjunctive.BindingNeuron;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.text.TokenActivation;
import network.aika.utils.TestUtils;
import org.junit.jupiter.api.Test;

import static network.aika.utils.TestUtils.createNeuron;
import static network.aika.utils.TestUtils.createSynapse;

public class InductionTest {

    @Test
    public void testInduceFromMaturePattern() {
        TextModel m = new TextModel();
        Templates t = new Templates(m);

        PatternNeuron in = t.INPUT_PATTERN_TEMPLATE.instantiateTemplate(true);
        in.setTokenLabel("A");
        in.setNetworkInput(true);
        in.setLabel("IN");

        in.setFrequency(12);

        in.getSampleSpace().setN(200);
        m.setN(200);

        Document doc = new Document(m, "");

        doc.addToken(in, 0, 1);

        doc.processFinalMode();
        doc.postProcessing();
        doc.updateModel();

        System.out.println(doc);
    }

    @Test
    public void initialGradientTest() {
        TextModel m = new TextModel();

        Templates t = new Templates(m);

        PatternNeuron inA = createNeuron(t.INPUT_PATTERN_TEMPLATE, "IN-A");
        PatternNeuron inB = createNeuron(t.INPUT_PATTERN_TEMPLATE, "IN-B");
        BindingNeuron targetN = createNeuron(t.BINDING_TEMPLATE, "OUT-Target");

        targetN.getBias().setAndTriggerUpdate(0.0);

        Synapse sA = createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, inA, targetN, 0.1);
        Synapse sB = createSynapse(t.PRIMARY_INPUT_SYNAPSE_FROM_PATTERN_TEMPLATE, inB, targetN, 0.0);


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

        sA.createLink(actA, actTarget);
        sB.createLink(actB, actTarget);

     //   actTarget.updateEntropyGradient();
 //       actTarget.computeInitialLinkGradients();
    }

    @Test
    public void inductionTest() throws InterruptedException {
        TextModel model = new TextModel();

        String phrase = "der Hund";

        Document doc = new Document(model, phrase);
        doc.setConfig(
                TestUtils.getConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setTrainingEnabled(true)
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

        doc.processFinalMode();
        doc.postProcessing();
        doc.updateModel();

        System.out.println(doc);
    }
}
