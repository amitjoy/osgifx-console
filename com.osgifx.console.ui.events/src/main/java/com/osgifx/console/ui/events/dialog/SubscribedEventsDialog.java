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
package com.osgifx.console.ui.events.dialog;

import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;
import static javafx.scene.control.ButtonType.OK;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.di.extensions.OSGiBundle;
import org.eclipse.fx.core.di.LocalInstance;
import org.osgi.framework.BundleContext;

import com.osgifx.console.util.fx.Fx;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Dialog;
import javafx.scene.image.ImageView;
import javafx.stage.StageStyle;

public final class SubscribedEventsDialog extends Dialog<List<String>> {

    @Inject
    @LocalInstance
    private FXMLLoader    loader;
    @Inject
    @OSGiBundle
    private BundleContext context;

    public void init() {
        final var dialogPane = getDialogPane();
        initStyle(StageStyle.UNDECORATED);
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());

        dialogPane.setHeaderText("Subscribed Event Topics");
        dialogPane.setGraphic(new ImageView(this.getClass().getResource("/graphic/images/events.png").toString()));

        dialogPane.getButtonTypes().addAll(OK);

        final var dialogContent = Fx.loadFXML(loader, context, "/fxml/subscribed-events-dialog.fxml");
        dialogPane.setContent(dialogContent);
    }

}
