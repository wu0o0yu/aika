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
import network.aika.enums.Scope;
import network.aika.fields.*;
import network.aika.elements.neurons.BindingNeuron;
import network.aika.visitor.operator.SelfRefOperator;
import network.aika.visitor.inhibitory.InhibitoryVisitor;
import network.aika.visitor.pattern.PatternCategoryVisitor;
import network.aika.visitor.pattern.PatternVisitor;

import java.util.Objects;
import java.util.stream.Stream;

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
                isTrue(net, 0.0) &&
                        getOutputPatternActivations()
                                .anyMatch(Activation::isFired)
        );
    }

    public Stream<PatternActivation> getOutputPatternActivations() {
        return getInputLinksByType(PositiveFeedbackLink.class)
                .map(Link::getInput)
                .filter(Objects::nonNull);
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

    public static boolean isSelfRef(BindingActivation in, BindingActivation out, Scope identityRef) {
        if (in.isAbstract() && !out.isAbstract())
            return in.isSelfRef((BindingActivation) out.getTemplate(), identityRef);
        else if (!in.isAbstract() && out.isAbstract())
            return out.isSelfRef((BindingActivation) in.getTemplate(), identityRef);
        else
            return in.isSelfRef(out, identityRef);
    }

    private boolean isSelfRef(BindingActivation out, Scope identityRef) {
        if(this == out)
            return true;

        SelfRefOperator op = new SelfRefOperator(out);
        new InhibitoryVisitor(thought, op, identityRef)
                .start(this);
        return op.isSelfRef();
    }

    @Override
    public void patternVisit(PatternVisitor v, Link lastLink, int depth) {
        super.patternVisit(v, lastLink, depth);
        v.up(this, depth);
    }

    @Override
    public void patternCatVisit(PatternCategoryVisitor v, Link lastLink, int depth) {
        if(v.getDirection().isDown()) {
            v.setReferenceAct(this);
            super.patternCatVisit(v, lastLink, depth);
        } else {
            CategoryActivation cAct = getCategoryActivation();
            CategoryActivation refCAct = v.getReferenceAct().getCategoryActivation();

            if(cAct != null && cAct == refCAct)
                super.patternCatVisit(v, lastLink, depth);
        }
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
        getNet().receiveUpdate(false, u);
    }
}
