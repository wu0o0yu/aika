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

import network.aika.direction.Direction;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.neuron.activation.PatternActivation;

/**
 * @author Lukas Molzberger
 */
public class SelfRefVisitor extends Visitor {

    Activation oAct;

    boolean isSelfRef;

    public SelfRefVisitor(Activation oAct) {
        super(oAct.getThought());
    }

    protected SelfRefVisitor(SelfRefVisitor parent, Direction dir) {
        super(parent, dir);
    }

    public boolean isSelfRef() {
        return isSelfRef;
    }

    @Override
    public Visitor up(PatternActivation origin) {
        return new SelfRefVisitor(this, Direction.OUTPUT);
    }

    @Override
    public void check(Link lastLink, Activation act) {
        if(act == oAct)
            isSelfRef = true;
    }
}
