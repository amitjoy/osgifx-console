/*******************************************************************************
 * Copyright 2021-2025 Amit Kumar Mondal
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
package com.osgifx.console.ui.heap;

import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.ui.ConsoleStatusBar;

import javafx.scene.layout.BorderPane;

public final class HeapMonitorFxUI {

    @Log
    @Inject
    private FluentLogger     logger;
    @Inject
    private ConsoleStatusBar statusBar;
    @Inject
    private HeapMonitorPane  memoryViewPane;

    @PostConstruct
    public void postConstruct(final BorderPane parent) {
        createControls(parent);
        logger.atDebug().log("Heap monitor part has been initialized");
    }

    private void createControls(final BorderPane parent) {
        parent.setCenter(memoryViewPane);
        statusBar.addTo(parent);
    }

    @Inject
    @Optional
    private void updateOnAgentConnectedEvent(@UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data,
                                             final BorderPane parent) {
        logger.atInfo().log("Agent connected event received");
        createControls(parent);
        memoryViewPane.init();
    }

    @Inject
    @Optional
    private void updateOnAgentDisconnectedEvent(@UIEventTopic(AGENT_DISCONNECTED_EVENT_TOPIC) final String data,
                                                final BorderPane parent) {
        logger.atInfo().log("Agent disconnected event received");
        memoryViewPane.stopUpdates();
        createControls(parent);
        memoryViewPane.init();
    }

}
