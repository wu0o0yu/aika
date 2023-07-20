package network.aika.meta;

import network.aika.Model;
import network.aika.elements.activations.Activation;
import network.aika.elements.synapses.Synapse;
import network.aika.parser.TrainingParser;
import network.aika.tokenizer.SimpleWordTokenizer;
import network.aika.tokenizer.Tokenizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static network.aika.parser.ParserPhase.COUNTING;
import static network.aika.parser.ParserPhase.TRAINING;


public class AnnealingTest extends TrainingParser {

    private AbstractTemplateModel templateModel;
    private Tokenizer tokenizer;

    @BeforeEach
    public void init() {
        Model model = new Model();

        templateModel = new PhraseTemplateModel(model);
        templateModel.initStaticNeurons();

        model.setN(0);

        tokenizer = new SimpleWordTokenizer(templateModel);
    }

    @Override
    public boolean check(Synapse s, Activation iAct) {
        return true;
    }

    @Test
    public void testAnnealing() {
        process("a b", null, COUNTING);
        templateModel.initTemplates();
        process("a b", null, TRAINING);
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
