package network.aika;

import network.aika.callbacks.FSSuspensionCallback;
import network.aika.debugger.AikaDebugger;
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
        m.getTemplates().SAME_BINDING_TEMPLATE.setDirectConjunctiveBias(-0.32);

        Document doc = new Document("arbeit fair arbeitsvermittlung ");

        Config c = new TestConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.011)
                .setEnableTraining(true);
        doc.setConfig(c);

        int i = 0;
        TextReference lastRef = null;
        for(String t: doc.getContent().split(" ")) {
            int j = i + t.length();
            lastRef = doc.processToken(m, lastRef, i, j, "W-" + t).getReference();

            i = j + 1;
        }

        AikaDebugger.createAndShowGUI(doc,m);

        doc.process(m);

        m.close();
    }
}
