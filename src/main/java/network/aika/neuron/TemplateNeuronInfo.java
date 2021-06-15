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
package network.aika.neuron;

import network.aika.neuron.activation.scopes.Scope;

import java.util.Collections;
import java.util.Set;

/**
 *
 * @author Lukas Molzberger
 */
public class TemplateNeuronInfo {

    Set<Scope> inputScopes = Collections.emptySet();
    Set<Scope> outputScopes = Collections.emptySet();

    private String label;
    private Set<Neuron<?>> templateGroup;

    private double xCoord;
    private double yCoord;


    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getXCoord() {
        return xCoord;
    }

    public void setXCoord(double xCoord) {
        this.xCoord = xCoord;
    }

    public double getYCoord() {
        return yCoord;
    }

    public void setYCoord(double yCoord) {
        this.yCoord = yCoord;
    }

    public Set<Neuron<?>> getTemplateGroup() {
        return templateGroup;
    }

    public void setTemplateGroup(Set<Neuron<?>> templateGroup) {
        this.templateGroup = templateGroup;
    }

    public Set<Scope> getInputScopes() {
        return inputScopes;
    }

    public Set<Scope> getOutputScopes() {
        return outputScopes;
    }
}
