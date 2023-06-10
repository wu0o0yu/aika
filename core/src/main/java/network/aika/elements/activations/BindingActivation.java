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
package network.aika.elements.activations;

import network.aika.Thought;
import network.aika.elements.links.InputPatternLink;
import network.aika.elements.links.Link;
import network.aika.elements.links.PositiveFeedbackLink;
import network.aika.elements.synapses.PositiveFeedbackSynapse;
import network.aika.fields.*;
import network.aika.elements.neurons.BindingNeuron;
import network.aika.visitor.DownVisitor;
import network.aika.visitor.linking.pattern.PatternCategoryDownVisitor;
import network.aika.visitor.linking.pattern.PatternCategoryUpVisitor;

import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;
import static network.aika.fields.Fields.isTrue;
import static network.aika.utils.Utils.TOLERANCE;

/**
 * @author Lukas Molzberger
 */
public class BindingActivation extends ConjunctiveActivation<BindingNeuron> {

    private boolean isInput;

    public BindingActivation(int id, Thought t, BindingNeuron n) {
        super(id, t, n);
    }

    @Override
    public boolean isActiveTemplateInstance() {
        return isNewInstance || (
                isTrue(net, MAX_VALUE, 0.0) &&
                        getOutputPatternActivations()
                                .anyMatch(Activation::isFired)
        );
    }

    public Stream<PatternActivation> getOutputPatternActivations() {
        return getInputLinksByType(PositiveFeedbackLink.class)
                .map(Link::getInput);
    }

    public PatternActivation getInputPatternActivation() {
        return getInputLinksByType(InputPatternLink.class)
                .map(Link::getInput)
                .findFirst()
                .orElse(null);
    }

    @Override
    protected void connectWeightUpdate() {
        updateValue = new MultiInputField(this, "updateValue", TOLERANCE);

        super.connectWeightUpdate();
    }

    @Override
    public void patternVisitDown(DownVisitor v, Link lastLink) {
        super.patternVisitDown(v, lastLink);
        v.up(this);
    }

    @Override
    public void patternCatVisitDown(PatternCategoryDownVisitor v, Link lastLink) {
        v.setReferenceAct(this);
        super.patternCatVisitDown(v, lastLink);
    }

    @Override
    public void patternCatVisitUp(PatternCategoryUpVisitor v, Link lastLink) {
        Activation template = getTemplate();
        Activation refTemplate = v.getReferenceAct().getTemplate();

        if(template != null && template == refTemplate)
            super.patternCatVisitUp(v, lastLink);
    }

    @Override
    public void selfRefVisitDown(DownVisitor v, Link lastLink) {
        v.up(this);
    }

    @Override
    protected void initDummyLinks() {
        neuron.getInputSynapsesByType(PositiveFeedbackSynapse.class)
                .forEach(s ->
                        s.initDummyLink(this)
                );
    }

    public boolean isInput() {
        return isInput;
    }

    public void setInput(boolean input) {
        isInput = input;
    }

    public void updateBias(double u) {
        getNet().receiveUpdate(0, u);
    }
}
