package io.github.adytech99.healthindicators.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** File storage for the legacy toggle configuration, without Minecraft or platform dependencies. */
public final class ToggleConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String HEARTS = "heartsRenderingEnabled";
    private static final String ARMOR = "armorRenderingEnabled";
    private static final String OVERRIDE = "overrideAllFiltersEnabled";

    private final Path path;
    private boolean heartsRenderingEnabled = true;
    private boolean armorRenderingEnabled;
    private boolean overrideAllFiltersEnabled;

    public ToggleConfigStore(Path path) {
        this.path = path.toAbsolutePath();
    }

    public synchronized boolean getHeartsRenderingEnabled() {
        return heartsRenderingEnabled;
    }

    public synchronized void setHeartsRenderingEnabled(boolean enabled) {
        heartsRenderingEnabled = enabled;
    }

    public synchronized boolean getArmorRenderingEnabled() {
        return armorRenderingEnabled;
    }

    public synchronized void setArmorRenderingEnabled(boolean enabled) {
        armorRenderingEnabled = enabled;
    }

    public synchronized boolean getOverrideAllFiltersEnabled() {
        return overrideAllFiltersEnabled;
    }

    public synchronized void setOverrideAllFiltersEnabled(boolean enabled) {
        // The key is retained in saved JSON for older versions, but a held key is session state.
        overrideAllFiltersEnabled = enabled;
    }

    public synchronized void load() throws IOException {
        overrideAllFiltersEnabled = false;
        JsonObject document = readDocument();
        // Apply only after the whole document has been validated.
        heartsRenderingEnabled = booleanValue(document, HEARTS, true);
        armorRenderingEnabled = booleanValue(document, ARMOR, false);
    }

    public synchronized void save() throws IOException {
        // Re-read before every save. Preserve unknown fields and refuse to overwrite a file
        // that has become invalid since startup, including one edited by another program.
        JsonObject document = readDocument();
        document.addProperty(HEARTS, heartsRenderingEnabled);
        document.addProperty(ARMOR, armorRenderingEnabled);
        document.addProperty(OVERRIDE, false);

        Path parent = path.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "healthindicators-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                GSON.toJson(document, writer);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private JsonObject readDocument() throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonReader jsonReader = new JsonReader(reader);
            jsonReader.setLenient(false);
            // Read through the public tree adapter: JsonParser enables lenient parsing even
            // when its JsonReader was strict, which could silently rewrite malformed files.
            JsonElement parsed = GSON.getAdapter(JsonElement.class).read(jsonReader);
            if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
                throw new IOException("Unexpected content after the JSON object in " + path);
            }
            if (!parsed.isJsonObject()) {
                throw new IOException("Expected a JSON object in " + path);
            }
            JsonObject document = parsed.getAsJsonObject();
            booleanValue(document, HEARTS, true);
            booleanValue(document, ARMOR, false);
            booleanValue(document, OVERRIDE, false);
            return document;
        } catch (NoSuchFileException e) {
            return new JsonObject();
        } catch (JsonParseException | IllegalStateException e) {
            throw new IOException("Invalid JSON in " + path, e);
        }
    }

    private static boolean booleanValue(JsonObject document, String key, boolean fallback) throws IOException {
        if (!document.has(key)) {
            return fallback;
        }
        JsonElement value = document.get(key);
        if (value instanceof JsonPrimitive primitive && primitive.isBoolean()) {
            return primitive.getAsBoolean();
        }
        throw new IOException("Expected a boolean for " + key);
    }
}
