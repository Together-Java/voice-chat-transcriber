package org.togetherjava.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import tools.jackson.databind.ObjectMapper;

public class ConfigLoader {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static Config loadConfig(Path path) throws IOException {
        return OBJECT_MAPPER.readValue(Files.readAllBytes(path), Config.class);
    }
}
