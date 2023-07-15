package network.aika.meta;

import network.aika.Scope;
import network.aika.elements.neurons.ActivationFunction;
import network.aika.elements.neurons.BindingNeuron;
import network.aika.elements.neurons.InhibitoryNeuron;
import network.aika.elements.neurons.PatternNeuron;
import network.aika.elements.synapses.InhibitorySynapse;
import network.aika.elements.synapses.NegativeFeedbackSynapse;
import network.aika.elements.synapses.PatternSynapse;
import network.aika.elements.synapses.PositiveFeedbackSynapse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static network.aika.meta.AbstractTemplateModel.POS_MARGIN;

public class NetworkUtils {
    private static final Logger log = LoggerFactory.getLogger(AbstractTemplateModel.class);


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
}
