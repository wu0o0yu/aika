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
package network.aika.fields;

import network.aika.callbacks.UpdateListener;

/**
 * @author Lukas Molzberger
 */
public class ListenerFieldLink extends AbstractFieldLink<UpdateListener> {

    private String listenerName;

    public ListenerFieldLink(FieldOutput input, String listenerName, UpdateListener output) {
        super(input, 0, output);
        this.listenerName = listenerName;
    }

    public void setInput(FieldOutput input) {
        this.input = input;
    }

    @Override
    public void unlinkOutput() {
    }

    public String getListenerName() {
        return listenerName;
    }

    @Override
    public String toString() {
        return input + " --> listener: " + listenerName;
    }
}
