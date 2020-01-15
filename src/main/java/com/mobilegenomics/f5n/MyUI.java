package com.mobilegenomics.f5n;

import com.mobilegenomics.f5n.controller.DataController;
import com.mobilegenomics.f5n.controller.UIController;
import com.mobilegenomics.f5n.core.Argument;
import com.mobilegenomics.f5n.core.PipelineStep;
import com.mobilegenomics.f5n.dto.State;
import com.mobilegenomics.f5n.dto.WrapperObject;
import com.mobilegenomics.f5n.support.FileServer;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.ui.ContentMode;
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

    public static Label averageProcessingTimeLabel;
    public static Label jobCompletionRateLabel;
    public static Label jobFailureRateLabel;
    public static Label newJobArrivalRateLabel;
    public static Label newJobRequestRateLabel;
    private TextField dataPathInput;
    private TextField timeoutInput;
    private TabSheet pipelineComponentsLayout;

    private VerticalLayout rootLayout;
    private HorizontalLayout gridLayout;

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        setupLayout();
        UI.getCurrent().setPollInterval(3000);
    }

    private void setupLayout() {
        rootLayout = new VerticalLayout();
        rootLayout.addStyleName("mystyle");

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
        serverStatisticsCalcLayout();
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

        VerticalLayout timeoutLayout = new VerticalLayout();
        timeoutInput = new TextField();
        timeoutInput.setPlaceholder("Set timeout in seconds");
        timeoutInput.setEnabled(false);
        CheckBox userTimeoutCheck = new CheckBox("Enable Job Timeout");
        timeoutLayout.setMargin(false);
        timeoutLayout.addComponents(userTimeoutCheck, timeoutInput);
        userTimeoutCheck.addValueChangeListener(event -> {
            if (event.getValue())
                timeoutInput.setEnabled(true);
            else
                timeoutInput.setEnabled(false);
        });

        CheckBox automateListingCheck = new CheckBox("Automate Job Listing");
        Button btnStartServer = new Button("Start Server");
        btnStartServer.addClickListener(event -> {
            if (dataPathInput.getValue() != null && !dataPathInput.getValue().trim().isEmpty()) {
                if (event.getButton().getCaption().equals("Start Server")) {
                    UIController.configurePipelineComponents(pipelineComponentsLayout);
                    UIController.configureWrapperObjects(dataPathInput.getValue().trim(), automateListingCheck.getValue());
                    createGrids();
                    if (userTimeoutCheck.getValue())
                        DataController.setAverageProcessingTime(Long.valueOf(timeoutInput.getValue()));
                    UIController.runServer();
                    FileServer.startFTPServer(8000, dataPathInput.getValue().trim());
                    UIController.startServerStatisticsCalc();
                    event.getButton().setCaption("Stop Server");
                    automateListingCheck.setEnabled(false);
                    pipelineComponentsLayout.setEnabled(false);
                } else {
                    UIController.stopServer();
                    FileServer.stopFileServer();
                    DataController.getFilePrefixes().clear();
                    UIController.clearWrapperObjects();
                    DataController.fileDirMonitorDetach();
                    UIController.resetServerStatisticsCalc();
                    removeGrids();
                    DataController.setAverageProcessingTime(DataController.getAverageProcessingTime());
                    event.getButton().setCaption("Start Server");
                    automateListingCheck.setEnabled(true);
                    pipelineComponentsLayout.setEnabled(true);
                }
            } else {
                dataPathInput.focus();
            }
        });


        btnLayout.addComponentsAndExpand(btnStartServer, automateListingCheck, timeoutLayout);
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
        CheckBox checkBoxDefaultValues = new CheckBox("Set default values");
        checkBoxDefaultValues.setId(componentName + "_checkbox_prepend_" + componentName);
        tabLayout.addComponent(checkBoxDefaultValues);
        for (Argument argument : UIController.getSteps().get(PipelineStep.valueOf(componentName).getValue()).getArguments()) {
            CheckBox checkBox = new CheckBox(argument.getArgName());
            checkBox.setId(componentName + "_checkbox_" + argument.getArgName());
            tabLayout.addComponents(checkBox);
            if (!argument.isFlagOnly()) {
                TextField argumentInput = new TextField(argument.getArgName());
                argumentInput.setWidth("300px");
                argumentInput.setId(componentName + "_textfield_" + argument.getArgName());
                if (argument.isRequired()) {
                    checkBox.setValue(true);
                    checkBox.setEnabled(false);
                    argumentInput.setRequiredIndicatorVisible(true);
                }
                tabLayout.addComponent(argumentInput);
            }
        }
        pipelineComponentsLayout.addTab(tabLayout, componentName);

        checkBoxDefaultValues.addValueChangeListener(event -> {
            boolean isSetDefaultArg;
            isSetDefaultArg = event.getValue();
            for (Argument argument : UIController.getSteps().get(PipelineStep.valueOf(componentName).getValue()).getArguments()) {
                if (!argument.isFlagOnly() && argument.isFile()) {
                    TextField argumentInput = (TextField) UIController.findComponentById(tabLayout, componentName + "_textfield_" + argument.getArgName());
                    if(isSetDefaultArg)
                        argumentInput.setValue(argument.getArgValue());
                    else
                        argumentInput.setValue("");
                }
            }
        });
    }

    private void dataPathSetterLayout() {
        FormLayout formFilePath = new FormLayout();

        dataPathInput = new TextField("Data Set Path: ");
        dataPathInput.setPlaceholder("Path to the genome data directory");
        dataPathInput.setWidth(30, Unit.EM);
        dataPathInput.setIcon(VaadinIcons.FILE);
        dataPathInput.setRequiredIndicatorVisible(true);
        formFilePath.addComponent(dataPathInput);

        rootLayout.addComponent(formFilePath);
    }

    private void serverStatisticsCalcLayout() {

        FormLayout statisticsLayout = new FormLayout();
        averageProcessingTimeLabel = new Label();
        averageProcessingTimeLabel.setCaption("Average Processing Time");
        averageProcessingTimeLabel.setStyleName(ValoTheme.LABEL_BOLD);
        averageProcessingTimeLabel.setStyleName(ValoTheme.LABEL_NO_MARGIN);
        averageProcessingTimeLabel.setValue("0");
        jobCompletionRateLabel = new Label();
        jobCompletionRateLabel.setCaption("Job Completion Rate");
        jobCompletionRateLabel.setStyleName(ValoTheme.LABEL_BOLD);
        jobCompletionRateLabel.setStyleName(ValoTheme.LABEL_NO_MARGIN);
        jobCompletionRateLabel.setValue("0");
        jobFailureRateLabel = new Label();
        jobFailureRateLabel.setCaption("Job Failure Rate");
        jobFailureRateLabel.setStyleName(ValoTheme.LABEL_BOLD);
        jobFailureRateLabel.setStyleName(ValoTheme.LABEL_NO_MARGIN);
        jobFailureRateLabel.setValue("0");
        newJobArrivalRateLabel = new Label();
        newJobArrivalRateLabel.setCaption("New Job Arrival Rate");
        newJobArrivalRateLabel.setStyleName(ValoTheme.LABEL_BOLD);
        newJobArrivalRateLabel.setStyleName(ValoTheme.LABEL_NO_MARGIN);
        newJobArrivalRateLabel.setValue("0");
        newJobRequestRateLabel = new Label();
        newJobRequestRateLabel.setCaption("New Job Request Rate");
        newJobRequestRateLabel.setStyleName(ValoTheme.LABEL_BOLD);
        newJobRequestRateLabel.setStyleName(ValoTheme.LABEL_NO_MARGIN);
        newJobRequestRateLabel.setValue("0");

        statisticsLayout.addComponents(averageProcessingTimeLabel, jobCompletionRateLabel, jobFailureRateLabel, newJobArrivalRateLabel, newJobRequestRateLabel);
        statisticsLayout.setMargin(false);
        rootLayout.addComponent(statisticsLayout);
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
                    label.setContentMode(ContentMode.PREFORMATTED);
                    StringBuilder newResultSummary = new StringBuilder();
                    newResultSummary.append(wrapper.getResultSummery());
                    newResultSummary.append("\n");
                    newResultSummary.append("Processed Job Prefix: " + wrapper.getPrefix());
                    newResultSummary.append("\n");
                    newResultSummary.append("Client Address: " + wrapper.getClientIP());
                    newResultSummary.append("\n");
                    Long jobProcessTime = wrapper.getCollectTime() - wrapper.getReleaseTime();
                    newResultSummary.append("Total Job Processing Time: " + jobProcessTime + "ms");
                    label.setValue(newResultSummary.toString());
                    window.setContent(label);
                    addWindow(window);
                } else {
                    Notification.show("Summery",
                            "Job result is pending",
                            Notification.Type.HUMANIZED_MESSAGE);
                }
            });
            return button;
        }).setCaption("Summary");

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
