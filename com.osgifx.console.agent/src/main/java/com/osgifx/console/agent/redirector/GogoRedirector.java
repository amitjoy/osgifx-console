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
package com.osgifx.console.agent.redirector;

import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.osgifx.console.agent.provider.AgentServer;

/**
 * Create a new Gogo Shell Command Session if there is a Gogo Command Processor
 * service present. If no Command Processor is present we will idle (a service
 * tracker is used that will handle multiple and switches).
 * <p>
 * There is a bit of a class space problem since the agent can be started on the
 * framework side of the class path. For this reason, we carry a copy of the
 * Gogo API classes and we will use proxies to use them. This leaves the Gogo
 * API unconstrained.
 */
public final class GogoRedirector implements Redirector {

    private AgentServer                                        agentServer;
    private ServiceTracker<CommandProcessor, CommandProcessor> tracker;
    private CommandProcessor                                   processor;
    private CommandSession                                     session;
    private final Shell                                        stdin;
    private RedirectOutput                                     stdout;

    /**
     * Create a redirector
     *
     * @param agentServer the server
     * @param context the context, needed to get the
     */
    public GogoRedirector(final AgentServer agentServer, final BundleContext context) {
        this.agentServer = agentServer;
        stdin            = new Shell();
        tracker          = new ServiceTracker<CommandProcessor, CommandProcessor>(context, CommandProcessor.class,
                                                                                  null) {
                             @Override
                             public CommandProcessor addingService(final ServiceReference<CommandProcessor> reference) {
                                 final CommandProcessor cp = proxy(CommandProcessor.class,
                                         super.addingService(reference));
                                 if (processor == null) {
                                     openSession(cp);
                                 }
                                 return cp;
                             }

                             @Override
                             public void removedService(final ServiceReference<CommandProcessor> reference,
                                                        final CommandProcessor service) {
                                 super.removedService(reference, service);
                                 if (service == processor) {
                                     closeSession();
                                     final CommandProcessor replacement = getService();
                                     if (replacement != null) {
                                         openSession(replacement);
                                     }
                                 }
                             }

                         };
        tracker.open();
    }

    private void closeSession() {
        if (session != null) {
            session.close();
            processor = null;
        }
    }

    private synchronized void openSession(final CommandProcessor replacement) {
        processor = replacement;
        final List<AgentServer> agents = Arrays.asList(agentServer);
        stdout  = new RedirectOutput(agents, null, false);
        session = processor.createSession(stdin, stdout, stdout);
        stdin.open(session);
    }

    /*
     * Create a proxy on a class. This is to prevent class cast exceptions. We get
     * our Gogo likely from another class loader since the agent can reside on the
     * framework side and we can't force Gogo to import our classes (nor should we).
     */
    @SuppressWarnings("unchecked")
    private <T> T proxy(final Class<T> clazz, final Object target) {
        final Class<?> targetClass = target.getClass();

        // We could also be in the same class space, in that case we can just return the value
        if (targetClass == clazz) {
            return clazz.cast(target);
        }
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[] { clazz }, (proxy, method, args) -> {
            final Method targetMethod = targetClass.getMethod(method.getName(), method.getParameterTypes());
            final Object result       = targetMethod.invoke(target, args);
            if (result != null && method.getReturnType().isInterface()
                    && targetMethod.getReturnType() != method.getReturnType()) {
                try {
                    return proxy(method.getReturnType(), result);
                } catch (final Exception e) {
                    // nothing to handle
                }
            }
            return result;
        });
    }

    @Override
    public void close() throws IOException {
        closeSession();
    }

    @Override
    public int getPort() {
        return -1;
    }

    @Override
    public void stdin(final String s) throws Exception {
        stdin.add(s);
    }

    @Override
    public PrintStream getOut() throws Exception {
        return stdout;
    }

}
