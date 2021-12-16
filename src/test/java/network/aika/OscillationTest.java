package network.aika;

import network.aika.debugger.AikaDebugger;
import network.aika.neuron.Synapse;
import network.aika.neuron.Templates;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.BindingNeuron;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.utils.TestUtils;
import org.junit.jupiter.api.Test;

import static network.aika.utils.TestUtils.*;

public class OscillationTest {

    @Test
    public void oscillationTest() {
        TextModel m = new TextModel();
        Templates t = m.getTemplates();

        m.setN(912);

        Document doc = new Document(m, "A ");
        doc.setConfig(
                getConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setEnableTraining(true)
        );

        PatternNeuron nA = createNeuron(t.SAME_PATTERN_TEMPLATE, "P-A");

        nA.setFrequency(53.0);
        nA.getSampleSpace().setN(299);
        nA.getSampleSpace().setLastPosition(899l);

        BindingNeuron nPPA = createNeuron(t.SAME_BINDING_TEMPLATE, "B-A");
        createSynapse(t.PRIMARY_INPUT_SYNAPSE_TEMPLATE, nA, nPPA, 0.3);

        AikaDebugger.createAndShowGUI(doc);

        doc.addToken(nA, 0, 1);
        doc.process();
        doc.updateModel();

        System.out.println();
    }
}
