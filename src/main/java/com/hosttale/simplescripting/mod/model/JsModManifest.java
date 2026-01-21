package com.hosttale.simplescripting.mod.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class JsModManifest {

    private final String id;
    private final String name;
    private final String version;
    private final String entrypoint;
    private final List<String> requiredAssetPacks;
    private final Set<String> permissions;
    private final String description;
    private final boolean preload;
    private final List<String> dependencies;

    @JsonCreator
    public JsModManifest(
            @JsonProperty(value = "id", required = true) String id,
            @JsonProperty(value = "name", required = true) String name,
            @JsonProperty(value = "version", required = true) String version,
            @JsonProperty("entrypoint") String entrypoint,
            @JsonProperty("requiredAssetPacks") List<String> requiredAssetPacks,
            @JsonProperty("permissions") Set<String> permissions,
            @JsonProperty("description") String description,
            @JsonProperty("preload") Boolean preload,
            @JsonProperty("dependencies") List<String> dependencies
    ) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.entrypoint = entrypoint;
        this.requiredAssetPacks = requiredAssetPacks == null ? Collections.emptyList() : List.copyOf(requiredAssetPacks);
        this.permissions = permissions == null ? Collections.emptySet() : Set.copyOf(permissions);
        this.description = description;
        this.preload = preload != null && preload;
        this.dependencies = dependencies == null ? Collections.emptyList() : List.copyOf(dependencies);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getEntrypointOrDefault() {
        return (entrypoint == null || entrypoint.isBlank()) ? "main.js" : entrypoint;
    }

    public List<String> getRequiredAssetPacks() {
        return requiredAssetPacks;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPreload() {
        return preload;
    }

    public List<String> getDependencies() {
        return dependencies;
    }
}
