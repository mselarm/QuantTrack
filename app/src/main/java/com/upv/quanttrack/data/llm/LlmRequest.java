package com.upv.quanttrack.data.llm;

import java.util.ArrayList;
import java.util.List;

public class LlmRequest {
    private List<Content> contents = new ArrayList<>();

    public LlmRequest(String promptText) {
        Content content = new Content();
        content.parts.add(new Part(promptText));
        this.contents.add(content);
    }

    private static class Content {
        List<Part> parts = new ArrayList<>();
    }

    private static class Part {
        String text;
        Part(String text) { this.text = text; }
    }
}
