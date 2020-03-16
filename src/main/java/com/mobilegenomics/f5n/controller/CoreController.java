package com.mobilegenomics.f5n.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mobilegenomics.f5n.core.Argument;
import com.mobilegenomics.f5n.core.PipelineComponent;
import com.mobilegenomics.f5n.core.PipelineStep;
import com.mobilegenomics.f5n.core.Step;
import com.mobilegenomics.f5n.support.JSONFileHelper;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

public class CoreController {

    private static ArrayList<PipelineStep> selectedPipelineSteps = new ArrayList<>();
    private static TreeMap<Integer, Step> steps;
    private static ArrayList<PipelineComponent> pipelineComponents;

    private static int current = 0;

    static void addPipelineStep(PipelineStep step) {
        selectedPipelineSteps.add(step);
    }

    static TreeMap<Integer, Step> getSteps() {
        return steps;
    }

    static void eraseSelectedPipeline() {
        selectedPipelineSteps.clear();
    }

    public static void addPipelineSteps(Set<String> checkedPipelineSteps) {
        for (PipelineStep pipelineStep : PipelineStep.values()) {
            if (checkedPipelineSteps.contains(pipelineStep.name())) {
                CoreController.addPipelineStep(pipelineStep);
            }
        }
        printList();
        configureSteps();
    }

    static void printList() {
        for (PipelineStep step : selectedPipelineSteps) {
            System.out.println("STEPS = " + step.toString());
        }
    }

    static void configureSteps() {
        steps = new TreeMap<>();
        for (PipelineStep pipelineStep : selectedPipelineSteps) {
            ArrayList<Argument> arguments = configureArguments(pipelineStep);
            Step step = new Step(pipelineStep, arguments);
            steps.put(pipelineStep.getValue(), step);
        }
    }

    private static ArrayList<Argument> configureArguments(PipelineStep pipelineStep) {
        String rawFile = null;
        switch (pipelineStep) {
            case MINIMAP2_SEQUENCE_ALIGNMENT:
                rawFile = "minimap2.json";
                break;
            case SAMTOOL_SORT:
                rawFile = "samtool_sort_arguments.json";
                break;
            case SAMTOOL_INDEX:
                rawFile = "samtool_index_arguments.json";
                break;
            case F5C_INDEX:
                rawFile = "f5c_index_arguments.json";
                break;
            case F5C_CALL_METHYLATION:
                rawFile = "f5c_call_methylation_arguments.json";
                break;
            case F5C_EVENT_ALIGNMENT:
                rawFile = "f5c_event_align_arguments.json";
                break;
            default:
                System.err.println("Invalid Pipeline Step");
                break;
        }
        return buildArgumentsFromJson(rawFile);
    }

    private static ArrayList<Argument> buildArgumentsFromJson(String fileName) {
        ArrayList<Argument> arguments = new ArrayList<>();
        JsonObject argsJson = JSONFileHelper.rawtoJsonObject(fileName);
        JsonArray argsJsonArray = argsJson.getAsJsonArray("args");
        for (JsonElement element : argsJsonArray) {
            Argument argument = new Gson().fromJson(element, Argument.class);
            arguments.add(argument);
        }
        return arguments;
    }

    static void buildCommandString() {
        for (Step step : steps.values()) {
            step.buildCommandString();
        }
    }

    public static Step getNextStep() {
        // TODO Fix boundary conditions
        return steps.get(current++);
    }

    public static Step getPreviousStep() {
        // TODO Fix boundary conditions
        return steps.get(--current);
    }

    public static void reduceStepCount() {
        // TODO Fix boundary conditions
        // Since UI is navigated using a stack, we just need to reduce the count
        current--;
    }

    public static void resetSteps() {
        current = 0;
    }

    public static int getCurrentStepCount() {
        return current;
    }

    public static boolean isFinalStep() {
        return current == selectedPipelineSteps.size();
    }

    public static String[] getSelectedCommandStrings() {
        String[] commandArray = new String[steps.size()];
        int stepId = 0;
        for (Step step : steps.values()) {
            commandArray[stepId++] = step.getCommandString();
        }
        return commandArray;
    }

    static void createPipeline() {
        pipelineComponents = new ArrayList<>();
        for (Step step : steps.values()) {
            PipelineComponent pipelineComponent = new PipelineComponent(step.getStep(), step.getCommandString());
            pipelineComponents.add(pipelineComponent);
        }
    }

    public static ArrayList<PipelineComponent> getPipelineComponents() {
        return pipelineComponents;
    }
}
