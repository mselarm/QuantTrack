package com.upv.quanttrack.data.llm;

import java.util.List;

public class LlmResponse {
    public List<Candidate> candidates;

    public static class Candidate {
        public Content content;
    }

    public static class Content {
        public List<Part> parts;
    }

    public static class Part {
        public String text;
    }

    // Método de utilidad para extraer el texto crudo esquivando la topología anidada
    public String getAnswer() {
        if (candidates != null && !candidates.isEmpty() &&
                candidates.get(0).content != null &&
                candidates.get(0).content.parts != null &&
                !candidates.get(0).content.parts.isEmpty()) {
            return candidates.get(0).content.parts.get(0).text;
        }
        return "Error: Respuesta vacía o malformada del LLM.";
    }
}
