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
package network.aika.neuron.activation;

import network.aika.Thought;
import network.aika.fields.FieldFunction;
import network.aika.fields.FieldOutput;
import network.aika.fields.SumField;
import network.aika.neuron.Range;
import network.aika.neuron.conjunctive.PatternNeuron;
import network.aika.neuron.visitor.DownVisitor;
import network.aika.sign.Sign;

import static network.aika.fields.FieldLink.connect;
import static network.aika.fields.Fields.*;

/**
 *
 * @author Lukas Molzberger
 */
public class PatternActivation extends ConjunctiveActivation<PatternNeuron> {

    private FieldFunction entropy;

    protected SumField outputGradient;

    public PatternActivation(int id, Thought t, PatternNeuron patternNeuron) {
        super(id, t, patternNeuron);
    }

    @Override
    public void connectGradientFields() {
        entropy = func(
                this,
                "Entropy",
                netUB,
                x ->
                        getNeuron().getSurprisal(
                                Sign.getSign(x),
                                getAbsoluteRange(),
                                true
                        ),
                forwardsGradient
        );

        outputGradient = new SumField(this, "outputGradient");

        super.connectGradientFields();

        connect(forwardsGradient, outputGradient);
        connect(backwardsGradientOut, outputGradient);
    }

    @Override
    public FieldOutput getOutputGradient() {
        return outputGradient;
    }

    public FieldOutput getEntropy() {
        return entropy;
    }

    @Override
    public void selfRefVisitDown(DownVisitor v, Link lastLink) {
        v.up(this);
    }

    @Override
    public void bindingVisitDown(DownVisitor v, Link lastLink) {
        super.bindingVisitDown(v, lastLink);
        v.up(this);
    }

    @Override
    public void inhibVisitDown(DownVisitor v, Link lastLink) {
        super.inhibVisitDown(v, lastLink);
        v.up(this);
    }
}
