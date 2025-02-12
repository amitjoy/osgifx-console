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
package com.osgifx.console.ui.components;

import static com.osgifx.console.constants.FxConstants.ROOT_FXML;
import static com.osgifx.console.event.topics.TableFilterUpdateTopics.UPDATE_COMPONENT_FILTER_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_CONNECTED_EVENT_TOPIC;
import static com.osgifx.console.supervisor.Supervisor.AGENT_DISCONNECTED_EVENT_TOPIC;
import static javafx.geometry.Orientation.VERTICAL;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.fx.core.di.LocalInstance;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;
import org.osgi.framework.BundleContext;

import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.dto.SearchFilterDTO;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.ui.ConsoleMaskerPane;
import com.osgifx.console.ui.ConsoleStatusBar;
import com.osgifx.console.util.fx.Fx;
import com.osgifx.console.util.fx.FxDialog;

import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;

public final class ComponentsFxUI {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    @OSGiBundle
    private BundleContext     context;
    @Inject
    private Executor          executor;
    @Inject
    @Named("is_connected")
    private boolean           isConnected;
    @Inject
    @Named("is_snapshot_agent")
    private boolean           isSnapshotAgent;
    @Inject
    private ConsoleStatusBar  statusBar;
    @Inject
    private IEventBroker      eventBroker;
    @Inject
    private ConsoleMaskerPane progressPane;
    @Inject
    private DataProvider      dataProvider;
    private SearchFilterDTO   searchFilter;

    @PostConstruct
    public void postConstruct(final BorderPane parent, @LocalInstance final FXMLLoader loader) {
        createControls(parent, loader);
        logger.atDebug().log("Components part has been initialized");
    }

    @Focus
    public void onFocus() {
        if (isConnected) {
            refreshData();
        }
    }

    @Inject
    @Optional
    private void updateOnAgentConnectedEvent(@UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data,
                                             final BorderPane parent,
                                             @LocalInstance final FXMLLoader loader) {
        logger.atInfo().log("Agent connected event received");
        createControls(parent, loader);
    }

    @Inject
    @Optional
    private void updateOnAgentDisconnectedEvent(@UIEventTopic(AGENT_DISCONNECTED_EVENT_TOPIC) final String data,
                                                final BorderPane parent,
                                                @LocalInstance final FXMLLoader loader) {
        logger.atInfo().log("Agent disconnected event received");
        searchFilter = null;
        createControls(parent, loader);
    }

    @Inject
    @Optional
    private void onFilterUpdateEvent(@UIEventTopic(UPDATE_COMPONENT_FILTER_EVENT_TOPIC) final SearchFilterDTO filter,
                                     final BorderPane parent) {
        logger.atInfo().log("Search filter update event received");
        searchFilter = filter;
        if (filter.predicate != null) {
            initSearchFilterResetButton(filter.description);
        } else {
            initStatusBar(parent);
        }
    }

    private void createControls(final BorderPane parent, final FXMLLoader loader) {
        progressPane.setVisible(true);
        final Task<Void> task = new Task<>() {

            Node tabContent;

            @Override
            protected Void call() throws Exception {
                tabContent = Fx.loadFXML(loader, context, ROOT_FXML);
                final var controller = (ComponentsFxController) loader.getController();
                if (searchFilter != null) {
                    controller.onFilterUpdateEvent(searchFilter);
                }
                return null;
            }

            @Override
            protected void succeeded() {
                parent.getChildren().clear();
                parent.setCenter(tabContent);
                statusBar.addTo(parent);
                progressPane.setVisible(false);
            }
        };
        parent.getChildren().clear();
        progressPane.addTo(parent);
        initStatusBar(parent);
        executor.runAsync(task);
    }

    private void initStatusBar(final BorderPane parent) {
        statusBar.clearAllInRight();
        statusBar.addTo(parent);
        if (isConnected) {
            final var node = Fx.initStatusBarButton(this::refreshData, "Refresh", "REFRESH");
            if (!isSnapshotAgent) {
                statusBar.addToRight(node);
            }
        }
    }

    private void initSearchFilterResetButton(final String description) {
        statusBar.clearAllInRight();
        if (isConnected) {
            final var searchFilterResetNode = Fx.initStatusBarButton(() -> FxDialog
                    .showConfirmationDialog("Reset Search Filter?", description, getClass().getClassLoader(), btn -> {
                        if (btn == ButtonType.OK) {
                            eventBroker.post(UPDATE_COMPONENT_FILTER_EVENT_TOPIC, new SearchFilterDTO());
                        }
                    }), "Reset Search Filter", "CLOSE", Color.RED);
            statusBar.addToRight(searchFilterResetNode);
            statusBar.addToRight(new Separator(VERTICAL));
            final var refreshNode = Fx.initStatusBarButton(this::refreshData, "Refresh", "REFRESH");
            statusBar.addToRight(refreshNode);
        }
    }

    private void refreshData() {
        dataProvider.retrieveInfo("components", true);
    }

}
