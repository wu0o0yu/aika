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
package network.aika.visitor.pattern;

import network.aika.Thought;
import network.aika.elements.activations.Activation;
import network.aika.elements.activations.BindingActivation;
import network.aika.elements.links.Link;
import network.aika.visitor.operator.Operator;
import network.aika.visitor.LinkingVisitor;

/**
 * @author Lukas Molzberger
 */
public class PatternVisitor extends LinkingVisitor<BindingActivation> {

    public PatternVisitor(Thought t, Operator operator) {
        super(t, operator);
    }

    protected PatternVisitor(PatternVisitor parent, BindingActivation origin) {
        super(parent, origin);
    }

    @Override
    public void upIntern(BindingActivation origin, int depth) {
        new PatternVisitor(this, origin)
                .visit(origin, null, depth);
    }

    public void visit(Link l, int depth) {
        l.patternVisit(this, depth);
    }

    public void visit(Activation act, Link l, int depth) {
        act.patternVisit(this, l, depth);
    }
}
