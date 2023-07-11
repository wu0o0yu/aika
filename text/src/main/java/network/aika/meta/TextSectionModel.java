package network.aika.meta;

import network.aika.Model;
import network.aika.elements.neurons.ActivationFunction;
import network.aika.elements.neurons.BindingNeuron;
import network.aika.elements.neurons.NeuronProvider;
import network.aika.elements.neurons.PatternNeuron;
import network.aika.elements.synapses.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static network.aika.meta.AbstractTemplateModel.PASSIVE_SYNAPSE_WEIGHT;

public class TextSectionModel {


    private PhraseTemplateModel phraseModel;


    protected Model model;

    private final Logger log = LoggerFactory.getLogger(PhraseTemplateModel.class);


    protected NeuronProvider textSectionPatternN;

    protected NeuronProvider textSectionHeadlineBN;

    protected NeuronProvider textSectionBeginBN;

    protected NeuronProvider textSectionHintBN;

    protected NeuronProvider textSectionEndBN;

    protected NeuronProvider textSectionPatternCategory;

    public TextSectionModel(PhraseTemplateModel phraseModel) {
        this.phraseModel = phraseModel;
        model = phraseModel.getModel();
    }

    protected void initTextSectionTemplates() {
        log.info("Text-Section");

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

        new PrimaryInhibitorySynapse()
                .setWeight(PASSIVE_SYNAPSE_WEIGHT)
                .init(inputToken.getNeuron(), inhibitoryN.getNeuron());

        new InhibitorySynapse(Scope.INPUT)
                .setWeight(1.0)
                .init(textSectionHeadlineBN.getNeuron(), inhibitoryN.getNeuron());

        new NegativeFeedbackSynapse()
                .setWeight(getNegMargin(pos) * -netTarget)
                .init(inhibitoryN.getNeuron(), bn);

        new RelationInputSynapse()
                .setWeight(5.0)
                .init(relPT.getNeuron(), bn)
                .adjustBias();

        SamePatternSynapse spSyn = new SamePatternSynapse()
                .setWeight(10.0)
                .init(lastBN, bn)
                .adjustBias(prevValueTarget);

        System.out.println("  " + spSyn + " targetNetContr:" + -spSyn.getSynapseBias().getValue());
    }

}
