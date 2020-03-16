package com.mobilegenomics.f5n.core;

public enum PipelineStep {
    //    MINIMAP2_SEQUENCE_ALIGNMENT(0,
//        "minimap2 -x map-ont -a /mnt/sdcard/f5c/test/ecoli_2kb_region/draft.fa /mnt/sdcard/f5c/test/ecoli_2kb_region/reads.fasta"),
    MINIMAP2_SEQUENCE_ALIGNMENT(0, "minimap2 -x map-ont", "Minimap2 Sequence Alignment"),
    SAMTOOL_SORT(1, "samtool sort", "Samtool Sort"),
    SAMTOOL_INDEX(2, "samtool index", "Samtool Index"),
    F5C_INDEX(3, "f5c index", "f5c Index"),
    F5C_CALL_METHYLATION(4, "f5c call-methylation", "f5c Call Methylation"),
    F5C_EVENT_ALIGNMENT(5, "f5c eventalign", "f5c Event Alignment");

    private final int value;

    private final String command;

    private final String attribute;

    PipelineStep(int value, String command, String attribute) {
        this.value = value;
        this.command = command;
        this.attribute = attribute;
    }

    public int getValue() {
        return value;
    }

    public String getCommand() {
        return command;
    }

    public String getAttribute() {
        return attribute;
    }
}
