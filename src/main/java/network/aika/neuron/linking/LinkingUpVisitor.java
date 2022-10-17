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

import network.aika.Thought;
import network.aika.neuron.activation.*;
import network.aika.neuron.conjunctive.Scope;

import static network.aika.neuron.conjunctive.Scope.INPUT;

/**
 * @author Lukas Molzberger
 */
public abstract class LinkingUpVisitor<T extends Activation> extends UpVisitor {

    LinkingOperator operator;

    T origin;

    public LinkingUpVisitor(Thought t, LinkingOperator operator) {
        super(t);

        this.operator = operator;
    }

    protected LinkingUpVisitor(LinkingDownVisitor parent, T origin) {
        super(parent);
        this.origin = origin;
        this.operator = parent.operator;
    }

    public void check(Link lastLink, Activation act) {
        operator.check(this, lastLink, act);
    }

    public abstract boolean compatible(Scope from, Scope to);

    public void createRelation(Link l) {
    }
}
