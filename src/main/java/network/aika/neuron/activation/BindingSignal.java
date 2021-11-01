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
package network.aika.neuron.activation;

/**
 * @author Lukas Molzberger
 */
public class BindingSignal {

    private BindingSignal parent; // debugging

    private Activation currentAct;
    private Activation bindingSignalAct;
    private byte scope;
    private byte depth;

    public BindingSignal(BindingSignal parent, Activation bsAct, Activation currentAct, byte scope, byte depth) {
        this.parent = parent;
        this.bindingSignalAct = bsAct;
        this.currentAct = currentAct;
        this.scope = scope;
        this.depth = depth;
    }

    public void link() {
        currentAct.bindingSignals.put(getBindingSignalAct(), this);
        getBindingSignalAct().registerBindingSignal(currentAct, this);
    }

    public Activation<?> getBindingSignalAct() {
        return bindingSignalAct;
    }

    public Activation getCurrentAct() {
        return currentAct;
    }

    public byte getScope() {
        return scope;
    }

    public byte getDepth() {
        return depth;
    }

    public String toString() {
        return "[id:" + bindingSignalAct.getId() + ",s:" + scope + ",d:" + depth + "]";
    }
}
