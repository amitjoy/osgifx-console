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
package com.osgifx.console.application.dialog;

import static com.google.common.base.Verify.verify;
import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;
import static javafx.scene.control.ButtonType.CANCEL;
import static org.controlsfx.validation.Validator.createEmptyValidator;
import static org.controlsfx.validation.Validator.createPredicateValidator;

import java.util.Optional;

import javax.inject.Inject;

import org.controlsfx.control.textfield.CustomPasswordField;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.dialog.LoginDialog;
import org.controlsfx.validation.ValidationSupport;
import org.eclipse.fx.core.ThreadSynchronize;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.google.common.primitives.Ints;
import com.osgifx.console.util.fx.FxDialog;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

public final class MqttConnectionDialog extends Dialog<MqttConnectionSettingDTO> {

    @Log
    @Inject
    private FluentLogger      logger;
    @Inject
    private ThreadSynchronize threadSync;

    public void init(final MqttConnectionSettingDTO setting) {
        final var dialogPane = getDialogPane();
        initStyle(StageStyle.UNDECORATED);

        if (setting == null) {
            dialogPane.setHeaderText("Add MQTT Connection Settings");
        } else {
            dialogPane.setHeaderText("Edit MQTT Connection Settings");
        }
        dialogPane.getStylesheets().add(LoginDialog.class.getResource("dialogs.css").toExternalForm());
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());
        dialogPane.setGraphic(
                new ImageView(this.getClass().getResource("/graphic/images/connection-setting.png").toString()));
        dialogPane.getButtonTypes().addAll(CANCEL);

        final var name = (CustomTextField) TextFields.createClearableTextField();
        name.setLeft(new ImageView(getClass().getResource("/graphic/icons/name.png").toExternalForm()));
        Optional.ofNullable(setting).ifPresent(s -> name.setText(s.name));

        final var clientId = (CustomTextField) TextFields.createClearableTextField();
        clientId.setLeft(new ImageView(getClass().getResource("/graphic/icons/name.png").toExternalForm()));
        Optional.ofNullable(setting).ifPresent(s -> clientId.setText(s.clientId));

        final var server = (CustomTextField) TextFields.createClearableTextField();
        server.setLeft(new ImageView(getClass().getResource("/graphic/icons/hostname.png").toExternalForm()));
        Optional.ofNullable(setting).ifPresent(s -> server.setText(s.server));

        final var port = (CustomTextField) TextFields.createClearableTextField();
        port.setLeft(new ImageView(getClass().getResource("/graphic/icons/port.png").toExternalForm()));
        Optional.ofNullable(setting).ifPresent(s -> port.setText(String.valueOf(s.port)));

        final var timeout = (CustomTextField) TextFields.createClearableTextField();
        timeout.setLeft(new ImageView(getClass().getResource("/graphic/icons/timeout.png").toExternalForm()));
        Optional.ofNullable(setting).ifPresent(s -> timeout.setText(String.valueOf(s.timeout)));

        final var username = (CustomTextField) TextFields.createClearableTextField();
        username.setLeft(new ImageView(getClass().getResource("/graphic/icons/username.png").toExternalForm()));
        Optional.ofNullable(setting).ifPresent(s -> username.setText(s.username));

        final var password = (CustomPasswordField) TextFields.createClearablePasswordField();
        password.setLeft(new ImageView(getClass().getResource("/graphic/icons/username.png").toExternalForm()));
        Optional.ofNullable(setting).ifPresent(s -> password.setText(s.password));

        final var tokenConfig = (CustomTextField) TextFields.createClearableTextField();
        tokenConfig.setLeft(new ImageView(getClass().getResource("/graphic/icons/username.png").toExternalForm()));
        Optional.ofNullable(setting).ifPresent(s -> tokenConfig.setText(s.tokenConfig));

        final var pubTopic = (CustomTextField) TextFields.createClearableTextField();
        pubTopic.setLeft(new ImageView(getClass().getResource("/graphic/icons/topic.png").toExternalForm()));
        Optional.ofNullable(setting).ifPresent(s -> pubTopic.setText(s.pubTopic));

        final var subTopic = (CustomTextField) TextFields.createClearableTextField();
        subTopic.setLeft(new ImageView(getClass().getResource("/graphic/icons/topic.png").toExternalForm()));
        Optional.ofNullable(setting).ifPresent(s -> subTopic.setText(s.subTopic));

        final var lwtTopic = (CustomTextField) TextFields.createClearableTextField();
        lwtTopic.setLeft(new ImageView(getClass().getResource("/graphic/icons/topic.png").toExternalForm()));
        Optional.ofNullable(setting).ifPresent(s -> lwtTopic.setText(s.lwtTopic));

        final var lbMessage = new Label("");
        lbMessage.getStyleClass().addAll("message-banner");
        lbMessage.setVisible(false);
        lbMessage.setManaged(false);

        final var content = new VBox(10);

        content.getChildren().add(lbMessage);
        content.getChildren().add(name);
        content.getChildren().add(clientId);
        content.getChildren().add(server);
        content.getChildren().add(port);
        content.getChildren().add(timeout);
        content.getChildren().add(username);
        content.getChildren().add(password);
        content.getChildren().add(tokenConfig);
        content.getChildren().add(pubTopic);
        content.getChildren().add(subTopic);
        content.getChildren().add(lwtTopic);

        dialogPane.setContent(content);

        final var saveButtonType = new ButtonType("Save", ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(saveButtonType);

        final var loginButton = (Button) dialogPane.lookupButton(saveButtonType);
        loginButton.setOnAction(actionEvent -> {
            try {
                lbMessage.setVisible(false);
                lbMessage.setManaged(false);
                hide();
            } catch (final Exception ex) {
                lbMessage.setVisible(true);
                lbMessage.setManaged(true);
                lbMessage.setText(ex.getMessage());
                FxDialog.showExceptionDialog(ex, getClass().getClassLoader());
            }
        });
        final var nameCaption        = "Connection Name";
        final var clientIdCaption    = "Client ID";
        final var serverCaption      = "Server";
        final var portCaption        = "Port (1883 or 8883)";
        final var timeoutCaption     = "Timeout in millis";
        final var usernameCaption    = "Username";
        final var passwordCaption    = "Password";
        final var tokenConfigCaption = "Token Configuration (JSON)";
        final var pubTopicCaption    = "Agent Publish Topic";
        final var subTopicCaption    = "Agent Subscription Topic";
        final var lwtTopicCaption    = "Agent Runtime LWT Topic";

        name.setPromptText(nameCaption);
        clientId.setPromptText(clientIdCaption);
        server.setPromptText(serverCaption);
        port.setPromptText(portCaption);
        timeout.setPromptText(timeoutCaption);
        username.setPromptText(usernameCaption);
        password.setPromptText(passwordCaption);
        tokenConfig.setPromptText(tokenConfigCaption);
        pubTopic.setPromptText(pubTopicCaption);
        subTopic.setPromptText(subTopicCaption);
        lwtTopic.setPromptText(lwtTopicCaption);

        final var validationSupport = new ValidationSupport();
        threadSync.asyncExec(() -> {
            final var requiredFormat       = "'%s' is required";
            final var requiredPortFormat   = "'%s' should be a valid port number";
            final var requiredNumberFormat = "'%s' should be a valid integer number";

            validationSupport.registerValidator(name, createEmptyValidator(String.format(requiredFormat, nameCaption)));
            validationSupport.registerValidator(clientId,
                    createEmptyValidator(String.format(requiredFormat, clientIdCaption)));
            validationSupport.registerValidator(server,
                    createEmptyValidator(String.format(requiredFormat, serverCaption)));
            validationSupport.registerValidator(port, createEmptyValidator(String.format(requiredFormat, portCaption)));
            validationSupport.registerValidator(port, createPredicateValidator(value -> {
                final var parsedPort = Ints.tryParse(value.toString());
                return parsedPort != null && (parsedPort == 1883 || parsedPort == 8883);
            }, String.format(requiredPortFormat, portCaption)));
            validationSupport.registerValidator(timeout,
                    createEmptyValidator(String.format(requiredFormat, timeoutCaption)));
            validationSupport.registerValidator(timeout,
                    createPredicateValidator(value -> Ints.tryParse(value.toString()) != null,
                            String.format(requiredNumberFormat, timeoutCaption)));
            validationSupport.registerValidator(pubTopic,
                    createEmptyValidator(String.format(requiredFormat, pubTopicCaption)));
            validationSupport.registerValidator(subTopic,
                    createEmptyValidator(String.format(requiredFormat, subTopicCaption)));
        });
        final var saveBtn = (Button) dialogPane.lookupButton(saveButtonType);
        saveBtn.disableProperty().bind(validationSupport.invalidProperty());

        setResultConverter(dialogButton -> {
            if (dialogButton == CANCEL) {
                return null;
            }
            final var p = Ints.tryParse(port.getText());
            final var t = Ints.tryParse(timeout.getText());

            verify(p != null && t != null, "Port and timeout formats are not compliant");
            if (setting != null) {
                setting.name        = name.getText();
                setting.clientId    = clientId.getText();
                setting.server      = server.getText();
                setting.port        = p;
                setting.timeout     = t;
                setting.username    = username.getText();
                setting.password    = password.getText();
                setting.tokenConfig = tokenConfig.getText();
                setting.pubTopic    = pubTopic.getText();
                setting.subTopic    = subTopic.getText();
                setting.lwtTopic    = lwtTopic.getText();

                return setting;
            }
            return new MqttConnectionSettingDTO(name.getText(), clientId.getText(), server.getText(), p, t,
                                                username.getText(), password.getText(), tokenConfig.getText(),
                                                pubTopic.getText(), subTopic.getText(), lwtTopic.getText());
        });
    }

}
