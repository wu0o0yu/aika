package network.aika;

import network.aika.debugger.AikaDebugger;
import network.aika.neuron.Neuron;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.excitatory.BindingNeuron;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.text.TextReference;
import org.junit.jupiter.api.Test;

public class GradientTest {


    @Test
    public void gradientAndInduction2() {
        TextModel m = new TextModel();

        m.setN(912);
        m.init();

        Document doc = new Document("A B ");
        doc.setConfig(
                Util.getTestConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.1)
                .setEnableTraining(true)
        );

        processDoc(m, doc);

        Neuron nA = m.getNeuron("A");
        nA.setFrequency(53.0);
        nA.getSampleSpace().setN(299);
        nA.getSampleSpace().setOffset(899l);


        Neuron nB = m.getNeuron("B");
        nB.setFrequency(10.0);
        nB.getSampleSpace().setN(121);
        nB.getSampleSpace().setOffset(739l);


        AikaDebugger.createAndShowGUI(doc,m);

        doc.process(m);

        System.out.println();
    }


    @Test
    public void gradientAndInduction3() {
        TextModel m = new TextModel();

        m.setN(912);
        m.init();

        Document doc = new Document("A B C ");
        doc.setConfig(
                Util.getTestConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setEnableTraining(true)
        );

        processDoc(m, doc);

        m.getTemplates().SAME_BINDING_TEMPLATE.addConjunctiveBias(-0.32);

        Neuron nA = m.getNeuron("A");
        nA.setFrequency(53.0);
        nA.getSampleSpace().setN(299);
        nA.getSampleSpace().setOffset(899l);


        Neuron nB = m.getNeuron("B");
        nB.setFrequency(10.0);
        nB.getSampleSpace().setN(121);
        nB.getSampleSpace().setOffset(739l);


        Neuron nC = m.getNeuron("C");
        nC.setFrequency(30.0);
        nC.getSampleSpace().setN(234);
        nC.getSampleSpace().setOffset(867l);


        AikaDebugger.createAndShowGUI(doc,m);

        doc.process(m);

        System.out.println();
    }


    @Test
    public void gradientAndInduction2With2Docs() {
        TextModel m = new TextModel();

        m.setN(912);
        m.getTemplates().SAME_BINDING_TEMPLATE.addConjunctiveBias(-0.32);
        m.init();

        Document doc1 = new Document("A B ");
        doc1.setConfig(
                Util.getTestConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setEnableTraining(true)
        );
        processDoc(m, doc1);

        Neuron nA = m.getNeuron("A");
        nA.setFrequency(53.0);
        nA.getSampleSpace().setN(299);
        nA.getSampleSpace().setOffset(899l);

        Neuron nB = m.getNeuron("B");
        nB.setFrequency(10.0);
        nB.getSampleSpace().setN(121);
        nB.getSampleSpace().setOffset(739l);

        AikaDebugger.createAndShowGUI(doc1,m);

        doc1.process(m);


        Document doc2 = new Document("A C ");
        doc2.setConfig(
                Util.getTestConfig()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
                        .setEnableTraining(true)
        );
        processDoc(m, doc2);

        Neuron nC = m.getNeuron("C");
        nC.setFrequency(30.0);
        nC.getSampleSpace().setN(234);
        nC.getSampleSpace().setOffset(867l);

        AikaDebugger.createAndShowGUI(doc2,m);

        doc2.process(m);

        System.out.println();
    }

    private void processDoc(TextModel m, Document doc) {
        int i = 0;
        TextReference lastRef = null;
        for(String t: doc.getContent().split(" ")) {
            int j = i + t.length();
            lastRef = doc.processToken(m, lastRef, i, j, t).getReference();

            i = j + 1;
        }
    }
}
