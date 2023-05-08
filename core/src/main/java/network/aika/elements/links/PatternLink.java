/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.aika.elements.links;

import network.aika.elements.activations.BindingActivation;
import network.aika.elements.activations.PatternActivation;
import network.aika.elements.neurons.Range;
import network.aika.fields.AbstractFunction;
import network.aika.elements.synapses.PatternSynapse;
import network.aika.elements.synapses.PositiveFeedbackSynapse;
import network.aika.fields.Field;
import network.aika.fields.SumField;
import network.aika.sign.Sign;
import network.aika.steps.link.LinkCounting;
import network.aika.visitor.Visitor;

import static network.aika.fields.ConstantField.ZERO;
import static network.aika.fields.FieldLink.linkAndConnect;
import static network.aika.fields.Fields.func;
import static network.aika.fields.Fields.scale;
import static network.aika.utils.Utils.TOLERANCE;

/**
 * @author Lukas Molzberger
 */
public class PatternLink extends ConjunctiveLink<PatternSynapse, BindingActivation, PatternActivation> {

    private AbstractFunction outputEntropy;
    private AbstractFunction informationGain;

    public PatternLink(PatternSynapse s, BindingActivation input, PatternActivation output) {
        super(s, input, output);

        LinkCounting.add(this);
    }

    @Override
    public void addInputLinkingStep() {
        super.addInputLinkingStep();

        PositiveFeedbackSynapse posFeedbackSyn = (PositiveFeedbackSynapse) input.getNeuron().getInputSynapse(output.getNeuronProvider());
        if(posFeedbackSyn != null && !posFeedbackSyn.linkExists(input))
            posFeedbackSyn.createAndInitLink(output, input);
    }

    @Override
    public void connectGradientFields() {
        gradient = new SumField(this, "Gradient", TOLERANCE);

        if(input != null)
            linkAndConnect(input.getGradient(), gradient);
        linkAndConnect(gradient, output.getGradient());

        Range r = (input != null ? input : output).getAbsoluteRange();

        informationGain = func(
                this,
                "Information-Gain",
                getInputPatternNet(),
                output.getNet(),
                (x1, x2) ->
                        synapse.getSurprisal(
                                Sign.getSign(x1),
                                Sign.getSign(x2),
                                r,
                                true
                        ),
                gradient
        );

        outputEntropy = scale(this, "-Entropy", -1,
                output.getEntropy(),
                gradient
        );
    }

    public Field getInputPatternNet() {
        if(input == null)
            return ZERO;

        PatternActivation inputPatternAct = input.getInputPatternActivation();
        if(inputPatternAct == null)
            return ZERO;

        return inputPatternAct.getNet();
    }

    public AbstractFunction getOutputEntropy() {
        return outputEntropy;
    }

    public AbstractFunction getInformationGain() {
        return informationGain;
    }

    @Override
    public void bindingVisit(Visitor v) {
    }

    @Override
    public void inhibVisit(Visitor v) {
    }
}
