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
package network.aika.debugger.properties;

import network.aika.callbacks.FieldObserver;
import network.aika.fields.IQueueField;

import javax.swing.*;
import java.awt.*;


/**
 * @author Lukas Molzberger
 */
public class QueueFieldProperty extends FieldOutputProperty<IQueueField> implements FieldObserver {


    public QueueFieldProperty(Container parent, IQueueField field, boolean showReference, Boolean isConnected, Boolean isPropagateUpdates) {
        super(parent, field, showReference, isConnected, isPropagateUpdates);

        currentValueField.addPropertyChangeListener("value", e -> {
            if(withinUpdate)
                return;

            Number v = (Number) currentValueField.getValue();
            if(v == null)
                return;

            field.setValue(v.doubleValue());
        });

        currentValueField.setEnabled(true);
    }

    @Override
    public void registerListener() {
        field.addObserver(this);
    }

    @Override
    public void deregisterListener() {
        field.removeObserver(this);
    }

    @Override
    public void receiveUpdate(Double v) {
        SwingUtilities.invokeLater(() -> {
            withinUpdate = true;
            currentValueField.setValue(v);
            withinUpdate = false;
        });
    }
}
