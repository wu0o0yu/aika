package network.aika.parser;


import network.aika.debugger.AIKADebugger;
import network.aika.elements.activations.TokenActivation;
import network.aika.meta.AbstractTemplateModel;
import network.aika.text.Document;
import network.aika.tokenizer.TokenConsumer;
import network.aika.tokenizer.Tokenizer;


import static network.aika.steps.Phase.ANNEAL;
import static network.aika.steps.Phase.INFERENCE;
import static network.aika.steps.keys.QueueKey.MAX_ROUND;


public abstract class Parser {

    public abstract Tokenizer getTokenizer();

    protected Document initDocument(String txt, Context context, ParserPhase phase) {
        return null;
    }

    protected abstract AbstractTemplateModel getTemplateModel();


    public Document process(String txt, Context context, ParserPhase phase) {
        Document doc = initDocument(txt, context, phase);

        TokenConsumer tokenConsumer = (n, pos, begin, end) -> {
            TokenActivation tAct = doc.addToken(n, pos, begin, end);
            tAct.setNet(getTemplateModel().getInputPatternNetTarget());
        };

        doc.setFeedbackTriggerRound();

        getTokenizer().tokenize(doc.getContent(), context, tokenConsumer);

        doc.process(MAX_ROUND, INFERENCE);

        doc.anneal();

        doc.process(MAX_ROUND, ANNEAL);

        return doc;
    }


    protected static void waitForClick(AIKADebugger debugger) {
        if(debugger != null)
            debugger.getStepManager().waitForClick();
    }
}
