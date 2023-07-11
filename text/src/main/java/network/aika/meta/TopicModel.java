package network.aika.meta;

import network.aika.Model;
import network.aika.elements.neurons.NeuronProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicModel {
    private final Logger log = LoggerFactory.getLogger(PhraseTemplateModel.class);


    private PhraseTemplateModel phraseModel;


    protected Model model;


    protected NeuronProvider topicPatternN;

    protected NeuronProvider topicPatternCategory;


    public TopicModel(PhraseTemplateModel phraseModel) {
        this.phraseModel = phraseModel;
        model = phraseModel.getModel();
    }

    protected void initTopicTemplates() {

    }
}
