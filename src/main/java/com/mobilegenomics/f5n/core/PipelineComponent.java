package com.mobilegenomics.f5n.core;

import java.io.Serializable;

interface runNative {

    int run();
}

public class PipelineComponent implements Serializable {

    private static final long serialVersionUID = 0L;

    PipelineStep pipelineStep;

    private String command;

    public PipelineComponent(PipelineStep pipelineStep, String command) {
        this.pipelineStep = pipelineStep;
        this.command = command;
    }

//    @Override
//    public int run() {
//        int status;
//        if (this.pipelineStep.getValue() < 1) {
//            // minimap2
//            status = NativeCommands.getNativeInstance().initminimap2(command);
//        } else if (this.pipelineStep.getValue() < 3) {
//            // samtools
//            status = NativeCommands.getNativeInstance().initsamtool(command);
//        } else {
//            // f5c
//            status = NativeCommands.getNativeInstance().init(command);
//        }
//        return status;
//    }
}