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
package network.aika.neuron.activation.text;

import network.aika.fields.ValueSortedQueueField;
import network.aika.neuron.Range;
import network.aika.neuron.activation.*;
import network.aika.neuron.conjunctive.text.TokenNeuron;
import network.aika.neuron.linking.RelationLinkingVisitor;
import network.aika.neuron.linking.Visitor;
import network.aika.text.Document;


/**
 *
 * @author Lukas Molzberger
 */
public class TokenActivation extends PatternActivation {

    private Integer position;

    public TokenActivation(int id, Integer pos, int begin, int end, Document doc, TokenNeuron tokenNeuron) {
        super(id, doc, tokenNeuron);
        position = pos;
        range = new Range(begin, end);

        doc.registerTokenActivation(this);
    }

    @Override
    public void visit(Visitor v, Link lastLink) {
        super.visit(v, lastLink);

        if (v instanceof RelationLinkingVisitor) {
            RelationLinkingVisitor relV = (RelationLinkingVisitor) v;

            relV.getRelation()
                    .evaluateLatentRelation(this, relV.getRelationDir())
                    .forEach(relTokenAct ->
                            next(relV.up(this, relTokenAct))
                    );
        }
    }

    public Integer getPosition() {
        return position;
    }

    protected void initNet() {
        netUB = new ValueSortedQueueField(this, "net UB");
        netLB = new ValueSortedQueueField(this, "net LB");
    }

    public boolean isInput() {
        return true;
    }

    @Override
    public Range getRange() {
        return range;
    }
}
