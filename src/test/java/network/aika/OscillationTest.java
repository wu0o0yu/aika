package network.aika;

import network.aika.debugger.AikaDebugger;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.excitatory.BindingNeuron;
import network.aika.neuron.excitatory.BindingNeuronSynapse;
import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.text.TextReference;
import org.junit.jupiter.api.Test;

public class OscillationTest {

    @Test
    public void oscillationTest() throws InterruptedException {
        TextModel m = new TextModel();
        m.setConfig(
                new Config() {
                    public String getLabel(Activation act) {
                        return "X";
                    }
                }
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setEnableTraining(true)
                        .setSurprisalInductionThreshold(0.0)
                        .setGradientInductionThreshold(0.0)
        );

        m.setN(912);

        Document doc = new Document("A ");
        PatternNeuron nA = m.getTemplates().SAME_PATTERN_TEMPLATE.instantiateTemplate();
        nA.setLabel("P-A");

        nA.setFrequency(53.0);
        nA.getSampleSpace().setN(299);
        nA.getSampleSpace().setLastPos(899);

        BindingNeuron nPPA =  m.getTemplates().SAME_PATTERN_PART_TEMPLATE.instantiateTemplate();
        nPPA.setLabel("PP-A");

        BindingNeuronSynapse s = m.getTemplates().PRIMARY_INPUT_SYNAPSE_TEMPLATE.instantiateTemplate(nA, nPPA);

        s.setWeight(0.3);

        s.linkInput();
        s.linkOutput();

        AikaDebugger.createAndShowGUI(doc,m);

        doc.addInput(nA, new TextReference(doc, 0, 1));

        doc.process(m);

        System.out.println();
    }

}
