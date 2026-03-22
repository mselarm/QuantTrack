package com.upv.quanttrack.data;

public class QuantTerm {
    private final String term;
    private final String definition;

    public QuantTerm(String term, String definition) {
        this.term = term;
        this.definition = definition;
    }

    public String getTerm() { return term; }
    public String getDefinition() { return definition; }
}
