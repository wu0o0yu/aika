package network.aika;

import network.aika.debugger.AikaDebugger;
import network.aika.neuron.Neuron;
import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.text.TokenActivation;
import network.aika.utils.TestUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static network.aika.utils.TestUtils.getConfig;

public class GradientTest {


    @Test
    public void gradientAndInduction2() {
        TextModel m = new TextModel();

        m.setN(912);
        m.init();

        Document doc = new Document(m, "A B ");
        doc.setConfig(
                getConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.1)
                .setEnableTraining(true)
        );
/*
        Neuron nA = m.getNeuron("A");
        nA.setFrequency(53.0);
        nA.getSampleSpace().setN(299);
        nA.getSampleSpace().setOffset(899l);


        Neuron nB = m.getNeuron("B");
        nB.setFrequency(10.0);
        nB.getSampleSpace().setN(121);
        nB.getSampleSpace().setOffset(739l);
*/

        AikaDebugger.createAndShowGUI(doc);

        processDoc(doc);

        doc.process();

        System.out.println();
    }


    @Test
    public void gradientAndInduction3() {
        TextModel m = new TextModel();

        m.setN(912);
        m.init();

        Document doc = new Document(m, "A B C ");
        doc.setConfig(
                getConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setEnableTraining(true)
        );

        processDoc(doc);

        Neuron nA = m.getNeuron("A");
        nA.setFrequency(53.0);
        nA.getSampleSpace().setN(299);
        nA.getSampleSpace().setLastPosition(899l);


        Neuron nB = m.getNeuron("B");
        nB.setFrequency(10.0);
        nB.getSampleSpace().setN(121);
        nB.getSampleSpace().setLastPosition(739l);


        Neuron nC = m.getNeuron("C");
        nC.setFrequency(30.0);
        nC.getSampleSpace().setN(234);
        nC.getSampleSpace().setLastPosition(867l);


        AikaDebugger.createAndShowGUI(doc);

        doc.process();
        doc.updateModel();

        System.out.println();
    }


    @Test
    public void gradientAndInduction2With2Docs() {
        TextModel m = new TextModel();

        m.setN(912);
        m.getTemplates().SAME_BINDING_TEMPLATE.getBias().add(-0.32);
        m.init();

        Document doc1 = new Document(m, "A B ");
        doc1.setConfig(
                getConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setEnableTraining(true)
        );
        processDoc(doc1);

        Neuron nA = m.getNeuron("A");
        nA.setFrequency(53.0);
        nA.getSampleSpace().setN(299);
        nA.getSampleSpace().setLastPosition(899l);

        Neuron nB = m.getNeuron("B");
        nB.setFrequency(10.0);
        nB.getSampleSpace().setN(121);
        nB.getSampleSpace().setLastPosition(739l);

        AikaDebugger.createAndShowGUI(doc1);

        doc1.process();
        doc1.updateModel();

        Document doc2 = new Document(m, "A C ");
        doc2.setConfig(
                getConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setEnableTraining(true)
        );
        processDoc(doc2);

        Neuron nC = m.getNeuron("C");
        nC.setFrequency(30.0);
        nC.getSampleSpace().setN(234);
        nC.getSampleSpace().setLastPosition(867l);

        AikaDebugger.createAndShowGUI(doc2);

        doc2.process();
        doc2.updateModel();

        System.out.println();
    }

    private void processDoc(Document doc) {
        doc.processTokens(Arrays.asList(doc.getContent().split(" ")));
    }
}
