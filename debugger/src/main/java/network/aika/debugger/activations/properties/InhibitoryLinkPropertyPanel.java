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
package network.aika.debugger.activations.properties;

import network.aika.elements.links.AbstractInhibitoryLink;

/**
 * @author Lukas Molzberger
 */
public class InhibitoryLinkPropertyPanel extends LinkPropertyPanel<AbstractInhibitoryLink> {

    public InhibitoryLinkPropertyPanel(AbstractInhibitoryLink l) {
        super(l);
    }

    @Override
    public void initIdentitySection(AbstractInhibitoryLink l) {
        super.initIdentitySection(l);
    }

    @Override
    public void initInferenceSection(AbstractInhibitoryLink l) {
        addField(l.getInputValue());
        addField(l.getValue());
        addField(l.getNet());
    }

    @Override
    public void initTrainingSection(AbstractInhibitoryLink l) {
        super.initTrainingSection(l);
    }
}
