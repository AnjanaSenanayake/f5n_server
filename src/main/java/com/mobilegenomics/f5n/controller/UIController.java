package com.mobilegenomics.f5n.controller;

import com.mobilegenomics.f5n.MyUI;
import com.mobilegenomics.f5n.core.Argument;
import com.mobilegenomics.f5n.core.PipelineStep;
import com.mobilegenomics.f5n.support.FileServer;
import com.vaadin.ui.*;

import java.util.ArrayList;

public class UIController {

    private static ArrayList<String> componentsNameList;
    private MyUI myUI;
    private boolean isTimeOutSeconds;

    public static Component findComponentById(HasComponents root, String id) {
        for (Component child : root) {
            if (id.equals(child.getId())) {
                return child; // found it!
            } else if (child instanceof HasComponents) { // recursively go through all children that themselves have children
                Component result = findComponentById((HasComponents) child, id);
                if (result != null) {
                    return result;
                }
            }
        }
        return null; // none was found
    }

    public void initiateUISettings(MyUI myUI) {
        this.myUI = myUI;
        pipelineComponentsController();
        jobTimeOutController();
        runServerController();
    }

    private void pipelineComponentsController() {
        myUI.pipelineComponentsCheckGroup.setItems("MINIMAP2_SEQUENCE_ALIGNMENT", "SAMTOOL_SORT", "SAMTOOL_INDEX", "F5C_INDEX", "F5C_CALL_METHYLATION", "F5C_EVENT_ALIGNMENT");

        myUI.pipelineComponentsCheckGroup.addValueChangeListener(event -> {
            if (myUI.jobStatusGridsLayout != null)
                myUI.rootLayout.removeComponent(myUI.jobStatusGridsLayout);
            if (!event.getValue().isEmpty()) {
                myUI.serverButtonsLayout.setEnabled(true);
                myUI.pipelineComponentsLayout.removeAllComponents();
                CoreController.eraseSelectedPipeline();
                componentsNameList = new ArrayList<>();
                DataController.getFilePrefixes().clear();
                CoreController.addPipelineSteps(myUI.pipelineComponentsCheckGroup.getSelectedItems());
                myUI.pipelineComponentsLayout.addTab(myUI.pipelineComponentsCheckGroup, "Components List");
                for (String componentName : event.getValue()) {
                    pipelineComponentsGenerator(myUI.pipelineComponentsLayout, componentName);
                    componentsNameList.add(componentName);
                }
            } else {
                myUI.serverButtonsLayout.setEnabled(false);
                myUI.pipelineComponentsLayout.removeAllComponents();
                myUI.pipelineComponentsLayout.addTab(myUI.pipelineComponentsCheckGroup, "Components List");
            }
        });
    }

    private void runServerController() {
        myUI.btnStartServer.addClickListener(event -> {
            if (myUI.dataSetPathInput.getValue() != null && !myUI.dataSetPathInput.getValue().trim().isEmpty()) {
                if (event.getButton().getCaption().equals("Start Server")) {
                    readPipelineComponents(myUI.pipelineComponentsLayout);
                    CoreController.buildCommandString();
                    DataController.createWrapperObjects(myUI.dataSetPathInput.getValue().trim(), myUI.automateListingCheck.getValue());
                    myUI.jobsStatusGridsView();
                    if (myUI.userTimeoutCheck.getValue()) {
                        if (!isTimeOutSeconds)
                            DataController.setAverageProcessingTime(Long.parseLong(myUI.timeoutInput.getValue()) * 60);
                        else
                            DataController.setAverageProcessingTime(Long.parseLong(myUI.timeoutInput.getValue()));
                    }
                    ServerController.runServer();
                    FileServer.startFTPServer(8000, myUI.dataSetPathInput.getValue().trim());
                    ServerController.startServerStatisticsCalc();
                    event.getButton().setCaption("Stop Server");
                    myUI.automateListingCheck.setEnabled(false);
                    myUI.pipelineComponentsLayout.setEnabled(false);
                } else {
                    ServerController.stopServer();
                    FileServer.stopFileServer();
                    DataController.getFilePrefixes().clear();
                    DataController.clearWrapperObjects();
                    DataController.fileDirMonitorDetach();
                    ServerController.resetServerStatisticsCalc();
                    myUI.removeJobStatusGridsView();
                    DataController.setAverageProcessingTime(DataController.getAverageProcessingTime());
                    event.getButton().setCaption("Start Server");
                    myUI.automateListingCheck.setEnabled(true);
                    myUI.pipelineComponentsLayout.setEnabled(true);
                }
            } else {
                myUI.dataSetPathInput.focus();
            }
        });
    }

    private void jobTimeOutController() {
        myUI.timeoutInput.setEnabled(false);
        myUI.selectTimeInputType.setItems("Seconds", "Minutes");
        myUI.selectTimeInputType.setSelectedItem("Seconds");
        myUI.timeoutInput.setPlaceholder("Set timeout in seconds");
        myUI.selectTimeInputType.setEnabled(false);
        myUI.selectTimeInputType.addValueChangeListener(event -> {
            if (event.getValue().equals("Seconds")) {
                isTimeOutSeconds = true;
                myUI.timeoutInput.setPlaceholder("Set timeout in seconds");
            } else {
                isTimeOutSeconds = false;
                myUI.timeoutInput.setPlaceholder("Set timeout in minutes");
            }
        });

        myUI.userTimeoutCheck.addValueChangeListener(event -> {
            if (event.getValue()) {
                myUI.timeoutInput.setEnabled(true);
                myUI.selectTimeInputType.setEnabled(true);
            } else {
                myUI.timeoutInput.setEnabled(false);
                myUI.selectTimeInputType.setEnabled(false);
            }
        });
    }

    private void pipelineComponentsGenerator(TabSheet pipelineComponentsLayout, String componentName) {
        myUI.componentTabLayout = new FormLayout();
        myUI.componentTabLayout.setMargin(true);
        CheckBox checkBoxDefaultValues = new CheckBox("Set default values");
        checkBoxDefaultValues.setId(componentName + "_checkbox_prepend_" + componentName);
        myUI.componentTabLayout.addComponent(checkBoxDefaultValues);
        for (Argument argument : CoreController.getSteps().get(PipelineStep.valueOf(componentName).getValue()).getArguments()) {
            CheckBox checkBox = new CheckBox(argument.getArgName());
            checkBox.setId(componentName + "_checkbox_" + argument.getArgName());
            myUI.componentTabLayout.addComponents(checkBox);
            if (!argument.isFlagOnly()) {
                TextField argumentInput = new TextField(argument.getArgName());
                argumentInput.setWidth("300px");
                argumentInput.setId(componentName + "_textfield_" + argument.getArgName());
                if (argument.isRequired()) {
                    checkBox.setValue(true);
                    checkBox.setEnabled(false);
                    argumentInput.setRequiredIndicatorVisible(true);
                }
                myUI.componentTabLayout.addComponent(argumentInput);
            } else {
                if (argument.getArgID().equals("MINIMAP2_GENERATE_CIGAR")) {
                    CheckBox checkBoxSAM = (CheckBox) UIController.findComponentById(myUI.componentTabLayout, componentName + "_checkbox_" + "Output SAM format (Default PAF format)");
                    CheckBox checkBoxPAF = (CheckBox) UIController.findComponentById(myUI.componentTabLayout, componentName + "_checkbox_" + argument.getArgName());
                    assert checkBoxSAM != null;
                    checkBoxSAM.setValue(true);
                    checkBoxSAM.addValueChangeListener(event -> {
                        assert checkBoxPAF != null;
                        checkBoxPAF.setValue(!event.getValue());
                        checkBoxPAF.setEnabled(!event.getValue());
                    });

                    assert checkBoxPAF != null;
                    checkBoxPAF.addValueChangeListener(event -> {
                        checkBoxSAM.setValue(!event.getValue());
                        checkBoxSAM.setEnabled(!event.getValue());
                    });
                }
            }

        }
        pipelineComponentsLayout.addTab(myUI.componentTabLayout, componentName);

        checkBoxDefaultValues.addValueChangeListener(event -> {
            boolean isSetDefaultArg;
            isSetDefaultArg = event.getValue();
            for (Argument argument : CoreController.getSteps().get(PipelineStep.valueOf(componentName).getValue()).getArguments()) {
                if (!argument.isFlagOnly() && argument.isRequired()) {
                    TextField argumentInput = (TextField) UIController.findComponentById(pipelineComponentsLayout, componentName + "_textfield_" + argument.getArgName());
                    assert argumentInput != null;
                    if (isSetDefaultArg) {
                        argumentInput.setValue(argument.getArgValue());
                    }
                    else {
                        argumentInput.setValue("");
                    }
                }
            }
        });
    }

    public void readPipelineComponents(TabSheet pipelineComponentsLayout) {
        String DATA_SET_PATH = "$DATA_SET_PATH/";
        ArrayList<Argument> arguments;
        for (String componentName : componentsNameList) {
            arguments = CoreController.getSteps().get(PipelineStep.valueOf(componentName).getValue()).getArguments();
            for (Argument argument : arguments) {
                CheckBox checkBox = (CheckBox) findComponentById(pipelineComponentsLayout, componentName + "_checkbox_" + argument.getArgName());
                assert checkBox != null;
                if (checkBox.getValue() && !argument.isFlagOnly()) {
                    TextField argumentInput = (TextField) findComponentById(pipelineComponentsLayout, componentName + "_textfield_" + argument.getArgName());
                    if (argumentInput != null && !argumentInput.isEmpty() && argument.isFile()) {
                        argument.setArgValue(DATA_SET_PATH + "" + argumentInput.getValue());
                        argument.setSetByUser(true);
                    } else if (argumentInput != null && !argumentInput.isEmpty() && argument.isRequired()) {
                        argument.setArgValue(argumentInput.getValue());
                        argument.setSetByUser(true);
                    }
                } else {
                    if (checkBox.getValue() != null && !checkBox.isEmpty()) {
                        argument.setSetByUser(true);
                    }
                }
                System.out.println(argument.getArgValue());
            }
        }
    }
}
