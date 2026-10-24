package io.github.adytech99.healthindicators.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ToggleConfigStoreTest {
    @TempDir
    Path directory;

    @Test
    void missingFileUsesDefaultsWithoutCreatingAFileUntilSave() throws IOException {
        Path path = directory.resolve("nested/healthindicators.json");
        ToggleConfigStore store = new ToggleConfigStore(path);
        store.load();
        assertTrue(store.getHeartsRenderingEnabled());
        assertFalse(store.getArmorRenderingEnabled());
        assertFalse(store.getOverrideAllFiltersEnabled());
        assertFalse(Files.exists(path));
        store.save();
        assertTrue(Files.isRegularFile(path));
    }

    @Test
    void legacyPropertiesLoadAndPersistUnderTheirOriginalNames() throws IOException {
        Path path = write("{\"heartsRenderingEnabled\":false,\"armorRenderingEnabled\":true,\"overrideAllFiltersEnabled\":true}");
        ToggleConfigStore store = new ToggleConfigStore(path);
        store.load();
        assertFalse(store.getHeartsRenderingEnabled());
        assertTrue(store.getArmorRenderingEnabled());
        assertFalse(store.getOverrideAllFiltersEnabled());
        store.setHeartsRenderingEnabled(true);
        store.setArmorRenderingEnabled(false);
        store.save();
        JsonObject saved = json(path);
        assertEquals(3, saved.size());
        assertTrue(saved.get("heartsRenderingEnabled").getAsBoolean());
        assertFalse(saved.get("armorRenderingEnabled").getAsBoolean());
        assertFalse(saved.get("overrideAllFiltersEnabled").getAsBoolean());
        ToggleConfigStore reloaded = new ToggleConfigStore(path);
        reloaded.load();
        assertTrue(reloaded.getHeartsRenderingEnabled());
        assertFalse(reloaded.getArmorRenderingEnabled());
    }

    @Test
    void missingPropertiesReceiveDefaultsAndUnknownUnicodeFieldsSurviveSave() throws IOException {
        Path path = write("{\"armorRenderingEnabled\":true,\"future\":{\"label\":\"corazón ❤️ 日本語\",\"values\":[1,null,true]}}");
        JsonObject original = json(path);
        ToggleConfigStore store = new ToggleConfigStore(path);
        store.load();
        assertTrue(store.getHeartsRenderingEnabled());
        assertTrue(store.getArmorRenderingEnabled());
        store.save();
        assertEquals(original.get("future"), json(path).get("future"));
        assertTrue(Files.readString(path, StandardCharsets.UTF_8).contains("corazón ❤️ 日本語"));
    }

    @Test
    void temporaryOverrideDoesNotWriteAndIsNeverSavedAsTrue() throws IOException {
        Path path = write("{\"heartsRenderingEnabled\":false}");
        byte[] original = Files.readAllBytes(path);
        ToggleConfigStore store = new ToggleConfigStore(path);
        store.load();
        for (int tick = 0; tick < 40; tick++) {
            store.setOverrideAllFiltersEnabled(true);
        }
        assertTrue(store.getOverrideAllFiltersEnabled());
        assertArrayEquals(original, Files.readAllBytes(path));
        store.setArmorRenderingEnabled(true);
        store.save();
        assertTrue(store.getOverrideAllFiltersEnabled());
        assertFalse(json(path).get("overrideAllFiltersEnabled").getAsBoolean());
        store.load();
        assertFalse(store.getOverrideAllFiltersEnabled());
    }

    @Test
    void invalidDocumentsAreReportedAndNeverOverwritten() throws IOException {
        for (String invalid : new String[]{"{broken", "", "null", "[]", "{unquoted:true}", "{'single':true}", "{} {}",
                "{/*comment*/}", "{\"armorRenderingEnabled\":\"yes\"}",
                "{\"heartsRenderingEnabled\":null}", "{\"overrideAllFiltersEnabled\":1}"}) {
            Path path = write(invalid);
            byte[] original = Files.readAllBytes(path);
            ToggleConfigStore store = new ToggleConfigStore(path);
            assertThrows(IOException.class, store::load, invalid);
            store.setArmorRenderingEnabled(true);
            assertThrows(IOException.class, store::save, invalid);
            assertArrayEquals(original, Files.readAllBytes(path), invalid);
        }
    }

    @Test
    void newlyCorruptedFileCannotBeOverwrittenByAPreviouslyLoadedStore() throws IOException {
        Path path = write("{}");
        ToggleConfigStore store = new ToggleConfigStore(path);
        store.load();
        Files.writeString(path, "{incomplete", StandardCharsets.UTF_8);
        store.setHeartsRenderingEnabled(false);
        assertThrows(IOException.class, store::save);
        assertEquals("{incomplete", Files.readString(path, StandardCharsets.UTF_8));
    }

    @Test
    void preservesUnknownPropertiesAddedAfterLoadingAndCleansTemporaryFiles() throws IOException {
        Path path = write("{}");
        ToggleConfigStore store = new ToggleConfigStore(path);
        store.load();
        Files.writeString(path, "{\"external\":42}", StandardCharsets.UTF_8);
        store.save();
        assertEquals(42, json(path).get("external").getAsInt());
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }

    private Path write(String json) throws IOException {
        Path path = directory.resolve("healthindicators.json");
        Files.writeString(path, json, StandardCharsets.UTF_8);
        return path;
    }

    private static JsonObject json(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
