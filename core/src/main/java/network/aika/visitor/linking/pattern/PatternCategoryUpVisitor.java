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
package network.aika.visitor.linking.pattern;

import network.aika.elements.activations.Activation;
import network.aika.elements.activations.BindingActivation;
import network.aika.elements.activations.PatternActivation;
import network.aika.elements.links.Link;
import network.aika.elements.synapses.Scope;
import network.aika.visitor.linking.LinkingUpVisitor;
import network.aika.visitor.linking.inhibitory.InhibitoryDownVisitor;

/**
 * @author Lukas Molzberger
 */
public class PatternCategoryUpVisitor extends LinkingUpVisitor<BindingActivation> {


    protected PatternCategoryUpVisitor(PatternCategoryDownVisitor parent, BindingActivation origin) {
        super(parent, origin);
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

    @Override
    protected void visitUp(Link l) {
        l.patternCatVisit(this);
    }

    @Override
    public void visitUp(Activation act, Link l) {
        act.patternCatVisitUp(this, l);
    }
}
