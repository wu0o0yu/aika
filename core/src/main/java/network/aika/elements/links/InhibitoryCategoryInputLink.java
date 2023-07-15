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

import network.aika.elements.activations.*;
import network.aika.elements.synapses.*;
import network.aika.visitor.Visitor;
import network.aika.visitor.linking.pattern.PatternCategoryVisitor;

import java.util.stream.Stream;

import static network.aika.elements.activations.InhibitoryActivation.crossConnectFields;


/**
 * @author Lukas Molzberger
 */
public class InhibitoryCategoryInputLink extends DisjunctiveLink<InhibitoryCategoryInputSynapse, CategoryActivation, InhibitoryActivation> implements CategoryInputLink {

    public InhibitoryCategoryInputLink(InhibitoryCategoryInputSynapse s, CategoryActivation input, InhibitoryActivation output) {
        super(s, input, output);

        input.getCategoryInputs()
                .forEach(act ->
                        crossConnectFields((InhibitoryActivation) act, output)
                );
    }

    @Override
    public CategorySynapse createCategorySynapse() {
        return new InhibitoryCategorySynapse();
    }

    @Override
    public void patternCatVisit(PatternCategoryVisitor v, int depth) {
    }

    @Override
    public void instantiateTemplate(CategoryActivation iAct, InhibitoryActivation oAct) {
        instantiateTemplate(iAct, oAct, this);
    }
}