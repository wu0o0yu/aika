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
package network.aika.visitor;

import network.aika.Thought;
import network.aika.elements.activations.Activation;
import network.aika.elements.links.Link;
import network.aika.elements.activations.TokenActivation;

/**
 * @author Lukas Molzberger
 */
public abstract class DownVisitor<T extends Activation> extends Visitor {

    public DownVisitor(Thought t) {
        super(t);
    }

    public void check(Link lastLink, Activation act) {
        // Nothing to do
    }

    public void start(Activation<?> act) {
        visitDown(act, null);
    }

    public void next(Activation<?> act) {
        act.getInputLinks()
                .forEach(l -> visitDown(l));
    }

    public void next(Link<?, ?, ?> l) {
        if(l.getInput() != null)
            visitDown(l.getInput(), l);
    }

    protected abstract void visitDown(Link l);

    protected abstract void visitDown(Activation act, Link l);

    public abstract void up(T origin);

    public void expandRelations(TokenActivation tAct) {
    }

    public boolean isDown() {
        return true;
    }

    public boolean isUp() {
        return false;
    }
}
