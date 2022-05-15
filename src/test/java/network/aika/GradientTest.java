package network.aika;

import network.aika.debugger.AIKADebugger;
import network.aika.neuron.Neuron;
import network.aika.text.Document;
import network.aika.text.TextModel;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static network.aika.utils.TestUtils.getConfig;
import static network.aika.utils.TestUtils.setStatistic;

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
                .setTrainingEnabled(true)
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

        AIKADebugger.createAndShowGUI(doc);

        processDoc(doc);

        doc.processFinalMode();
        doc.postProcessing();

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
                        .setTrainingEnabled(true)
        );

        processDoc(doc);

        Neuron nA = m.getNeuron("A");
        setStatistic(nA, 53.0,299,899l);

        Neuron nB = m.getNeuron("B");
        setStatistic(nB, 10.0, 121, 739l);

        Neuron nC = m.getNeuron("C");
        setStatistic(nC, 30.0, 234, 867l);


        AIKADebugger.createAndShowGUI(doc);

        doc.processFinalMode();
        doc.postProcessing();
        doc.updateModel();

        System.out.println();
    }


    @Test
    public void gradientAndInduction2With2Docs() {
        TextModel m = new TextModel();

        m.setN(912);
        m.getTemplates().BINDING_TEMPLATE.getBias().receiveUpdate(-0.32);
        m.init();

        Document doc1 = new Document(m, "A B ");
        doc1.setConfig(
                getConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setTrainingEnabled(true)
        );
        processDoc(doc1);

        Neuron nA = m.getNeuron("A");
        setStatistic(nA, 53.0, 299, 899l);

        Neuron nB = m.getNeuron("B");
        setStatistic(nB, 10.0, 121, 739l);

        AIKADebugger.createAndShowGUI(doc1);

        doc1.processFinalMode();
        doc1.postProcessing();
        doc1.updateModel();

        Document doc2 = new Document(m, "A C ");
        doc2.setConfig(
                getConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setTrainingEnabled(true)
        );
        processDoc(doc2);

        Neuron nC = m.getNeuron("C");
        setStatistic(nC, 30.0, 234, 867l);

        AIKADebugger.createAndShowGUI(doc2);

        doc2.processFinalMode();
        doc2.postProcessing();
        doc2.updateModel();

        System.out.println();
    }

    private void processDoc(Document doc) {
        doc.processTokens(Arrays.asList(doc.getContent().split(" ")));
    }
}
