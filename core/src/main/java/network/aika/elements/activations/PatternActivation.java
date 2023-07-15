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
import network.aika.elements.links.Link;
import network.aika.Scope;
import network.aika.fields.*;
import network.aika.elements.neurons.PatternNeuron;
import network.aika.visitor.linking.binding.BindingVisitor;
import network.aika.visitor.linking.inhibitory.InhibitoryVisitor;
import network.aika.sign.Sign;
import network.aika.visitor.linking.pattern.PatternCategoryVisitor;

import static network.aika.fields.Fields.*;
import static network.aika.utils.Utils.TOLERANCE;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternActivation extends ConjunctiveActivation<PatternNeuron> {

    private FieldFunction entropy;

    private Double[] cachedSurprisal;

    public PatternActivation(int id, Thought t, PatternNeuron patternNeuron) {
        super(id, t, patternNeuron);
    }

    @Override
    public void connectGradientFields() {
        entropy = func(
                this,
                "entropy",
                TOLERANCE,
                net,
                x -> getSurprisal(Sign.getSign(x)),
                gradient
        );

        super.connectGradientFields();
    }

    @Override
    protected void connectWeightUpdate() {
        updateValue = scale(
                this,
                "updateValue = lr * grad * f'(net)",
                getConfig().getLearnRate(neuron.isAbstract()),
                mul(
                        this,
                        "gradient * f'(net)",
                        gradient,
                        netOuterGradient
                )
        );

        super.connectWeightUpdate();
    }

    public FieldOutput getEntropy() {
        return entropy;
    }

    @Override
    public void bindingVisit(BindingVisitor v, Link lastLink, int depth) {
        super.bindingVisit(v, lastLink, depth);

        v.up(this, depth);
    }

    @Override
    public void inhibVisit(InhibitoryVisitor v, Link lastLink, int depth) {
        if(lastLink == null)
            super.inhibVisit(v, lastLink, depth);

        v.up(this, depth);
    }

    @Override
    public void patternCatVisit(PatternCategoryVisitor v, Link lastLink, int depth) {
        if(v.getDirection().isDown() && lastLink != null && lastLink.getSynapse().getScope() == Scope.INPUT)
            v.up(this, depth);
        else
            super.patternCatVisit(v, lastLink, depth);
    }

    public double getSurprisal(Sign sign) {
        if(cachedSurprisal == null)
            cachedSurprisal = new Double[2];

        Double s = cachedSurprisal[sign.index()];
        if(s == null) {
            s = neuron.getSurprisal(sign, getAbsoluteRange(), true);
            cachedSurprisal[sign.index()] = s;
        }
        return s;
    }
}
