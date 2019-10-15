package com.mobilegenomics.f5n;

import com.mobilegenomics.f5n.controller.DataController;
import com.mobilegenomics.f5n.controller.UIController;
import com.mobilegenomics.f5n.core.Argument;
import com.mobilegenomics.f5n.dto.State;
import com.mobilegenomics.f5n.dto.WrapperObject;

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

    private  TabSheet pipelineComponentsLayout;

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
            networkHostLabel.setValue("Host Name: " + inetAddress.getHostName()+" IP Address: "+ UIController.getLocalIPAddress());
            networkHostLabel.addStyleName(ValoTheme.LABEL_H3);
            rootLayout.addComponent(networkHostLabel, 1);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        dataPathSetter();
        generatePipelineComponentsLayout();

        setContent(rootLayout);
    }

    private void generatePipelineComponentsLayout(){

        pipelineComponentsLayout = new TabSheet();
        pipelineComponentsLayout.setHeight(100.0f, Unit.PERCENTAGE);
        pipelineComponentsLayout.addStyleName(ValoTheme.TABSHEET_FRAMED);
        pipelineComponentsLayout.addStyleName(ValoTheme.TABSHEET_PADDED_TABBAR);

        Label checkBoxGroupLabel = new Label();
        checkBoxGroupLabel.setValue("Select Pipeline Components");
        checkBoxGroupLabel.addStyleName(ValoTheme.LABEL_LARGE);
        checkBoxGroupLabel.addStyleName(ValoTheme.LABEL_NO_MARGIN);

        CheckBoxGroup<String> pipelineComponentsCheckGroup = new CheckBoxGroup<>("Select components");
        pipelineComponentsCheckGroup.setItems("MINIMAP2_SEQUENCE_ALIGNMENT", "SAMTOOL_SORT", "SAMTOOL_INDEX", "F5C_INDEX", "F5C_CALL_METHYLATION", "F5C_EVENT_ALIGNMENT");
        pipelineComponentsCheckGroup.addStyleName(ValoTheme.CHECKBOX_LARGE);

        pipelineComponentsLayout.addTab(pipelineComponentsCheckGroup, "Components List");

        HorizontalLayout btnLayout = new HorizontalLayout();

        VerticalLayout layoutGenerateJobs = new VerticalLayout();
        Button btnGenerateJobs = new Button("Generate Job List");
        CheckBox automateListingCheck = new CheckBox("Automate Listing");
        //btnGenerateJobs.setEnabled(false);
        btnGenerateJobs.setDisableOnClick(true);

        btnGenerateJobs.addClickListener(event -> {
            UIController.configurePipelineComponents(pipelineComponentsLayout);
            UIController.configureWrapperObjects(dataPathInput.getValue());
            createGrids();
        });
        layoutGenerateJobs.addComponents(btnGenerateJobs, automateListingCheck);
        layoutGenerateJobs.setMargin(false);

        Button btnStartServer = new Button("Start Server");
        btnStartServer.addClickListener(event -> {
            UIController.runServer();
        });
        btnLayout.addComponents(layoutGenerateJobs, btnStartServer);

        pipelineComponentsCheckGroup.addValueChangeListener(event -> {

            if(!event.getValue().isEmpty()) {
                btnGenerateJobs.setEnabled(true);
                pipelineComponentsLayout.removeAllComponents();
                UIController.eraseSelectedPipelineSteps();
                UIController.getComponentsNameList();
                DataController.getFilePrefixes().clear();
                UIController.addPipelineSteps(pipelineComponentsCheckGroup.getSelectedItems());
                pipelineComponentsLayout.addTab(pipelineComponentsCheckGroup, "Components List");
                for(String componentName : event.getValue()) {
                    generatePipelineComponentArgumentLayout(pipelineComponentsLayout, componentName);
                    UIController.setComponentsNames(componentName);
                }
            } else {
                //btnGenerateJobs.setEnabled(false);
                pipelineComponentsLayout.removeAllComponents();
                pipelineComponentsLayout.addTab(pipelineComponentsCheckGroup, "Components List");
            }
        });

        rootLayout.addComponents(checkBoxGroupLabel, pipelineComponentsLayout, btnLayout);
    }

    private void generatePipelineComponentArgumentLayout(TabSheet pipelineComponentsLayout, String componentName) {
        FormLayout tabLayout = new FormLayout();
        tabLayout.setMargin(true);
        for (Argument argument : UIController.getSteps().get(componentName).getArguments()) {

            if(!argument.isRequired()) {
                CheckBox checkBox = new CheckBox(argument.getArgName());
                checkBox.setId("checkbox_" + argument.getArgName());
                tabLayout.addComponent(checkBox);
                if (!argument.isFlagOnly()) {
                    TextField argumentInput = new TextField(argument.getArgName());
                    argumentInput.setId("textfield_" + argument.getArgName());
                    if(argument.getArgValue() != null)
                        argumentInput.setValue(argument.getArgValue());
                    tabLayout.addComponent(argumentInput);
                }
            }
        }
        pipelineComponentsLayout.addTab(tabLayout, componentName);
    }

    private void dataPathSetter() {
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
        HorizontalLayout horizontalLayout = new HorizontalLayout();

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
        gridAllocatedJobs.setSizeFull();
        gridAllocatedJobs.addStyleName(ValoTheme.TABLE_NO_VERTICAL_LINES);

        horizontalLayout.addComponentsAndExpand(gridIdleJobs, gridAllocatedJobs);
        rootLayout.addComponentsAndExpand(horizontalLayout);
    }

    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {
    }
}
