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

import network.aika.elements.activations.BindingActivation;
import network.aika.elements.synapses.SamePatternSynapse;
import network.aika.visitor.DownVisitor;
import network.aika.visitor.UpVisitor;

/**
 * @author Lukas Molzberger
 */
public class SamePatternLink extends BindingNeuronLink<SamePatternSynapse, BindingActivation> {

    public SamePatternLink(SamePatternSynapse s, BindingActivation input, BindingActivation output) {
        super(s, input, output);
    }

    @Override
    public void propagateRangeOrTokenPos() {
    }

    @Override
    public void bindingVisitDown(DownVisitor v) {
    }

    @Override
    public void bindingVisitUp(UpVisitor v) {
    }

    @Override
    public void patternVisitDown(DownVisitor v) {
        v.next(this);
    }

    @Override
    public void inhibVisitDown(DownVisitor v) {
    }

    @Override
    public void inhibVisitUp(UpVisitor v) {
    }
}
