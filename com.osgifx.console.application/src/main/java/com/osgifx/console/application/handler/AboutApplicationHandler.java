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

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.application.dialog.AboutApplicationDialog;

public final class AboutApplicationHandler {

    @Log
    @Inject
    private FluentLogger    logger;
    @Inject
    private IEclipseContext context;

    @Execute
    public void execute() {
        final var dialog = new AboutApplicationDialog();
        ContextInjectionFactory.inject(dialog, context);
        logger.atInfo().log("Injected about dialog to eclipse context");

        dialog.init();
        dialog.show();
    }

}
