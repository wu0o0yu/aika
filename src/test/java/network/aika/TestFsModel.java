package network.aika;

import network.aika.callbacks.FSSuspensionCallback;
import network.aika.debugger.AikaDebugger;
import network.aika.debugger.StepMode;
import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.text.TokenActivation;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;


public class TestFsModel {

    @Test
    public void testOpenModel() throws IOException {

        TextModel m = new TextModel(
                new FSSuspensionCallback(new File("/Users/lukas.molzberger/models").toPath(), "AIKA-2.0-10", true)
        );

        m.open(false);
        m.init();

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

        Config c = Util.getTestConfig()
                .setAlpha(0.99)
                .setLearnRate(-0.011)
                .setEnableTraining(train);
        doc.setConfig(c);

        int i = 0;
        TokenActivation lastToken = null;
        for(String t: doc.getContent().split(" ")) {
            int j = i + t.length();
            TokenActivation currentToken = doc.addToken(m,"W-" + t, i, j);
            TokenActivation.addRelation(lastToken, currentToken);

            lastToken = currentToken;
            i = j + 1;
        }
        return doc;
    }
}
