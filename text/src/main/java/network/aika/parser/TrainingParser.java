package network.aika.parser;


import network.aika.Config;
import network.aika.Model;
import network.aika.debugger.AIKADebugger;
import network.aika.elements.activations.TokenActivation;
import network.aika.meta.AbstractTemplateModel;
import network.aika.text.Document;
import network.aika.tokenizer.TokenConsumer;
import network.aika.tokenizer.Tokenizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static network.aika.meta.LabelUtil.generateTemplateInstanceLabels;
import static network.aika.parser.TrainingPhase.TRAINING;
import static network.aika.steps.Phase.ANNEAL;
import static network.aika.steps.Phase.INFERENCE;
import static network.aika.steps.keys.QueueKey.MAX_ROUND;

public class TrainingParser extends Parser {

    AbstractTemplateModel templateModel;

    public TrainingParser(AbstractTemplateModel templateModel, Tokenizer tokenizer) {
        super(templateModel.getModel(), tokenizer);
        this.templateModel = templateModel;
    }

    protected Document initDocument(String txt, TrainingPhase phase) {
        Document doc = new Document(model, txt);

        Config conf = new Config()
                .setAlpha(null)
                .setLearnRate(0.01)
                .setTrainingEnabled(phase == TRAINING)
                .setMetaInstantiationEnabled(phase == TRAINING)
                .setCountingEnabled(true);

        doc.setConfig(conf);

        return doc;
    }

    public void train(String txt, TrainingPhase phase) {
        Document doc = initDocument(txt, phase);

        doc.setInstantiationCallback(act ->
                generateTemplateInstanceLabels(act)
        );


        doc.setFeedbackTriggerRound();

        List<TokenActivation> tokenActs = new ArrayList<>();

        TokenConsumer tc = (n, pos, begin, end) -> {
            tokenActs.add(
                    doc.addToken(n, pos, begin, end)
            );
        };

        tokenizer.tokenize(doc.getContent(), null, tc);

        if (phase == TRAINING) {
//            determineMaxSurprisalToken(doc, tokenActs);

/*            doc.setInstantiationCallback(act ->
                    initTemplateInstanceNeuron(act, rule)
            );
*/
            for(TokenActivation tAct: tokenActs) {
                tAct.setNet(templateModel.getInputPatternNetTarget());
                doc.process(MAX_ROUND, INFERENCE);
            }
        }

        doc.anneal();

        doc.process(MAX_ROUND, ANNEAL);

//        waitForClick(debugger);

        doc.instantiateTemplates();

//        waitForClick(debugger);

        doc.train();

//        waitForClick(debugger);

        doc.postProcessing();
        doc.updateModel();
        doc.disconnect();
    }
}
