package com.mobilegenomics.genopo.core;

import java.io.Serializable;
import java.util.ArrayList;

public class Step implements Serializable {

    private ArrayList<Argument> arguments;

    private PipelineStep stepName;

    private StringBuilder commandBuilder;

    private String command;

    public Step(final PipelineStep stepName, final ArrayList<Argument> arguments) {
        this.stepName = stepName;
        this.arguments = arguments;
        buildCommandString();
    }

    public PipelineStep getStep() {
        return stepName;
    }

    public ArrayList<Argument> getArguments() {
        return arguments;
    }

    public void buildCommandString() {
        commandBuilder = new StringBuilder(stepName.getCommand());
        for (Argument argument : arguments) {
            commandBuilder.append(" ");
            commandBuilder.append(argument.toString());
        }
        command = commandBuilder.toString();
    }

    private boolean isEqualPresent(String flag) {
        if(flag.substring(flag.length()-1).equals("="))
            return true;
        else
            return false;
    }

    public String getCommandString() {
        return command;
    }

    public void setCommandString(final String command) {
        this.command = command;
    }

}
