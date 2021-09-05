package network.aika;

import network.aika.callbacks.FSSuspensionCallback;
import network.aika.debugger.AikaDebugger;
import network.aika.debugger.StepMode;
import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.text.TextReference;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;


public class TestFsModel {

    @Test
    public void testOpenModel() throws IOException {

        TextModel m = new TextModel(
                new FSSuspensionCallback(new File("F:/Model").toPath(), "AIKA-2.0-3", true)
        );

        m.open(false);
        m.init();
        m.getTemplates().SAME_BINDING_TEMPLATE.addConjunctiveBias(-0.32, false);

        {
            Document doc = generateDocument(m, "arbeit fair arbeitsvermittlung ", true);

            AikaDebugger debugger = AikaDebugger.createAndShowGUI(doc, m);
            debugger.setStepMode(StepMode.ACT);

            doc.process(m);
        }


        {
            Document doc = generateDocument(m, "arbeit fair arbeitsvermittlung ", false);

            AikaDebugger debugger = AikaDebugger.createAndShowGUI(doc, m);
            debugger.setStepMode(StepMode.ACT);

            doc.process(m);
        }

        m.close();
    }

    private Document generateDocument(TextModel m, String txt, boolean train) {
        Document doc = new Document(txt);

        Config c = new TestConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.011)
                .setEnableTraining(train);
        doc.setConfig(c);

        int i = 0;
        TextReference lastRef = null;
        for(String t: doc.getContent().split(" ")) {
            int j = i + t.length();
            lastRef = doc.processToken(m, lastRef, i, j, "W-" + t).getReference();

            i = j + 1;
        }
        return doc;
    }
}
