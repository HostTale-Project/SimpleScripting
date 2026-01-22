package com.hosttale.simplescripting.mod;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

public class JsModValidationException extends Exception {

    private final Path manifestPath;
    private final List<String> errors;

    public JsModValidationException(Path manifestPath, List<String> errors) {
        super("Invalid mod manifest at " + manifestPath + ": " + String.join("; ", errors));
        this.manifestPath = manifestPath;
        this.errors = Collections.unmodifiableList(errors);
    }

    public Path getManifestPath() {
        return manifestPath;
    }

    public List<String> getErrors() {
        return errors;
    }
}
