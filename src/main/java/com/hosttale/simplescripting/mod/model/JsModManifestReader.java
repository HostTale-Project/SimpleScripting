package com.hosttale.simplescripting.mod.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsModManifestReader {

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .build();

    private JsModManifestReader() {
    }

    public static JsModManifest read(Path manifestPath) throws IOException {
        byte[] content = Files.readAllBytes(manifestPath);
        return OBJECT_MAPPER.readValue(content, JsModManifest.class);
    }
}
