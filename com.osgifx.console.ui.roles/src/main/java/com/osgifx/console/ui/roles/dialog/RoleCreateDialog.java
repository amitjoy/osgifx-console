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
package com.osgifx.console.ui.roles.dialog;

import static com.google.common.base.Verify.verify;
import static com.osgifx.console.constants.FxConstants.STANDARD_CSS;

import javax.inject.Inject;

import org.apache.commons.lang3.EnumUtils;
import org.controlsfx.control.textfield.CustomTextField;
import org.controlsfx.control.textfield.TextFields;
import org.controlsfx.dialog.LoginDialog;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.eclipse.fx.core.log.FluentLogger;
import org.eclipse.fx.core.log.Log;

import com.osgifx.console.agent.dto.XRoleDTO;
import com.osgifx.console.ui.roles.dialog.RoleCreateDialog.RoleDTO;
import com.osgifx.console.util.fx.FxDialog;

import javafx.collections.FXCollections;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.StageStyle;

public final class RoleCreateDialog extends Dialog<RoleDTO> {

    public record RoleDTO(String name, XRoleDTO.Type type) {
    }

    @Log
    @Inject
    private FluentLogger            logger;
    private final ValidationSupport validationSupport = new ValidationSupport();

    public void init() {
        final var dialogPane = getDialogPane();

        initStyle(StageStyle.UNDECORATED);
        dialogPane.setHeaderText("Create New Role for OSGi User Admin");
        dialogPane.getStylesheets().add(LoginDialog.class.getResource("dialogs.css").toExternalForm());
        dialogPane.getStylesheets().add(getClass().getResource(STANDARD_CSS).toExternalForm());
        dialogPane.setGraphic(new ImageView(this.getClass().getResource("/graphic/images/role.png").toString()));
        dialogPane.getButtonTypes().addAll(ButtonType.CANCEL);

        final var dropdownRoleType = new ComboBox<String>();
        dropdownRoleType.getItems().addAll(FXCollections.observableArrayList("User", "Group"));

        final var txtRoleName = (CustomTextField) TextFields.createClearableTextField();
        txtRoleName.setLeft(new ImageView(getClass().getResource("/graphic/icons/id.png").toExternalForm()));
        validationSupport.registerValidator(txtRoleName, Validator.createEmptyValidator("Invalid Role Name"));

        final var lbMessage = new Label("");
        lbMessage.getStyleClass().addAll("message-banner");
        lbMessage.setVisible(false);
        lbMessage.setManaged(false);

        final var content = new VBox(10);

        content.getChildren().add(lbMessage);
        content.getChildren().add(dropdownRoleType);
        content.getChildren().add(txtRoleName);

        dialogPane.setContent(content);

        final var createButtonType = new ButtonType("Create", ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(createButtonType);

        final var createButton = (Button) dialogPane.lookupButton(createButtonType);
        createButton.setOnAction(actionEvent -> {
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
        final var pidCaption = "Role Name";

        dropdownRoleType.getSelectionModel().select(0);
        txtRoleName.setPromptText(pidCaption);

        createButton.disableProperty()
                .bind(txtRoleName.textProperty().isEmpty().or(validationSupport.invalidProperty()));

        setResultConverter(param -> {
            final var data = param == null ? null : param.getButtonData();
            try {
                if (data == ButtonData.OK_DONE) {
                    verify(!validationSupport.isInvalid(), "Role name validation failed");
                    return getInput(dropdownRoleType.getValue(), txtRoleName.getText());
                }
                return null;
            } catch (final Exception e) {
                logger.atError().withException(e).log("Role cannot be created");
                throw e;
            }
        });
    }

    private RoleDTO getInput(final String roleType, final String roleName) {
        final var type = EnumUtils.getEnumIgnoreCase(XRoleDTO.Type.class, roleType);
        verify(type != null, "Role type cannot be mapped to any existing type");
        return new RoleDTO(roleName, type);
    }

}
