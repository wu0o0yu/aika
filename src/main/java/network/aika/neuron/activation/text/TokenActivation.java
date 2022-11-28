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

import network.aika.neuron.Range;
import network.aika.neuron.activation.*;
import network.aika.neuron.conjunctive.LatentRelationNeuron;
import network.aika.neuron.conjunctive.text.TokenNeuron;
import network.aika.neuron.visitor.DownVisitor;
import network.aika.text.Document;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;



/**
 *
 * @author Lukas Molzberger
 */
public class TokenActivation extends PatternActivation {

    private Integer position;

    private Map<LatentRelationNeuron, LatentRelationActivation> toRelations = new TreeMap<>(
            Comparator.comparingLong(n -> n.getId())
    );

    public TokenActivation(int id, Integer pos, int begin, int end, Document doc, TokenNeuron tokenNeuron) {
        super(id, doc, tokenNeuron);
        position = pos;
        range = new Range(begin, end);

        doc.registerTokenActivation(this);
    }

    @Override
    public void updateRange() {
    }

    @Override
    protected void connectWeightUpdate() {
        // Input activations don't need weight updates
    }

    public Map<LatentRelationNeuron, LatentRelationActivation> getToRelations() {
        return toRelations;
    }

    @Override
    public void bindingVisitDown(DownVisitor v, Link lastLink) {
        super.bindingVisitDown(v, lastLink);
        v.expandRelations(this);
    }

    public Integer getPosition() {
        return position;
    }

    public boolean isInput() {
        return true;
    }

}
