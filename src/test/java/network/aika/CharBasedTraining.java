package network.aika;

import network.aika.text.Document;
import network.aika.text.TextModel;
import network.aika.text.TokenActivation;

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

        TokenActivation lastToken = null;
        for(int i = 0; i < doc.length(); i++) {
            char c = doc.charAt(i);

            TokenActivation currentToken = doc.addToken(model, "" + c, i, i + 1);
            TokenActivation.addRelation(lastToken, currentToken);

            lastToken = currentToken;
        }
        doc.process(model);

        System.out.println(); // doc.activationsToString()
    }
}
