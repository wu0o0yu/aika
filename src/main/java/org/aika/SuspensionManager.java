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
package org.aika;


import java.util.*;

public interface SuspensionManager {


    void register(Provider n);


    void unregister(Provider n);


    class LastUsedSuspensionManager implements SuspensionManager {

        private Map<Integer, Provider<? extends AbstractNode>> activeProviders = new TreeMap<>();

        @Override
        public synchronized void register(Provider p) {
            activeProviders.put(p.id, p);
        }

        @Override
        public synchronized void unregister(Provider p) {
            activeProviders.remove(p.id);
        }


        /**
         * Suspend all neurons and logic nodes whose last used document id is lower/older than {@param docId}.
         *
         * @param docId
         */
        public void suspendUnusedNodes(int docId) {
            List<Provider> tmp;
            synchronized (this) {
                tmp = new ArrayList<>(activeProviders.values());
            }
            for (Provider p: tmp) {
                suspend(docId, p);
            }
        }


        public void suspendAll() {
            suspendUnusedNodes(Integer.MAX_VALUE);
        }


        private boolean suspend(int docId, Provider<? extends AbstractNode> p) {
            if (!p.isSuspended() && p.get().lastUsedDocumentId <= docId) {
                p.suspend();
                return true;
            }
            return false;
        }
    }

/*
    class FixedSizeSuspensionManager implements SuspensionManager {

        @Override
        public void register(Provider n) {

        }
    }
*/
}
