package com.mobilegenomics.f5n.controller;

import com.mobilegenomics.f5n.MyUI;
import com.mobilegenomics.f5n.core.Argument;
import com.mobilegenomics.f5n.core.PipelineStep;
import com.mobilegenomics.f5n.support.FileServer;
import com.mobilegenomics.f5n.support.TimeFormat;
import com.vaadin.ui.*;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class UIController {

    private static ArrayList<String> componentsNameList;
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

    public void initiateUISettings() {
        pipelineComponentsController();
        jobTimeOutController();
        runServerController();
    }

    private void pipelineComponentsController() {
        MyUI.pipelineComponentsCheckGroup.setItems("MINIMAP2_SEQUENCE_ALIGNMENT", "SAMTOOLS_SORT", "SAMTOOLS_INDEX", "F5C_INDEX", "F5C_CALL_METHYLATION", "F5C_EVENT_ALIGNMENT", "F5C_METH_FREQ");

        MyUI.pipelineComponentsCheckGroup.addValueChangeListener(event -> {
            if (MyUI.jobStatusGridsLayout != null)
                MyUI.rootLayout.removeComponent(MyUI.jobStatusGridsLayout);
            if (!event.getValue().isEmpty()) {
                MyUI.serverButtonsLayout.setEnabled(true);
                MyUI.pipelineComponentsLayout.removeAllComponents();
                CoreController.eraseSelectedPipeline();
                componentsNameList = new ArrayList<>();
                DataController.getFilePrefixes().clear();
                CoreController.addPipelineSteps(MyUI.pipelineComponentsCheckGroup.getSelectedItems());
                MyUI.pipelineComponentsLayout.addTab(MyUI.pipelineComponentsCheckGroup, "Components List");
                for (String componentName : event.getValue()) {
                    pipelineComponentsGenerator(MyUI.pipelineComponentsLayout, componentName);
                    componentsNameList.add(componentName);
                }
            } else {
                MyUI.serverButtonsLayout.setEnabled(false);
                MyUI.pipelineComponentsLayout.removeAllComponents();
                MyUI.pipelineComponentsLayout.addTab(MyUI.pipelineComponentsCheckGroup, "Components List");
            }
        });
    }

    private void runServerController() {
        MyUI.btnStartServer.addClickListener(event -> {
            if (MyUI.dataSetPathInput.getValue() != null && !MyUI.dataSetPathInput.getValue().trim().isEmpty()) {
                if (event.getButton().getCaption().equals("Start Server")) {
                    readPipelineComponents(MyUI.pipelineComponentsLayout);
                    CoreController.buildCommandString();
                    DataController.createWrapperObjects(MyUI.dataSetPathInput.getValue().trim(), MyUI.automateListingCheck.getValue());
                    MyUI.jobsStatusGridsView();
                    if (MyUI.userTimeoutCheck.getValue()) {
                        if (!isTimeOutSeconds)
                            DataController.setAverageProcessingTime(Long.parseLong(MyUI.timeoutInput.getValue()) * 60);
                        else
                            DataController.setAverageProcessingTime(Long.parseLong(MyUI.timeoutInput.getValue()));
                    }
                    ServerController.runServer();
                    FileServer.startFTPServer(8000, MyUI.dataSetPathInput.getValue().trim());
                    UIController.startServerStatisticsCalc();
                    event.getButton().setCaption("Stop Server");
                    MyUI.automateListingCheck.setEnabled(false);
                    MyUI.pipelineComponentsLayout.setEnabled(false);
                } else {
                    ServerController.stopServer();
                    FileServer.stopFileServer();
                    DataController.getFilePrefixes().clear();
                    DataController.clearWrapperObjects();
                    DataController.fileDirMonitorDetach();
                    UIController.resetServerStatisticsCalc();
                    MyUI.removeJobStatusGridsView();
                    DataController.setAverageProcessingTime(DataController.getAverageProcessingTime());
                    event.getButton().setCaption("Start Server");
                    MyUI.automateListingCheck.setEnabled(true);
                    MyUI.pipelineComponentsLayout.setEnabled(true);
                }
            } else {
                MyUI.dataSetPathInput.focus();
            }
        });
    }

    private void jobTimeOutController() {
        MyUI.timeoutInput.setEnabled(false);
        MyUI.selectTimeInputType.setItems("Seconds", "Minutes");
        MyUI.selectTimeInputType.setSelectedItem("Seconds");
        MyUI.timeoutInput.setPlaceholder("Set timeout in seconds");
        MyUI.selectTimeInputType.setEnabled(false);
        MyUI.selectTimeInputType.addValueChangeListener(event -> {
            if (event.getValue().equals("Seconds")) {
                isTimeOutSeconds = true;
                MyUI.timeoutInput.setPlaceholder("Set timeout in seconds");
            } else {
                isTimeOutSeconds = false;
                MyUI.timeoutInput.setPlaceholder("Set timeout in minutes");
            }
        });

        MyUI.userTimeoutCheck.addValueChangeListener(event -> {
            if (event.getValue()) {
                MyUI.timeoutInput.setEnabled(true);
                MyUI.selectTimeInputType.setEnabled(true);
            } else {
                MyUI.timeoutInput.setEnabled(false);
                MyUI.selectTimeInputType.setEnabled(false);
            }
        });
    }

    private void pipelineComponentsGenerator(TabSheet pipelineComponentsLayout, String componentName) {
        MyUI.componentTabLayout = new FormLayout();
        MyUI.componentTabLayout.setMargin(true);
        CheckBox checkBoxDefaultValues = new CheckBox("Set default values");
        checkBoxDefaultValues.setId(componentName + "_checkbox_prepend_" + componentName);
        MyUI.componentTabLayout.addComponent(checkBoxDefaultValues);
        for (Argument argument : CoreController.getSteps().get(PipelineStep.valueOf(componentName).getValue()).getArguments()) {
            CheckBox checkBox = new CheckBox(argument.getArgName());
            checkBox.setId(componentName + "_checkbox_" + argument.getArgName());
            MyUI.componentTabLayout.addComponents(checkBox);
            if (argument.getArgID().equals("MINIMAP2_REF_FILE") || argument.getArgID().equals("F5C_METH_REF_FILE") || argument.getArgID().equals("F5C_ALIGN_REF_FILE")) {
                CheckBox checkBoxExtra = new CheckBox("Use Internal Reference File");
                checkBoxExtra.setId(componentName + "_checkbox_" + "internal_reference");
                MyUI.componentTabLayout.addComponents(checkBoxExtra);
            }
            if (!argument.isFlagOnly()) {
                TextField argumentInput = new TextField(argument.getArgName());
                argumentInput.setWidth("300px");
                argumentInput.setId(componentName + "_textfield_" + argument.getArgName());
                if (argument.isRequired()) {
                    checkBox.setValue(true);
                    checkBox.setEnabled(false);
                    argumentInput.setRequiredIndicatorVisible(true);
                }
                MyUI.componentTabLayout.addComponent(argumentInput);
            } else {
                if (argument.getArgID().equals("MINIMAP2_GENERATE_CIGAR")) {
                    CheckBox checkBoxSAM = (CheckBox) UIController.findComponentById(MyUI.componentTabLayout, componentName + "_checkbox_" + "Output SAM format (Default PAF format)");
                    CheckBox checkBoxPAF = (CheckBox) UIController.findComponentById(MyUI.componentTabLayout, componentName + "_checkbox_" + argument.getArgName());
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
        pipelineComponentsLayout.addTab(MyUI.componentTabLayout, componentName);

        checkBoxDefaultValues.addValueChangeListener(event -> {
            boolean isSetDefaultArg;
            isSetDefaultArg = event.getValue();
            for (Argument argument : CoreController.getSteps().get(PipelineStep.valueOf(componentName).getValue()).getArguments()) {
                if (!argument.isFlagOnly() && argument.isRequired()) {
                    TextField argumentInput = (TextField) UIController.findComponentById(pipelineComponentsLayout, componentName + "_textfield_" + argument.getArgName());
                    assert argumentInput != null;
                    if (isSetDefaultArg) {
                        argument.setArgValue(argument.getArgValue().replace("$DATA_SET/", ""));
                        argument.setArgValue(argument.getArgValue().replace("$REF_GNOME/", ""));
                        argumentInput.setValue(argument.getArgValue());
                    } else {
                        argumentInput.setValue("");
                    }
                }
            }
        });
    }

    public void readPipelineComponents(TabSheet pipelineComponentsLayout) {
        String DATA_SET = "$DATA_SET/";
        String REF_GNOME = "$REF_GNOME/";
        ArrayList<Argument> arguments;
        for (String componentName : componentsNameList) {
            arguments = CoreController.getSteps().get(PipelineStep.valueOf(componentName).getValue()).getArguments();
            for (Argument argument : arguments) {
                CheckBox checkBox = (CheckBox) findComponentById(pipelineComponentsLayout, componentName + "_checkbox_" + argument.getArgName());
                assert checkBox != null;
                if (checkBox.getValue() && !argument.isFlagOnly()) {
                    TextField argumentInput = (TextField) findComponentById(pipelineComponentsLayout, componentName + "_textfield_" + argument.getArgName());
                    if (argumentInput != null && !argumentInput.isEmpty() && argument.isFile()) {
                        if (argument.getArgID().equals("MINIMAP2_REF_FILE") || argument.getArgID().equals("F5C_METH_REF_FILE") || argument.getArgID().equals("F5C_ALIGN_REF_FILE")) {
                            CheckBox checkBoxExtra = (CheckBox) findComponentById(pipelineComponentsLayout, componentName + "_checkbox_" + "internal_reference");
                            assert checkBoxExtra != null;
                            if (checkBoxExtra.getValue())
                                argument.setArgValue(REF_GNOME + "" + argumentInput.getValue());
                            else
                                argument.setArgValue(DATA_SET + "" + argumentInput.getValue());
                        } else {
                            argument.setArgValue(DATA_SET + "" + argumentInput.getValue());
                        }
                        argument.setSetByUser(true);
                    } else if (argumentInput != null && !argumentInput.isEmpty()) {
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

    public static void startServerStatisticsCalc() {
        DataController.calculateStats();
        Timer t = new Timer();
        MyUI.serverStartTimeLabel.setValue(TimeFormat.currentDateTime());
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                DataController.calculateStats();
                MyUI.jobCompletionRateLabel.setValue(String.valueOf(DataController.getJobCompletionRate()));
                MyUI.jobFailureRateLabel.setValue(String.valueOf(DataController.getJobFailureRate()));
                MyUI.newJobArrivalRateLabel.setValue(String.valueOf(DataController.getNewJobArrivalRate()));
                MyUI.newJobRequestRateLabel.setValue(String.valueOf(DataController.getNewJobRequestRate()));
            }
        }, DataController.statWatchTimerInMinutes * 60 * 1000, DataController.statWatchTimerInMinutes * 60 * 1000);
    }

    public static void resetServerStatisticsCalc() {
        MyUI.serverStartTimeLabel.setValue("-");
        MyUI.jobCompletionRateLabel.setValue(String.valueOf(0));
        MyUI.jobFailureRateLabel.setValue(String.valueOf(0));
        MyUI.newJobArrivalRateLabel.setValue(String.valueOf(0));
        MyUI.newJobRequestRateLabel.setValue(String.valueOf(0));
    }
}
