/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.ui.terminal;

import java.util.concurrent.locks.ReentrantLock;

import org.osgi.service.component.annotations.Component;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;

@Component(service = TerminalHistory.class)
public final class TerminalHistory {

    private final ReentrantLock         lock    = new ReentrantLock();
    private final EvictingQueue<String> history = EvictingQueue.create(20);

    public void add(final String command) {
        lock.lock();
        try {
            history.add(command);
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        lock.lock();
        try {
            history.clear();
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return history.size();
        } finally {
            lock.unlock();
        }
    }

    public String get(final int index) {
        lock.lock();
        try {
            if (history.isEmpty()) {
                return "";
            }
            return Lists.newArrayList(history).get(index);
        } finally {
            lock.unlock();
        }
    }

}
