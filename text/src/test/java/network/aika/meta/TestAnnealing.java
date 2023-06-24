package network.aika.meta;

import network.aika.Model;
import network.aika.parser.ParserPhase;
import network.aika.parser.TrainingParser;
import network.aika.tokenizer.SimpleCharTokenizer;
import network.aika.tokenizer.Tokenizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static network.aika.parser.ParserPhase.COUNTING;
import static network.aika.parser.ParserPhase.TRAINING;


public class TestAnnealing extends TrainingParser {

    private AbstractTemplateModel templateModel;
    private Tokenizer tokenizer;

    @BeforeEach
    public void init() {
        Model model = new Model();

        templateModel = new SyllableTemplateModel(model);
        templateModel.initStaticNeurons();

        model.setN(0);

        tokenizer = new SimpleCharTokenizer(templateModel);
    }

    @Test
    public void testAnnealing() {
        process("ab", null, COUNTING);
        templateModel.initTemplates();
        process("ab", null, TRAINING);
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
