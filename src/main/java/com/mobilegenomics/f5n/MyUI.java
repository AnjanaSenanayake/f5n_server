package com.mobilegenomics.f5n;

import com.mobilegenomics.f5n.controller.DataController;
import com.mobilegenomics.f5n.controller.UIController;
import com.mobilegenomics.f5n.core.Argument;
import com.mobilegenomics.f5n.dto.State;
import com.mobilegenomics.f5n.dto.WrapperObject;
import com.mobilegenomics.f5n.support.FileServer;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

import javax.servlet.annotation.WebServlet;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * This UI is the application entry point. A UI may either represent a browser window
 * (or tab) or some part of an HTML page where a Vaadin application is embedded.
 * <p>
 * The UI is initialized using {@link #init(VaadinRequest)}. This method is intended to be
 * overridden to add component to the user interface and initialize non-component functionality.
 */
@Theme("mytheme")
public class MyUI extends UI {

    private VerticalLayout rootLayout;
    private TextField dataPathInput;
    public static Label averageProcessingTimeLabel;
    private TabSheet pipelineComponentsLayout;
    private HorizontalLayout gridLayout;

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        setupLayout();
        UI.getCurrent().setPollInterval(3000);
    }

    private void setupLayout() {
        rootLayout = new VerticalLayout();

        Label headerLabel = new Label();
        headerLabel.setValue("Welcome to the f5n Server");
        headerLabel.addStyleName(ValoTheme.LABEL_H1);
        headerLabel.addStyleName(ValoTheme.LABEL_NO_MARGIN);
        rootLayout.addComponent(headerLabel);

        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getLocalHost();
            Label networkHostLabel = new Label();
            networkHostLabel.setValue("Host Name: " + inetAddress.getHostName() + " IP Address: " + DataController.getLocalIPAddress());
            networkHostLabel.addStyleName(ValoTheme.LABEL_H3);
            rootLayout.addComponent(networkHostLabel, 1);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        dataPathSetterLayout();
        generatePipelineComponentsLayout();

        setContent(rootLayout);
    }

    private void generatePipelineComponentsLayout() {

        Label checkBoxGroupLabel = new Label();
        checkBoxGroupLabel.setValue("Select Pipeline Components");
        checkBoxGroupLabel.addStyleName(ValoTheme.LABEL_LARGE);
        checkBoxGroupLabel.addStyleName(ValoTheme.LABEL_NO_MARGIN);

        pipelineComponentsLayout = new TabSheet();
        pipelineComponentsLayout.setHeight(100.0f, Unit.PERCENTAGE);
        pipelineComponentsLayout.addStyleName(ValoTheme.TABSHEET_FRAMED);
        pipelineComponentsLayout.addStyleName(ValoTheme.TABSHEET_PADDED_TABBAR);
        CheckBoxGroup<String> pipelineComponentsCheckGroup = new CheckBoxGroup<>("Select components");
        pipelineComponentsCheckGroup.setItems("MINIMAP2_SEQUENCE_ALIGNMENT", "SAMTOOL_SORT", "SAMTOOL_INDEX", "F5C_INDEX", "F5C_CALL_METHYLATION", "F5C_EVENT_ALIGNMENT");
        pipelineComponentsCheckGroup.addStyleName(ValoTheme.CHECKBOX_LARGE);
        pipelineComponentsLayout.addTab(pipelineComponentsCheckGroup, "Components List");

        HorizontalLayout btnLayout = new HorizontalLayout();

        CheckBox automateListingCheck = new CheckBox("Automate Job Listing");
        Button btnStartServer = new Button("Start Server");
        btnStartServer.addClickListener(event -> {
            if (dataPathInput.getValue() != null && !dataPathInput.getValue().trim().isEmpty()) {
                if (event.getButton().getCaption().equals("Start Server")) {
                    UIController.configurePipelineComponents(pipelineComponentsLayout);
                    UIController.configureWrapperObjects(dataPathInput.getValue().trim(), automateListingCheck.getValue());
                    createGrids();
                    UIController.runServer();
                    FileServer.startFileServer(8000, dataPathInput.getValue().trim());
                    event.getButton().setCaption("Stop Server");
                    automateListingCheck.setEnabled(false);
                    pipelineComponentsLayout.setEnabled(false);
                } else {
                    UIController.stopServer();
                    //FileServer.stopFileServer();
                    DataController.getFilePrefixes().clear();
                    UIController.clearWrapperObjects();
                    DataController.fileDirMonitorDetach();
                    removeGrids();
                    event.getButton().setCaption("Start Server");
                    automateListingCheck.setEnabled(true);
                    pipelineComponentsLayout.setEnabled(true);
                }
            } else {
                dataPathInput.focus();
            }
        });

        averageProcessingTimeLabel = new Label();
        averageProcessingTimeLabel.setCaption("Average Processing Time:");
        averageProcessingTimeLabel.setValue("0");
        btnLayout.addComponentsAndExpand(btnStartServer, automateListingCheck, averageProcessingTimeLabel);

        pipelineComponentsCheckGroup.addValueChangeListener(event -> {
            if (gridLayout != null)
                rootLayout.removeComponent(gridLayout);
            if (!event.getValue().isEmpty()) {
                btnLayout.setEnabled(true);
                pipelineComponentsLayout.removeAllComponents();
                UIController.eraseSelectedPipelineSteps();
                UIController.getComponentsNameList();
                DataController.getFilePrefixes().clear();
                UIController.addPipelineSteps(pipelineComponentsCheckGroup.getSelectedItems());
                pipelineComponentsLayout.addTab(pipelineComponentsCheckGroup, "Components List");
                for (String componentName : event.getValue()) {
                    generatePipelineComponentArgumentLayout(pipelineComponentsLayout, componentName);
                    UIController.setComponentsNames(componentName);
                }
            } else {
                btnLayout.setEnabled(false);
                pipelineComponentsLayout.removeAllComponents();
                pipelineComponentsLayout.addTab(pipelineComponentsCheckGroup, "Components List");
            }
        });
        rootLayout.addComponents(checkBoxGroupLabel, pipelineComponentsLayout);
        rootLayout.addComponent(btnLayout);
    }

    private void generatePipelineComponentArgumentLayout(TabSheet pipelineComponentsLayout, String componentName) {
        FormLayout tabLayout = new FormLayout();
        tabLayout.setMargin(true);
        CheckBox checkBox_prepend = new CheckBox("Prepend data set path to file inputs");
        checkBox_prepend.setId(componentName + "_checkbox_prepend_" + componentName);
        tabLayout.addComponent(checkBox_prepend);
        for (Argument argument : UIController.getSteps().get(componentName).getArguments()) {

            CheckBox checkBox = new CheckBox(argument.getArgName());
            checkBox.setId(componentName + "_checkbox_" + argument.getArgName());
            tabLayout.addComponents(checkBox);
            if (!argument.isFlagOnly()) {
                TextField argumentInput = new TextField(argument.getArgName());
                argumentInput.setId(componentName + "_textfield_" + argument.getArgName());
                if (argument.isRequired()) {
                    checkBox.setValue(true);
                    checkBox.setEnabled(false);
                    argumentInput.setRequiredIndicatorVisible(true);
                }
                if (argument.getArgValue() != null)
                    argumentInput.setValue(argument.getArgValue());
                tabLayout.addComponent(argumentInput);
            }
        }
        pipelineComponentsLayout.addTab(tabLayout, componentName);

        checkBox_prepend.addValueChangeListener(event -> {
            String DATA_SET_PATH;
            if (event.getValue()) {
                DATA_SET_PATH = "$DATA_SET_PATH/";
            } else {
                DATA_SET_PATH = "";
            }
            for (Argument argument : UIController.getSteps().get(componentName).getArguments()) {
                if (!argument.isFlagOnly() && argument.isFile()) {
                    TextField argumentInput = (TextField) UIController.findComponentById(tabLayout, componentName + "_textfield_" + argument.getArgName());
                    argumentInput.setValue(DATA_SET_PATH);
                }
            }
        });
    }

    private void dataPathSetterLayout() {
        FormLayout formFilePath = new FormLayout();

        dataPathInput = new TextField("Data Set Path: ");
        dataPathInput.setPlaceholder("Path to the genome data directory");
        dataPathInput.setWidth(20, Unit.EM);
        dataPathInput.setIcon(VaadinIcons.FILE);
        dataPathInput.setRequiredIndicatorVisible(true);
        formFilePath.addComponent(dataPathInput);

        rootLayout.addComponent(formFilePath);
    }

    private void createGrids() {
        gridLayout = new HorizontalLayout();

        Grid<WrapperObject> gridIdleJobs = new Grid<>();
        gridIdleJobs.setCaption("Unallocated Jobs");
        gridIdleJobs.setDataProvider(DataController.setListDataProvider(State.IDLE));
        gridIdleJobs.addColumn(WrapperObject::getPrefix).setCaption("Job ID");
        gridIdleJobs.addColumn(WrapperObject::getState).setCaption("State");
        gridIdleJobs.setSizeFull();
        gridIdleJobs.addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);

        Grid<WrapperObject> gridAllocatedJobs = new Grid<>();
        gridAllocatedJobs.setCaption("Allocated Jobs");
        gridAllocatedJobs.setDataProvider(DataController.setListDataProvider(State.PENDING));
        gridAllocatedJobs.addColumn(WrapperObject::getPrefix).setCaption("Job ID");
        gridAllocatedJobs.addColumn(WrapperObject::getState).setCaption("State");
        gridAllocatedJobs.addColumn(WrapperObject::getClientIP).setCaption("Client IP");

        gridAllocatedJobs.addComponentColumn(wrapper -> {
            Button button = new Button();
            button.setCaption("Summery");
            button.addClickListener(event -> {
                if (wrapper.getState() == State.SUCCESS) {
                    Window window = new Window();
                    window.setWidth("600px");
                    window.setHeight("400px");
                    Label label = new Label();
                    label.setValue(wrapper.getResultSummery());
                    window.setContent(label);
                    addWindow(window);
                } else {
                    Notification.show("Summery",
                            "Job result is pending",
                            Notification.Type.HUMANIZED_MESSAGE);
                }
            });
            return button;
        }).setCaption("Summery");

        gridAllocatedJobs.setSizeFull();
        gridAllocatedJobs.addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);

        gridLayout.addComponentsAndExpand(gridIdleJobs, gridAllocatedJobs);
        rootLayout.addComponent(gridLayout);
    }

    private void removeGrids() {
        rootLayout.removeComponent(gridLayout);
    }

    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {
    }
}
