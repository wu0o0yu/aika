package network.aika;

import network.aika.neuron.activation.Reference;
import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.text.TextReference;

public class CharBasedTraining {

    private TextModel model;

    public void init() {
        model = new TextModel();
    }

    public TextModel getModel() {
        return model;
    }

    public void train(String word) {
        Document doc = new Document(word);
        doc.setConfig(
                new Config()
                        .setAlpha(0.99)
                        .setLearnRate(-0.1)
        );

        System.out.println("  " + word);

        TextReference lastRef = null;
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.charAt(i);

            lastRef = (TextReference) doc.processToken(model, lastRef, i, i + 1, "" + c).getReference();
        }
        doc.process(model);

        System.out.println(); // doc.activationsToString()
    }
}
