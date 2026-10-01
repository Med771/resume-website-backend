package ru.ai.sin.helper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.config.FileConfig;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class FileHelperTest {

    @Mock
    private FileConfig fileConfig;

    private FileHelper fileHelper;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileHelper = new FileHelper(fileConfig);
        lenient().when(fileConfig.getFilePath()).thenReturn(tempDir);
    }

    @Test
    void getFileContent_rejectsPathOutsideBase() {
        assertThatThrownBy(() -> fileHelper.getFileContent("../secret.txt"))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void getFileContent_readsFileInsideBase() throws Exception {
        Files.writeString(tempDir.resolve("allowed.txt"), "x");
        byte[] data = fileHelper.getFileContent("allowed.txt");
        org.assertj.core.api.Assertions.assertThat(data).asString().isEqualTo("x");
    }

    @Test
    void getFileContent_rejectsNullOrBlank() {
        assertThatThrownBy(() -> fileHelper.getFileContent(null))
                .isInstanceOf(FileNotFoundException.class);
        assertThatThrownBy(() -> fileHelper.getFileContent("  "))
                .isInstanceOf(FileNotFoundException.class);
    }
}
