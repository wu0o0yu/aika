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
package network.aika.visitor.inhibitory;

import network.aika.Thought;
import network.aika.elements.activations.Activation;
import network.aika.elements.links.Link;
import network.aika.elements.activations.PatternActivation;
import network.aika.Scope;
import network.aika.visitor.operator.Operator;
import network.aika.visitor.LinkingVisitor;

/**
 * @author Lukas Molzberger
 */
public class InhibitoryVisitor extends LinkingVisitor<PatternActivation> {

    private Scope identityRef;

    public InhibitoryVisitor(Thought t, Operator operator, Scope identityRef) {
        super(t, operator);

        this.identityRef = identityRef;
    }

    protected InhibitoryVisitor(InhibitoryVisitor parent, PatternActivation origin, Scope identityRef) {
        super(parent, origin);

        this.identityRef = identityRef;
    }

    public Scope getIdentityRef() {
        return identityRef;
    }

    @Override
    public void nextUp(PatternActivation origin, int depth) {
        new InhibitoryVisitor(this, origin, identityRef)
                .visit(origin, null, depth);
    }

    public void check(Link lastLink, Activation act) {
        operator.check(this, lastLink, act);
    }

    public boolean compatible(Scope from, Scope to) {
        if(origin == null)
            return false;

        return from == to;
    }

    public void createRelation(Link l) {
    }

    public void visit(Link l, int depth) {
        l.inhibVisit(this, depth);
    }

    public void visit(Activation act, Link l, int depth) {
        act.inhibVisit(this, l, depth);
    }
}
