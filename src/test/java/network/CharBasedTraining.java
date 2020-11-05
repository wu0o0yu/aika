package network;

import network.aika.Config;
import network.aika.text.Document;
import network.aika.text.TextModel;

public class CharBasedTraining {

    private TextModel model;

    public void init() {
        model = new TextModel();
    }

    public TextModel getModel() {
        return model;
    }

    public void train(String word) {
        Document doc = new Document(word,
                new Config()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
        );
        System.out.println("  " + word);

        for(int i = 0; i < doc.length(); i++) {
            char c = doc.charAt(i);

            doc.processToken(model, i, i + 1, "" + c);
        }
        doc.process(model);

        System.out.println(); // doc.activationsToString()
    }
}
