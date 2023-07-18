package network.aika.meta;

import network.aika.Scope;
import network.aika.elements.neurons.*;
import network.aika.elements.synapses.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static network.aika.meta.AbstractTemplateModel.POS_MARGIN;

public class NetworkMotivs {
    private static final Logger log = LoggerFactory.getLogger(NetworkMotivs.class);

    protected static double PASSIVE_SYNAPSE_WEIGHT = 0.0;

    private static final String CATEGORY_LABEL = " Category";

    public static void addNegativeFeedbackLoop(BindingNeuron bn, InhibitoryNeuron in, double weight) {
        new InhibitorySynapse(Scope.INPUT)
                .setWeight(1.0)
                .init(bn, in);

        new NegativeFeedbackSynapse()
                .setWeight(weight)
                .init(in, bn)
                .adjustBias();
    }

    public static void addPositiveFeedbackLoop(
            BindingNeuron bn,
            PatternNeuron pn,
            double weight,
            double patternValueTarget,
            double netTarget,
            double weakInputMargin,
            boolean isOptional
    ) {
        double valueTarget = ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT
                .f(netTarget);

        PatternSynapse pSyn = new PatternSynapse()
                .setWeight(weight)
                .setOptional(isOptional)
                .init(bn, pn)
                .adjustBias(valueTarget + weakInputMargin);

        log.info("  " + pSyn + " targetNetContr:" + -pSyn.getSynapseBias().getValue());

        PositiveFeedbackSynapse posFeedSyn = new PositiveFeedbackSynapse()
                .setWeight(POS_MARGIN * (netTarget / patternValueTarget))
                .init(pn, bn)
                .adjustBias(patternValueTarget);

        log.info("  " + posFeedSyn + " targetNetContr:" + -posFeedSyn.getSynapseBias().getValue());
    }

    public static void addRelation(
            BindingNeuron lastBN,
            BindingNeuron bn,
            LatentRelationNeuron rel,
            double relWeight,
            double spsWeight
    ) {
        new RelationInputSynapse()
                .setWeight(relWeight)
                .init(rel, bn)
                .adjustBias();

        double prevNetTarget = lastBN.getBias().getValue();
        double prevValueTarget = ActivationFunction.RECTIFIED_HYPERBOLIC_TANGENT
                .f(prevNetTarget);

        SamePatternSynapse spSyn = new SamePatternSynapse()
                .setWeight(spsWeight)
                .init(lastBN, bn)
                .adjustBias(prevValueTarget);

        log.info("  " + spSyn + " targetNetContr:" + -spSyn.getSynapseBias().getValue());
    }

    public static PatternCategoryNeuron makeAbstract(PatternNeuron n) {
        PatternCategoryNeuron patternCategory = new PatternCategoryNeuron()
                .init(n.getModel(), n.getLabel() + CATEGORY_LABEL);

        patternCategory.getProvider(true);

        new PatternCategoryInputSynapse()
                .setWeight(PASSIVE_SYNAPSE_WEIGHT)
                .init(patternCategory, n);

        return patternCategory;
    }

    public static InhibitoryCategoryNeuron makeAbstract(InhibitoryNeuron n) {
        InhibitoryCategoryNeuron inhibCategory = new InhibitoryCategoryNeuron(Scope.SAME)
                .init(n.getModel(), n.getLabel() + CATEGORY_LABEL);

        inhibCategory.getProvider(true);

        new InhibitoryCategoryInputSynapse()
                .setWeight(1.0)
                .init(inhibCategory, n);

        return inhibCategory;
    }

    public static BindingCategoryNeuron makeAbstract(BindingNeuron n) {
        BindingCategoryNeuron bindingCategory = new BindingCategoryNeuron()
                .init(n.getModel(), n.getLabel() + CATEGORY_LABEL);

        bindingCategory.getProvider(true);

        new BindingCategoryInputSynapse()
                .setWeight(PASSIVE_SYNAPSE_WEIGHT)
                .init(bindingCategory, n);

        return bindingCategory;
    }
}
