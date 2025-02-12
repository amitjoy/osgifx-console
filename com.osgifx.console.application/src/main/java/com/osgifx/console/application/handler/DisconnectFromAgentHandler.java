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
package com.osgifx.console.application.handler;

import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;

import java.util.stream.Stream;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.fx.core.di.ContextBoundValue;
import org.eclipse.fx.core.di.ContextValue;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.application.dialog.SocketConnectionSettingDTO;
import com.osgifx.console.supervisor.Supervisor;
import com.osgifx.console.supervisor.factory.SupervisorFactory;
import com.osgifx.console.supervisor.factory.SupervisorFactory.SupervisorType;

public final class DisconnectFromAgentHandler {

    @Log
    @Inject
    private FluentLogger                                  logger;
    @Inject
    @Optional
    private Supervisor                                    supervisor;
    @Inject
    private IEventBroker                                  eventBroker;
    @Inject
    @Optional
    @ContextValue("is_connected")
    private ContextBoundValue<Boolean>                    isConnected;
    @Inject
    @Optional
    @ContextValue("is_local_agent")
    private ContextBoundValue<Boolean>                    isLocalAgent;
    @Inject
    @Optional
    @ContextValue("connected.agent")
    private ContextBoundValue<String>                     connectedAgent;
    @Inject
    @Optional
    @ContextValue("selected.settings")
    private ContextBoundValue<SocketConnectionSettingDTO> selectedSettings;
    @Inject
    private SupervisorFactory                             supervisorFactory;

    @Execute
    public void execute() {
        try {
            supervisor.disconnect();
            Stream.of(SupervisorType.values()).forEach(type -> supervisorFactory.removeSupervisor(type));

            eventBroker.post(AGENT_DISCONNECTED_EVENT_TOPIC, "");

            isConnected.publish(false);
            isLocalAgent.publish(false);
            selectedSettings.publish(null);
            connectedAgent.publish(null);

            logger.atInfo().log("Agent has been successfully disconnected");
        } catch (final Exception e) {
            logger.atError().withException(e).log("Agent cannot be disconnected");
        }
    }

    @CanExecute
    public boolean canExecute() {
        return isConnected.getValue();
    }

}
