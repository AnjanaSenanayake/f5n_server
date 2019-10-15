package com.mobilegenomics.f5n.controller;

import com.mobilegenomics.f5n.core.PipelineComponent;
import com.mobilegenomics.f5n.core.PipelineStep;
import com.mobilegenomics.f5n.core.Step;

import java.util.ArrayList;
import java.util.HashMap;

public class CoreController {

    private static ArrayList<PipelineStep> selectedPipelineSteps = new ArrayList<>();
    private static HashMap<String, Step> steps = new HashMap<>();
    private static ArrayList<PipelineComponent> pipelineComponents;

    private static int current = 0;

    static void addPipelineStep(PipelineStep step) {
        selectedPipelineSteps.add(step);
    }

    static HashMap<String, Step> getSteps() {
        return steps;
    }

    static void eraseSelectedPipeline() {
        selectedPipelineSteps.clear();
    }

    static void printList() {
        for (PipelineStep step : selectedPipelineSteps) {
            System.out.println("STEPS = " + step.toString());
        }
    }

    static void configureSteps() {
        steps = new HashMap<>();
        for (PipelineStep pipelineStep : selectedPipelineSteps) {
            Step step = new Step();
            step.setStep(pipelineStep);
            steps.put(pipelineStep.name(), step);
        }
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
