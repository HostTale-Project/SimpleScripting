package com.hosttale.simplescripting.mod;

import com.hosttale.simplescripting.mod.model.JsModManifest;

import java.nio.file.Path;

public final class JsModDefinition {

    private final Path rootDirectory;
    private final JsModManifest manifest;
    private final Path entrypoint;

    public JsModDefinition(Path rootDirectory, JsModManifest manifest) {
        this.rootDirectory = rootDirectory;
        this.manifest = manifest;
        this.entrypoint = rootDirectory.resolve(manifest.getEntrypointOrDefault());
    }

    public Path getRootDirectory() {
        return rootDirectory;
    }

    public JsModManifest getManifest() {
        return manifest;
    }

    public Path getEntrypoint() {
        return entrypoint;
    }
}
