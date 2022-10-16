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
package network.aika.neuron.linking;

import network.aika.neuron.activation.BindingActivation;
import network.aika.neuron.activation.LatentRelationActivation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.PatternActivation;
import network.aika.neuron.activation.text.TokenActivation;
import network.aika.neuron.conjunctive.LatentRelationNeuron;
import network.aika.neuron.conjunctive.RelationInputSynapse;
import network.aika.neuron.conjunctive.Scope;

import static network.aika.neuron.conjunctive.Scope.INPUT;

/**
 * @author Lukas Molzberger
 */
public class RelationLinkingUpVisitor extends LinkingUpVisitor {

    protected RelationInputSynapse relation;

    protected TokenActivation downOrigin;
    protected TokenActivation upOrigin;

    protected RelationLinkingUpVisitor(RelationLinkingDownVisitor parent, TokenActivation downOrigin, TokenActivation upOrigin) {
        super(parent, downOrigin);
        this.downOrigin = downOrigin;
        this.upOrigin = upOrigin;
        this.relation = parent.relation;
    }

    public PatternActivation getDownOrigin() {
        return downOrigin;
    }

    public TokenActivation getUpOrigin() {
        return upOrigin;
    }

    @Override
    public boolean compatible(Scope from, Scope to) {
        if(downOrigin == null)
            return false;

        if(from == INPUT)
            return downOrigin == upOrigin;

        if(from != to)
            return downOrigin != upOrigin;

        return false;
    }

    public void createRelation(Link l) {
        LatentRelationActivation latentRelAct = relation.getInput().createOrLookupLatentActivation(
                downOrigin,
                upOrigin
        );

        if(relation.linkExists(latentRelAct, (BindingActivation) l.getOutput()))
            return;

        relation.createLink(latentRelAct, (BindingActivation) l.getOutput());
    }
}
