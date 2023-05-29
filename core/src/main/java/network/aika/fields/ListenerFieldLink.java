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

    public static ListenerFieldLink createEventListener(FieldOutput in, String listenerName, FieldOnTrueEvent eventListener) {
        return createUpdateListener(in, listenerName, (arg, u) -> {
            if (u > 0.0)
                eventListener.onTrue();
        });
    }

    public static ListenerFieldLink createUpdateListener(FieldOutput in, String listenerName, FieldOnTrueEvent eventListener) {
        return createUpdateListener(in, listenerName, (arg, u) ->
            eventListener.onTrue()
        );
    }

    public static ListenerFieldLink createUpdateListener(FieldOutput in, String listenerName, UpdateListener updateListener) {
        return new ListenerFieldLink(in, listenerName, updateListener);
    }

    public ListenerFieldLink(FieldOutput input, String listenerName, UpdateListener output) {
        super(input, 0, output);
        this.listenerName = listenerName;
    }

    @Override
    public boolean crossesBorder() {
        return false;
    }

    public void setInput(FieldOutput input) {
        this.input = input;
    }

    @Override
    public void unlink() {

    }

    public String getListenerName() {
        return listenerName;
    }

    @Override
    public String toString() {
        return input + " --> listener: " + listenerName;
    }
}
