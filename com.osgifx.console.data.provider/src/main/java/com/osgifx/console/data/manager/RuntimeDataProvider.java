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
package com.osgifx.console.data.manager;

import static com.osgifx.console.data.manager.RuntimeInfoSupplier.PROPERTY_ID;
import static com.osgifx.console.data.supplier.BundlesInfoSupplier.BUNDLES_ID;
import static com.osgifx.console.data.supplier.ComponentsInfoSupplier.COMPONENTS_ID;
import static com.osgifx.console.data.supplier.ConfigurationsInfoSupplier.CONFIGURATIONS_ID;
import static com.osgifx.console.data.supplier.EventsInfoSupplier.EVENTS_ID;
import static com.osgifx.console.data.supplier.HealthChecksInfoSupplier.HEALTHCHECKS_ID;
import static com.osgifx.console.data.supplier.HttpComponentsInfoSupplier.HTTP_ID;
import static com.osgifx.console.data.supplier.LeaksInfoSupplier.LEAKS_ID;
import static com.osgifx.console.data.supplier.LoggerContextsInfoSupplier.LOGGER_CONTEXTS_ID;
import static com.osgifx.console.data.supplier.LogsInfoSupplier.LOGS_ID;
import static com.osgifx.console.data.supplier.PackagesInfoSupplier.PACKAGES_ID;
import static com.osgifx.console.data.supplier.PropertiesInfoSupplier.PROPERTIES_ID;
import static com.osgifx.console.data.supplier.RolesInfoSupplier.ROLES_ID;
import static com.osgifx.console.data.supplier.ServicesInfoSupplier.SERVICES_ID;
import static com.osgifx.console.data.supplier.ThreadsInfoSupplier.THREADS_ID;
import static com.osgifx.console.event.topics.DataRetrievedEventTopics.DATA_RETRIEVED_ALL_TOPIC;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.LoggerFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventAdmin;

import com.google.mu.util.stream.BiCollectors;
import com.google.mu.util.stream.BiStream;
import com.osgifx.console.agent.dto.RuntimeDTO;
import com.osgifx.console.agent.dto.XBundleDTO;
import com.osgifx.console.agent.dto.XBundleLoggerContextDTO;
import com.osgifx.console.agent.dto.XComponentDTO;
import com.osgifx.console.agent.dto.XConfigurationDTO;
import com.osgifx.console.agent.dto.XDmtNodeDTO;
import com.osgifx.console.agent.dto.XEventDTO;
import com.osgifx.console.agent.dto.XHealthCheckDTO;
import com.osgifx.console.agent.dto.XHeapUsageDTO;
import com.osgifx.console.agent.dto.XHttpComponentDTO;
import com.osgifx.console.agent.dto.XLogEntryDTO;
import com.osgifx.console.agent.dto.XMemoryInfoDTO;
import com.osgifx.console.agent.dto.XPropertyDTO;
import com.osgifx.console.agent.dto.XRoleDTO;
import com.osgifx.console.agent.dto.XServiceDTO;
import com.osgifx.console.agent.dto.XThreadDTO;
import com.osgifx.console.data.provider.DataProvider;
import com.osgifx.console.data.provider.PackageDTO;
import com.osgifx.console.executor.Executor;
import com.osgifx.console.supervisor.Supervisor;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Component
@SuppressWarnings("unchecked")
public final class RuntimeDataProvider implements DataProvider {

    @Reference
    private LoggerFactory                          factory;
    @Reference
    private Executor                               executor;
    @Reference
    private EventAdmin                             eventAdmin;
    @Reference(cardinality = OPTIONAL, policyOption = GREEDY)
    private volatile Supervisor                    supervisor;
    private FluentLogger                           logger;
    private final Map<String, RuntimeInfoSupplier> infoSuppliers = new ConcurrentHashMap<>();

    @Activate
    public void activate() {
        logger = FluentLogger.of(factory.createLogger(getClass().getName()));
    }

    @Reference(cardinality = MULTIPLE, policy = DYNAMIC)
    public void bindRuntimeInfoSupplier(final RuntimeInfoSupplier supplier,
                                        final ServiceReference<RuntimeInfoSupplier> reference) {
        infoSuppliers.put(reference.getProperty(PROPERTY_ID).toString(), supplier);
    }

    public void unbindRuntimeInfoSupplier(final RuntimeInfoSupplier supplier,
                                          final ServiceReference<RuntimeInfoSupplier> reference) {
        infoSuppliers.remove(reference.getProperty(PROPERTY_ID).toString());
    }

    @Override
    public void retrieveInfo(final String id, final boolean isAsync) {
        // @formatter:off
        if (id == null) {
            if (isAsync) {
                final var futures =
                        BiStream.from(infoSuppliers)
                                .mapValues((k, v) -> executor.runAsync(v::retrieve))
                                .collect(BiCollectors.toMap());

                CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
                                 .thenRunAsync(() -> RuntimeInfoSupplier.sendEvent(eventAdmin, DATA_RETRIEVED_ALL_TOPIC))
                                 .thenRunAsync(() -> logger.atInfo().log("All runtime informations have been retrieved successfully (async)"));
            } else {
                infoSuppliers.forEach((k, v) -> v.retrieve());
                RuntimeInfoSupplier.sendEvent(eventAdmin, DATA_RETRIEVED_ALL_TOPIC);
                logger.atInfo().log("All runtime informations have been retrieved successfully (sync)");
            }
        } else if (isAsync) {
            executor.runAsync(() -> retrieve(id))
                    .thenRunAsync(() -> logger.atInfo().log("Runtime information of '%s' has been retrieved successfully (async)", id));
        } else {
            retrieve(id);
            logger.atInfo().log("Runtime information of '%s' has been retrieved successfully (sync)", id);
        }
        // @formatter:on
    }

    @Override
    public ObservableList<XBundleDTO> bundles() {
        return (ObservableList<XBundleDTO>) supply(BUNDLES_ID);
    }

    @Override
    public ObservableList<PackageDTO> packages() {
        return (ObservableList<PackageDTO>) supply(PACKAGES_ID);
    }

    @Override
    public ObservableList<XServiceDTO> services() {
        return (ObservableList<XServiceDTO>) supply(SERVICES_ID);
    }

    @Override
    public ObservableList<XComponentDTO> components() {
        return (ObservableList<XComponentDTO>) supply(COMPONENTS_ID);
    }

    @Override
    public ObservableList<XConfigurationDTO> configurations() {
        return (ObservableList<XConfigurationDTO>) supply(CONFIGURATIONS_ID);
    }

    @Override
    public ObservableList<XEventDTO> events() {
        return (ObservableList<XEventDTO>) supply(EVENTS_ID);
    }

    @Override
    public ObservableList<XLogEntryDTO> logs() {
        return (ObservableList<XLogEntryDTO>) supply(LOGS_ID);
    }

    @Override
    public ObservableList<XPropertyDTO> properties() {
        return (ObservableList<XPropertyDTO>) supply(PROPERTIES_ID);
    }

    @Override
    public ObservableList<XThreadDTO> threads() {
        return (ObservableList<XThreadDTO>) supply(THREADS_ID);
    }

    @Override
    public ObservableList<XBundleDTO> leaks() {
        return (ObservableList<XBundleDTO>) supply(LEAKS_ID);
    }

    @Override
    public ObservableList<XHttpComponentDTO> httpComponents() {
        return (ObservableList<XHttpComponentDTO>) supply(HTTP_ID);
    }

    @Override
    public ObservableList<XHealthCheckDTO> healthchecks() {
        return (ObservableList<XHealthCheckDTO>) supply(HEALTHCHECKS_ID);
    }

    @Override
    public ObservableList<XRoleDTO> roles() {
        return (ObservableList<XRoleDTO>) supply(ROLES_ID);
    }

    @Override
    public ObservableList<XBundleLoggerContextDTO> loggerContexts() {
        return (ObservableList<XBundleLoggerContextDTO>) supply(LOGGER_CONTEXTS_ID);
    }

    @Override
    public CompletableFuture<XMemoryInfoDTO> memory() {
        final var agent = supervisor.getAgent();
        if (agent == null) {
            logger.atWarning().log("Agent not connected");
            return null;
        }
        return executor.supplyAsync(agent::getMemoryInfo);
    }

    @Override
    public CompletableFuture<XDmtNodeDTO> readDmtNode(final String rootURI) {
        final var agent = supervisor.getAgent();
        if (agent == null) {
            logger.atWarning().log("Agent not connected");
            return null;
        }
        return executor.supplyAsync(() -> agent.readDmtNode(rootURI));
    }

    @Override
    public CompletableFuture<RuntimeDTO> readRuntimeDTO() {
        final var agent = supervisor.getAgent();
        if (agent == null) {
            logger.atWarning().log("Agent not connected");
            return null;
        }
        return executor.supplyAsync(agent::getRuntimeDTO);
    }

    @Override
    public CompletableFuture<XHeapUsageDTO> heapUsage() {
        final var agent = supervisor.getAgent();
        if (agent == null) {
            logger.atWarning().log("Agent not connected");
            return null;
        }
        return executor.supplyAsync(agent::getHeapUsage);
    }

    private ObservableList<?> supply(final String id) {
        for (final Entry<String, RuntimeInfoSupplier> entry : infoSuppliers.entrySet()) {
            final var supplierId  = entry.getKey();
            final var supplierRef = entry.getValue();

            if (id.equals(supplierId)) {
                return supplierRef.supply();
            }
        }
        return FXCollections.observableArrayList();
    }

    private void retrieve(final String id) {
        for (final Entry<String, RuntimeInfoSupplier> entry : infoSuppliers.entrySet()) {
            final var supplierId  = entry.getKey();
            final var supplierRef = entry.getValue();

            if (id.equals(supplierId)) {
                supplierRef.retrieve();
            }
        }
    }

}
