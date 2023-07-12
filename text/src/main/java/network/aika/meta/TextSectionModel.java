package network.aika.meta;

import network.aika.Model;
import network.aika.elements.neurons.*;
import network.aika.elements.synapses.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TextSectionModel {


    private PhraseTemplateModel phraseModel;


    protected Model model;

    private final Logger log = LoggerFactory.getLogger(PhraseTemplateModel.class);

    protected NeuronProvider textSectionRelationPT;
    protected NeuronProvider textSectionRelationNT;

    protected NeuronProvider textSectionPatternN;

    protected NeuronProvider textSectionHeadlineBN;

    protected NeuronProvider textSectionBeginBN;

    protected NeuronProvider textSectionHintBN;

    protected NeuronProvider textSectionEndBN;

    protected NeuronProvider textSectionPatternCategory;


    protected double headlineInputPatternNetTarget = 5.0;

    protected double headlineInputPatternValueTarget;


    public TextSectionModel(PhraseTemplateModel phraseModel) {
        this.phraseModel = phraseModel;
        model = phraseModel.getModel();
    }

    protected void initTextSectionTemplates() {
        log.info("Text-Section");

        textSectionRelationPT = TokenPositionRelationNeuron.lookupRelation(model, -1, -300)
                .getProvider(true);

        textSectionRelationNT = TokenPositionRelationNeuron.lookupRelation(model, 300, 1)
                .getProvider(true);

        double netTarget = 2.5;
        double valueTarget = ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT
                .f(netTarget);

        textSectionPatternN = new PatternNeuron()
                .init(model, "Abstract Text-Section")
                .getProvider(true);

        textSectionHeadlineBN = new BindingNeuron()
                .init(model, "Abstract Text-Section-Headline")
                .getProvider(true);

        phraseModel.abstractNeurons.add(textSectionHeadlineBN);

        headlineInputPatternValueTarget = ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT
                .f(headlineInputPatternNetTarget);

        //     new BindingCategoryInputSynapse()
        new InputPatternSynapse()
                .setWeight(10.0)
                .init(phraseModel.patternCategory.getNeuron(), textSectionHeadlineBN.getNeuron())
                .adjustBias(headlineInputPatternValueTarget);


        textSectionBeginBN = new BindingNeuron()
                .init(model, "Abstract Text-Section-Begin")
                .getProvider(true);

        headlineToSectionBeginRelation();


        textSectionHintBN = new BindingNeuron()
                .init(model, "Abstract Text-Section Hint")
                .getProvider(true);

        textSectionEndBN = new BindingNeuron()
                .init(model, "Abstract Text-Section-End")
                .getProvider(true);

        sectionBeginToSectionEndRelation();
        sectionHintRelations((BindingNeuron) textSectionBeginBN.getNeuron(), (LatentRelationNeuron) textSectionRelationPT.getNeuron());
        sectionHintRelations((BindingNeuron) textSectionEndBN.getNeuron(), (LatentRelationNeuron) textSectionRelationNT.getNeuron());
    }

    private void headlineToSectionBeginRelation() {
        double prevNetTarget = textSectionHeadlineBN.getNeuron().getBias().getValue();
        double prevValueTarget = ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT
                .f(prevNetTarget);

        new RelationInputSynapse()
                .setWeight(5.0)
                .init(phraseModel.relPT.getNeuron(), textSectionBeginBN.getNeuron())
                .adjustBias();

        SamePatternSynapse spSyn = new SamePatternSynapse()
                .setWeight(10.0)
                .init(textSectionHeadlineBN.getNeuron(), textSectionBeginBN.getNeuron())
                .adjustBias(prevValueTarget);

        log.info("  HeadlineToSectionBeginRelation:  " + spSyn + " targetNetContr:" + -spSyn.getSynapseBias().getValue());
    }

    private void sectionBeginToSectionEndRelation() {
        double prevNetTarget = textSectionBeginBN.getNeuron().getBias().getValue();
        double prevValueTarget = ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT
                .f(prevNetTarget);

        new RelationInputSynapse()
                .setWeight(5.0)
                .init(textSectionRelationPT.getNeuron(), textSectionEndBN.getNeuron())
                .adjustBias();

        SamePatternSynapse spSyn = new SamePatternSynapse()
                .setWeight(10.0)
                .init(textSectionBeginBN.getNeuron(), textSectionEndBN.getNeuron())
                .adjustBias(prevValueTarget);

        log.info("  SectionBeginToSectionEndRelation:  " + spSyn + " targetNetContr:" + -spSyn.getSynapseBias().getValue());
    }

    private void sectionHintRelations(BindingNeuron fromBN, LatentRelationNeuron relN) {
        double prevNetTarget = fromBN.getBias().getValue();
        double prevValueTarget = ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT
                .f(prevNetTarget);

        new RelationInputSynapse()
                .setWeight(5.0)
                .init(relN, textSectionHintBN.getNeuron())
                .adjustBias();

        SamePatternSynapse spSyn = new SamePatternSynapse()
                .setWeight(10.0)
                .init(fromBN, textSectionHintBN.getNeuron())
                .adjustBias(prevValueTarget);

        log.info("  SectionHintRelations:  " + spSyn + " targetNetContr:" + -spSyn.getSynapseBias().getValue());
    }
}
