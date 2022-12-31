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
package network.aika.elements.links;

import network.aika.elements.activations.Activation;
import network.aika.elements.activations.BindingActivation;
import network.aika.elements.neurons.Range;
import network.aika.elements.synapses.FeedbackSynapse;
import network.aika.visitor.DownVisitor;
import network.aika.visitor.UpVisitor;
import network.aika.visitor.Visitor;
import network.aika.visitor.selfref.SelfRefDownVisitor;


/**
 * @author Lukas Molzberger
 *
 */
public abstract class FeedbackLink<S extends FeedbackSynapse, IA extends Activation<?>> extends BindingNeuronLink<S, IA> {

    protected long visited;

    public FeedbackLink(S s, IA input, BindingActivation output) {
        super(s, input, output);
    }

    @Override
    public void propagateRangeOrTokenPos(Range r, Integer tokenPos) {
    }

    @Override
    public void selfRefVisitDown(SelfRefDownVisitor v) {
        if(checkVisited(v))
            return;

        super.selfRefVisitDown(v);
    }

    @Override
    public void bindingVisitDown(DownVisitor v) {
        if(checkVisited(v))
            return;

        super.bindingVisitDown(v);
    }

    @Override
    public void bindingVisitUp(UpVisitor v) {
        if(checkVisited(v))
            return;

        super.bindingVisitUp(v);
    }

    @Override
    public void patternVisitDown(DownVisitor v) {
        if(checkVisited(v))
            return;

        super.patternVisitDown(v);
    }

    @Override
    public void patternVisitUp(UpVisitor v) {
        if(checkVisited(v))
            return;

        super.patternVisitUp(v);
    }

    @Override
    public void inhibVisitDown(DownVisitor v) {
    }

    @Override
    public void inhibVisitUp(UpVisitor v) {
    }

    private boolean checkVisited(Visitor v) {
        if(visited == v.getV())
            return true;
        visited = v.getV();
        return false;
    }
}
