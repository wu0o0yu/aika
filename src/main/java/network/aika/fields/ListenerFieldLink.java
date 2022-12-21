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

    public static ListenerFieldLink createEventListener(FieldOutput in, FieldOnTrueEvent eventListener) {
        return createUpdateListener(in, (arg, u) -> {
            if (u > 0.0)
                eventListener.onTrue();
        });
    }

    public static ListenerFieldLink createUpdateListener(FieldOutput in, UpdateListener updateListener) {
        return new ListenerFieldLink(in, 0, updateListener);
    }

    public ListenerFieldLink(FieldOutput input, int arg, UpdateListener output) {
        super(input, arg, output);
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
}
