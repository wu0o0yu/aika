package network.aika.parser;

import network.aika.Model;
import network.aika.debugger.AIKADebugger;
import network.aika.elements.activations.TokenActivation;
import network.aika.text.Document;
import network.aika.tokenizer.TokenConsumer;
import network.aika.tokenizer.Tokenizer;
import network.aika.tokenizer.TokenizerContext;

import java.util.Set;

import static network.aika.steps.Phase.INFERENCE;
import static network.aika.steps.keys.QueueKey.MAX_ROUND;

public class Parser {

    protected Model model;
    protected Tokenizer tokenizer;

    public Parser(Model model, Tokenizer tokenizer) {
        this.model = model;
        this.tokenizer = tokenizer;
    }

    protected Document initDocument(String txt) {
        return null;
    }

    public void parse(Document doc, TokenizerContext context) {
        TokenConsumer tokenConsumer = (n, pos, begin, end) -> {
            TokenActivation tAct = doc.addToken(n, pos, begin, end);
            tAct.setNet(10.0);
        };

        tokenizer.tokenize(doc.getContent(), context, tokenConsumer);

        doc.process(MAX_ROUND, INFERENCE);

        doc.anneal();
    }


    protected static void waitForClick(AIKADebugger debugger) {
        if(debugger != null)
            debugger.getStepManager().waitForClick();
    }
}
