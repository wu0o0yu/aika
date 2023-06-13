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

import java.util.Collection;

import static network.aika.fields.ListenerFieldLink.createEventListener;
import static network.aika.fields.ListenerFieldLink.createUpdateListener;

/**
 * @author Lukas Molzberger
 */
public interface FieldOutput {

    String getLabel();

    String getValueString();

    double getValue();

    double getUpdatedValue();

    void addOutput(AbstractFieldLink fl);

    void removeOutput(AbstractFieldLink fl);

    Collection<AbstractFieldLink> getReceivers();

    FieldObject getReference();

    void disconnectOutputs(boolean deinitialize);

    default void addEventListener(String listenerName, FieldOnTrueEvent eventListener) {
        addEventListener(listenerName, eventListener, false);
    }

    default void addEventListener(String listenerName, FieldOnTrueEvent eventListener, boolean assumeInitialized) {
        AbstractFieldLink fl = createEventListener(this, listenerName, eventListener);
        addOutput(fl);
        fl.connect(!assumeInitialized);
    }

    default void addUpdateListener(String listenerName, FieldOnTrueEvent eventListener) {
        addUpdateListener(listenerName, eventListener, true);
    }

    default void addUpdateListener(String listenerName, FieldOnTrueEvent eventListener, boolean assumeInitialized) {
        AbstractFieldLink fl = createUpdateListener(this, listenerName, eventListener);
        addOutput(fl);
        fl.connect(!assumeInitialized);
    }
}
