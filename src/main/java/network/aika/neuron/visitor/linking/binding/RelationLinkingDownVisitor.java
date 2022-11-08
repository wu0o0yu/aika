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
package network.aika.neuron.visitor.linking.binding;

import network.aika.Thought;
import network.aika.direction.Direction;
import network.aika.neuron.activation.text.TokenActivation;
import network.aika.neuron.conjunctive.RelationInputSynapse;
import network.aika.neuron.visitor.linking.LinkingOperator;

/**
 * @author Lukas Molzberger
 */
public class RelationLinkingDownVisitor extends BindingDownVisitor {

    protected RelationInputSynapse relation;
    protected Direction relationDir;


    public RelationLinkingDownVisitor(Thought t, LinkingOperator operator, RelationInputSynapse rel, Direction relationDir) {
        super(t, operator);

        this.relation = rel;
        this.relationDir = relationDir;
    }

    public RelationInputSynapse getRelation() {
        return relation;
    }

    public Direction getRelationDir() {
        return relationDir;
    }

    public void expandRelations(TokenActivation origin) {
        getRelation().getInput()
                .evaluateLatentRelation(origin, getRelationDir())
                .forEach(relTokenAct ->
                        up(origin, relTokenAct)
                );
    }

    private void up(TokenActivation origin, TokenActivation relOrigin) {
        new RelationLinkingUpVisitor(this, origin, relOrigin)
                .visitUp(relOrigin, null);
    }
}
