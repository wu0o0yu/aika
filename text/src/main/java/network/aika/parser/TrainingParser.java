package network.aika.parser;


import network.aika.Config;
import network.aika.debugger.AIKADebugger;
import network.aika.elements.activations.TokenActivation;
import network.aika.text.Document;
import network.aika.tokenizer.TokenConsumer;

import static network.aika.meta.LabelUtil.generateTemplateInstanceLabels;
import static network.aika.parser.ParserPhase.TRAINING;
import static network.aika.steps.Phase.ANNEAL;
import static network.aika.steps.keys.QueueKey.MAX_ROUND;

public abstract class TrainingParser extends Parser {

    @Override
    protected Document initDocument(String txt, Context context, ParserPhase phase) {
        Document doc = new Document(getTemplateModel().getModel(), txt);

        Config conf = new Config()
                .setAlpha(null)
                .setLearnRate(0.01)
                .setTrainingEnabled(phase == TRAINING)
                .setMetaInstantiationEnabled(phase == TRAINING)
                .setCountingEnabled(true);

        doc.setConfig(conf);

        return doc;
    }

    @Override
    public Document process(String txt, Context context, ParserPhase phase) {
        Document doc = process(txt, context, phase);

        AIKADebugger debugger = null;

        doc.setInstantiationCallback(act ->
                generateTemplateInstanceLabels(act)
        );

        doc.setFeedbackTriggerRound();

        TokenConsumer tokenConsumer = (n, pos, begin, end) -> {
            TokenActivation tAct = doc.addToken(n, pos, begin, end);
            tAct.setNet(10.0);
        };

        getTokenizer().tokenize(doc.getContent(), null, tokenConsumer);

        doc.anneal();

        doc.process(MAX_ROUND, ANNEAL);

        waitForClick(debugger);

        doc.instantiateTemplates();

        waitForClick(debugger);

        doc.train();

        waitForClick(debugger);

        doc.postProcessing();
        doc.updateModel();
        doc.disconnect();

        return doc;
    }

}
