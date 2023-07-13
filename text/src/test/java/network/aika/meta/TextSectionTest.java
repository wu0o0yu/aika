package network.aika.meta;

import network.aika.Model;
import network.aika.debugger.AIKADebugger;
import network.aika.elements.activations.Activation;
import network.aika.parser.Context;
import network.aika.parser.ParserPhase;
import network.aika.parser.TrainingParser;
import network.aika.text.Document;
import network.aika.tokenizer.SimpleWordTokenizer;
import network.aika.tokenizer.Tokenizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static network.aika.parser.ParserPhase.COUNTING;
import static network.aika.parser.ParserPhase.TRAINING;

public class TextSectionTest extends TrainingParser {

    private PhraseTemplateModel templateModel;
    private Tokenizer tokenizer;

    private String exampleTxt = "Java Softwaredeveloper\n" +
            " \n" +
            "Bla bla \n" +
            "\n" +
            "Your Tasks\n" +
            "Bla programming testing bla \n" +
            "\n" +
            "Your Profile\n" +
            "Bla java solr bla \n" +
            "\n";

    @BeforeEach
    public void init() {
        Model model = new Model();

        templateModel = new PhraseTemplateModel(model);
        templateModel.initStaticNeurons();

        model.setN(0);

        tokenizer = new SimpleWordTokenizer(templateModel);
    }

    @Override
    protected Document initDocument(String txt, Context context, ParserPhase phase) {
        Document doc = super.initDocument(txt, context, phase);
        if(phase == TRAINING) {
            AIKADebugger.createAndShowGUI(doc);
        }

        return doc;
    }

    @Override
    public boolean check(Activation iAct) {
        return true;
    }

    @Test
    public void testTextSections() {
        log.info("Start");
        process(exampleTxt, null, COUNTING);
        templateModel.initTemplates();
        process(exampleTxt, null, TRAINING);
    }

    @Override
    protected AbstractTemplateModel getTemplateModel() {
        return templateModel;
    }

    @Override
    public Tokenizer getTokenizer() {
        return tokenizer;
    }

}
