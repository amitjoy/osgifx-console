package in.bytehue.osgifx.console.application.fxml.controller;

import static in.bytehue.osgifx.console.supervisor.ConsoleSupervisor.AGENT_CONNECTED_EVENT_TOPIC;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.inject.Inject;

import org.controlsfx.control.table.TableFilter;
import org.controlsfx.dialog.ExceptionDialog;
import org.controlsfx.dialog.ProgressDialog;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.fx.core.command.CommandService;

import in.bytehue.osgifx.console.application.dialog.ConnectionDialog;
import in.bytehue.osgifx.console.application.dialog.ConnectionSettingDTO;
import in.bytehue.osgifx.console.application.preference.ConnectionsProvider;
import in.bytehue.osgifx.console.supervisor.ConsoleSupervisor;
import in.bytehue.osgifx.console.util.fx.DTOCellValueFactory;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.StageStyle;

public final class ConnectionSettingsWindowController implements Initializable {

    private static final String CONNECTION_WINDOW_ID         = "in.bytehue.osgifx.console.window.connection";
    private static final String COMMAND_ID_MANAGE_CONNECTION = "in.bytehue.osgifx.console.application.command.preference";

    @Inject
    private EModelService model;

    @Inject
    private MApplication application;

    @Inject
    private IEventBroker eventBroker;

    @Inject
    private ConsoleSupervisor supervisor;

    @Inject
    private CommandService commandService;

    @Inject
    private ConnectionsProvider connectionsProvider;

    @FXML
    private Button connectButton;

    @FXML
    private Button addConnectionButton;

    @FXML
    private Button removeConnectionButton;

    @FXML
    private TableView<ConnectionSettingDTO> connectionTable;

    @FXML
    private TableColumn<ConnectionSettingDTO, String> hostColumn;

    @FXML
    private TableColumn<ConnectionSettingDTO, Integer> portColumn;

    @FXML
    private TableColumn<ConnectionSettingDTO, Integer> timeoutColumn;

    ProgressDialog progressDialog;

    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        hostColumn.setCellValueFactory(new DTOCellValueFactory<>("host", String.class));
        portColumn.setCellValueFactory(new DTOCellValueFactory<>("port", Integer.class));
        timeoutColumn.setCellValueFactory(new DTOCellValueFactory<>("timeout", Integer.class));

        connectionTable.setItems(connectionsProvider.getConnections());
        connectionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                connectButton.setDisable(false);
                removeConnectionButton.setDisable(false);
            }
        });
        TableFilter.forTableView(connectionTable).apply();
    }

    @FXML
    public void handleClose(final ActionEvent event) {
        Platform.exit();
    }

    @FXML
    public void addConnection(final ActionEvent event) {
        final ConnectionDialog               connectionDialog = new ConnectionDialog();
        final Optional<ConnectionSettingDTO> value            = connectionDialog.showAndWait();
        if (value.isPresent()) {
            final ConnectionSettingDTO dto = value.get();
            triggerCommand(dto, "ADD");
        }
    }

    @FXML
    public void removeConnection(final ActionEvent event) {
        final ConnectionSettingDTO dto = connectionTable.getSelectionModel().getSelectedItem();
        triggerCommand(dto, "REMOVE");
    }

    @FXML
    public void connectAgent(final ActionEvent event) {
        final ConnectionSettingDTO selectedConnection = connectionTable.getSelectionModel().getSelectedItem();
        final Task<Void>           connectTask        = new Task<Void>() {
                                                          @Override
                                                          protected Void call() throws Exception {
                                                              try {
                                                                  supervisor.connect(selectedConnection.host, selectedConnection.port,
                                                                          selectedConnection.timeout);
                                                              } catch (final Exception e) {
                                                                  Platform.runLater(() -> {
                                                                                                                    progressDialog.close();
                                                                                                                    final ExceptionDialog dialog = new ExceptionDialog(
                                                                                                                            e);
                                                                                                                    dialog.initStyle(
                                                                                                                            StageStyle.UNDECORATED);
                                                                                                                    dialog.show();
                                                                                                                });
                                                                  throw e;
                                                              }
                                                              return null;
                                                          }

                                                          @Override
                                                          protected void succeeded() {
                                                              eventBroker.post(AGENT_CONNECTED_EVENT_TOPIC,
                                                                      selectedConnection.host + ":" + selectedConnection.port);
                                                          }
                                                      };

        final Thread th = new Thread(connectTask);
        th.setDaemon(true);
        th.start();

        progressDialog = new ProgressDialog(connectTask);
        progressDialog.setHeaderText("Connecting to " + selectedConnection.host + ":" + selectedConnection.port);
        progressDialog.initStyle(StageStyle.UNDECORATED);
        progressDialog.show();
    }

    private void triggerCommand(final ConnectionSettingDTO dto, final String type) {
        final Map<String, Object> properties = new HashMap<>();

        properties.put("host", dto.host);
        properties.put("port", dto.port);
        properties.put("timeout", dto.timeout);
        properties.put("type", type);

        commandService.execute(COMMAND_ID_MANAGE_CONNECTION, properties);
    }

    @Inject
    @org.eclipse.e4.core.di.annotations.Optional
    private void agentConnected(@UIEventTopic(AGENT_CONNECTED_EVENT_TOPIC) final String data) {
        final MWindow connectionChooserWindow = (MWindow) model.find(CONNECTION_WINDOW_ID, application);
        connectionChooserWindow.setVisible(false);
        progressDialog.close();
    }

}
