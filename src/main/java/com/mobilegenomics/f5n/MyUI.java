package com.mobilegenomics.f5n;

import com.mobilegenomics.f5n.controller.DataController;
import com.mobilegenomics.f5n.controller.UIController;
import com.mobilegenomics.f5n.dto.State;
import com.mobilegenomics.f5n.dto.WrapperObject;
import com.mobilegenomics.f5n.support.TimeFormat;
import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.*;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;

import javax.servlet.ServletException;
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
@PreserveOnRefresh
public class MyUI extends UI {

    private static final long serialVersionUID = -3368134597486018167L;
    public static Label headerLabel;
    public static Label networkHostLabel;
    public static Label checkBoxGroupLabel;
    public static Label serverStartTimeLabel;
    public static Label averageProcessingTimeLabel;
    public static Label jobCompletionRateLabel;
    public static Label jobFailureRateLabel;
    public static Label newJobArrivalRateLabel;
    public static Label newJobRequestRateLabel;
    public static TextField dataSetPathInput;
    public static TextField timeoutInput;
    public static CheckBoxGroup<String> pipelineComponentsCheckGroup;
    public static TabSheet pipelineComponentsLayout;
    public static Button btnStartServer;
    public static CheckBox automateListingCheck;
    public static CheckBox userTimeoutCheck;
    public static RadioButtonGroup<String> selectTimeInputType;
    public static Grid<WrapperObject> gridIdleJobs;
    public static Grid<WrapperObject> gridAllocatedJobs;
    public static Window window;

    public static VerticalLayout rootLayout;
    public static FormLayout componentTabLayout;
    public static HorizontalLayout serverButtonsLayout;
    public static HorizontalLayout jobStatusGridsLayout;

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        setupLayout();
        UIController uiController = new UIController();
        uiController.initiateUISettings();
        UI.getCurrent().setPollInterval(3000);
    }

    private void setupLayout() {
        rootLayout = new VerticalLayout();
        rootLayout.addStyleName("mystyle");

        serverHeaderView();
        serverNetworkInformationView();
        dataSetPathView();
        pipelineComponentsView();
        serverStatisticsCalcView();
        setContent(rootLayout);
    }

    private void serverHeaderView() {
        headerLabel = new Label();
        headerLabel.setValue("Welcome to the f5n Server");
        headerLabel.addStyleName(ValoTheme.LABEL_H1);
        headerLabel.addStyleName(ValoTheme.LABEL_NO_MARGIN);
        rootLayout.addComponent(headerLabel);
    }

    private void serverNetworkInformationView() {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getLocalHost();
            networkHostLabel = new Label();
            networkHostLabel.setValue("Host Name: " + inetAddress.getHostName() + " IP Address: " + DataController.getLocalIPAddress());
            networkHostLabel.addStyleName(ValoTheme.LABEL_H3);
            rootLayout.addComponent(networkHostLabel, 1);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private void pipelineComponentsView() {
        checkBoxGroupLabel = new Label();
        checkBoxGroupLabel.setValue("Select Pipeline Components");
        checkBoxGroupLabel.addStyleName(ValoTheme.LABEL_LARGE);
        checkBoxGroupLabel.addStyleName(ValoTheme.LABEL_NO_MARGIN);

        pipelineComponentsLayout = new TabSheet();
        pipelineComponentsLayout.setHeight(100.0f, Unit.PERCENTAGE);
        pipelineComponentsLayout.addStyleName(ValoTheme.TABSHEET_FRAMED);
        pipelineComponentsLayout.addStyleName(ValoTheme.TABSHEET_PADDED_TABBAR);
        pipelineComponentsCheckGroup = new CheckBoxGroup<>("Select components");
        pipelineComponentsCheckGroup.addStyleName(ValoTheme.CHECKBOX_LARGE);
        pipelineComponentsLayout.addTab(pipelineComponentsCheckGroup, "Components List");

        serverButtonsLayout = new HorizontalLayout();
        VerticalLayout timeoutLayout = new VerticalLayout();
        userTimeoutCheck = new CheckBox("Enable Job Timeout");
        HorizontalLayout timeInputLayout = new HorizontalLayout();
        timeoutInput = new TextField();
        timeoutLayout.setMargin(false);
        selectTimeInputType = new RadioButtonGroup<>();

        timeInputLayout.addComponents(timeoutInput, selectTimeInputType);
        timeoutLayout.addComponents(userTimeoutCheck, timeInputLayout);

        automateListingCheck = new CheckBox("Automate Job Listing");
        btnStartServer = new Button("Start Server");

        serverButtonsLayout.addComponentsAndExpand(btnStartServer, automateListingCheck, timeoutLayout);

        rootLayout.addComponents(checkBoxGroupLabel, pipelineComponentsLayout);
        rootLayout.addComponent(serverButtonsLayout);
    }

    private void dataSetPathView() {
        FormLayout formFilePath = new FormLayout();
        dataSetPathInput = new TextField("Data Set Path: ");
        dataSetPathInput.setPlaceholder("Path to the genome data directory");
        dataSetPathInput.setWidth(30, Unit.EM);
        dataSetPathInput.setIcon(VaadinIcons.FILE);
        dataSetPathInput.setRequiredIndicatorVisible(true);
        formFilePath.addComponent(dataSetPathInput);
        rootLayout.addComponent(formFilePath);
    }

    private void serverStatisticsCalcView() {
        FormLayout statisticsLayout = new FormLayout();
        serverStartTimeLabel = new Label();
        serverStartTimeLabel.setCaption("Server started at: ");
        serverStartTimeLabel.setStyleName(ValoTheme.LABEL_BOLD);
        serverStartTimeLabel.setStyleName(ValoTheme.LABEL_NO_MARGIN);
        serverStartTimeLabel.setValue("-");
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

        statisticsLayout.addComponents(serverStartTimeLabel, averageProcessingTimeLabel, jobCompletionRateLabel, jobFailureRateLabel, newJobArrivalRateLabel, newJobRequestRateLabel);
        statisticsLayout.setMargin(false);
        rootLayout.addComponent(statisticsLayout);
    }

    public static void jobsStatusGridsView() {
        jobStatusGridsLayout = new HorizontalLayout();

        gridIdleJobs = new Grid<>();
        gridIdleJobs.setCaption("Unallocated Jobs");
        gridIdleJobs.setDataProvider(DataController.setListDataProvider(State.IDLE));
        gridIdleJobs.addColumn(WrapperObject::getPrefix).setCaption("Job ID");
        gridIdleJobs.addColumn(WrapperObject::getState).setCaption("State");
        gridIdleJobs.addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);
        gridIdleJobs.setWidth(80, Unit.PIXELS);

        gridAllocatedJobs = new Grid<>();
        gridAllocatedJobs.setCaption("Allocated Jobs");
        gridAllocatedJobs.setDataProvider(DataController.setListDataProvider(State.PENDING));
        gridAllocatedJobs.addColumn(WrapperObject::getPrefix).setCaption("Job ID");
        gridAllocatedJobs.addColumn(WrapperObject::getState).setCaption("State");
        gridAllocatedJobs.addColumn(wrapperObject -> TimeFormat.millisToDateTime(wrapperObject.getReleaseTime())).setCaption("Release Time");
        gridAllocatedJobs.addColumn(wrapperObject -> {
            if (wrapperObject.getCollectTime() != null) {
                return TimeFormat.millisToDateTime(wrapperObject.getCollectTime());
            } else
                return "---";
        }).setCaption("Collect Time");
        gridAllocatedJobs.addColumn(WrapperObject::getClientIP).setCaption("Client IP");
        gridAllocatedJobsAddSummeryColumn();

        gridAllocatedJobs.setSizeFull();
        gridAllocatedJobs.addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);

        jobStatusGridsLayout.addComponentsAndExpand(gridIdleJobs, gridAllocatedJobs);
        rootLayout.addComponent(jobStatusGridsLayout);
    }

    public static void removeJobStatusGridsView() {
        rootLayout.removeComponent(jobStatusGridsLayout);
    }

    private static void gridAllocatedJobsAddSummeryColumn() {
        gridAllocatedJobs.addComponentColumn(wrapper -> {
            Button button = new Button();
            button.setCaption("Summary");
            button.addClickListener(event -> {
                if (wrapper.getState() == State.SUCCESS) {
                    final Window window = new Window("Job Result Summary");
                    window.setWidth(1000.0f, Unit.PIXELS);
                    final FormLayout content = new FormLayout();
                    content.setMargin(true);
                    Label label = new Label();
                    label.setContentMode(ContentMode.PREFORMATTED);
                    label.setValue(wrapper.getResultSummery());
                    content.addComponent(label);
                    window.setContent(content);
                    getCurrent().addWindow(window);
                } else {
                    Notification.show("Summary",
                            "Job result is pending",
                            Notification.Type.HUMANIZED_MESSAGE);
                }
            });
            return button;
        }).setCaption("Summary");
    }

    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet
            implements SessionInitListener, SessionDestroyListener {
        private static final long serialVersionUID = -706128960774628840L;

        @Override
        protected void servletInitialized() throws ServletException {
            super.servletInitialized();
            getService().addSessionInitListener(this);
            getService().addSessionDestroyListener(this);
        }

        @Override
        public void sessionInit(SessionInitEvent event)
                throws ServiceException {
            // Do session start stuff here
        }

        @Override
        public void sessionDestroy(SessionDestroyEvent event) {
            // Do session end stuff here
        }
    }
}
