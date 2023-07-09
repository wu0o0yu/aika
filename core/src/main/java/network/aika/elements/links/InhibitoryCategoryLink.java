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

import network.aika.elements.activations.CategoryActivation;
import network.aika.elements.activations.InhibitoryActivation;
import network.aika.elements.synapses.InhibitoryCategorySynapse;
import network.aika.visitor.Visitor;

import java.util.stream.Stream;

import static network.aika.elements.activations.InhibitoryActivation.crossConnectFields;

/**
 * @author Lukas Molzberger
 */
public class InhibitoryCategoryLink extends CategoryLink<InhibitoryCategorySynapse, InhibitoryActivation> {

    public InhibitoryCategoryLink(InhibitoryCategorySynapse s, InhibitoryActivation input, CategoryActivation output) {
        super(s, input, output);

        crossConnectFields(input, (InhibitoryActivation) output.getTemplate());
    }

    @Override
    public void patternCatVisit(Visitor v) {
    }
}
