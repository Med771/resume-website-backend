package ru.ai.sin.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.ai.sin.config.FileConfig;
import ru.ai.sin.exception.models.BadRequestException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileHelper {

    private static final int SIGNATURE_READ_BYTES = 32;

    /** Расширения, которые принимаем по имени файла, если браузер не прислал корректный MIME. */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".jpe", ".jfif", ".pjpeg", ".pjp",
            ".png",
            ".gif",
            ".webp",
            ".bmp",
            ".heic", ".heif",
            ".avif",
            ".tif", ".tiff"
    );

    /** Допустимые MIME (включая нестандартные варианты для JPEG). */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/pjpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp",
            "image/x-ms-bmp",
            "image/heic",
            "image/heif",
            "image/avif",
            "image/tiff",
            "image/x-tiff",
            "application/octet-stream"
    );

    private static final Map<String, String> MIME_TO_EXTENSION = Map.ofEntries(
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/jpg", ".jpg"),
            Map.entry("image/pjpeg", ".jpg"),
            Map.entry("image/png", ".png"),
            Map.entry("image/gif", ".gif"),
            Map.entry("image/webp", ".webp"),
            Map.entry("image/bmp", ".bmp"),
            Map.entry("image/x-ms-bmp", ".bmp"),
            Map.entry("image/heic", ".heic"),
            Map.entry("image/heif", ".heif"),
            Map.entry("image/avif", ".avif"),
            Map.entry("image/tiff", ".tiff"),
            Map.entry("image/x-tiff", ".tiff")
    );

    private final FileConfig fileConfig;

    public String saveFile(MultipartFile file, String fileName) {
        Path path = fileConfig.getFilePath();

        String extension;
        try {
            byte[] header = readHeader(file);
            extension = resolveExtension(file, header).orElseThrow(
                    () -> new BadRequestException("Unsupported or unrecognized image format")
            );
        } catch (IOException e) {
            log.error("Failed to read upload for saving", e);
            throw new BadRequestException("Failed to read uploaded file");
        }

        String finalFileName = fileName + extension;
        Path filePath = path.resolve(finalFileName);

        try {
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return finalFileName;
        } catch (IOException e) {
            log.error("Failed to save file {} to path {}", finalFileName, filePath, e);
        }

        return null;
    }

    public String uploadImage(MultipartFile file, String baseName) {
        validateMultipart(file);
        return saveFile(file, baseName);
    }

    public List<StoredFileInfo> listImageFiles() {
        Path base = fileConfig.getFilePath().toAbsolutePath().normalize();
        try (Stream<Path> stream = Files.list(base)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> isAllowedImageFileName(path.getFileName().toString()))
                    .map(this::toStoredFileInfo)
                    .sorted(Comparator.comparing(StoredFileInfo::fileName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (IOException e) {
            log.error("Failed to list storage directory {}", base, e);
            throw new BadRequestException("Failed to list storage files");
        }
    }

    public void deleteFile(String fileName) throws FileNotFoundException {
        Path resolved = resolveSafePath(fileName);
        if (!Files.exists(resolved)) {
            throw new FileNotFoundException(fileName);
        }
        try {
            Files.delete(resolved);
        } catch (IOException e) {
            log.error("Failed to delete file {}", fileName, e);
            throw new BadRequestException("Failed to delete file");
        }
    }

    public byte[] getFileContent(String fileName) throws FileNotFoundException {
        if (fileName == null || fileName.isBlank()) {
            throw new FileNotFoundException("empty");
        }
        Path filePath = resolveSafePath(fileName);

        if (!Files.exists(filePath)) {
            log.warn("File {} does not exist", fileName);
            throw new FileNotFoundException(fileName);
        }

        try (InputStream is = Files.newInputStream(filePath)) {
            return is.readAllBytes();
        } catch (IOException e) {
            log.error("Failed to read file {}", fileName, e);
            throw new UncheckedIOException(e);
        }
    }

    private Path resolveSafePath(String fileName) throws FileNotFoundException {
        Path base = fileConfig.getFilePath().toAbsolutePath().normalize();
        Path resolved = base.resolve(fileName).normalize();
        if (!resolved.startsWith(base)) {
            log.warn("Rejected path outside storage: {}", fileName);
            throw new FileNotFoundException(fileName);
        }
        return resolved;
    }

    private StoredFileInfo toStoredFileInfo(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            String fileName = path.getFileName().toString();
            return new StoredFileInfo(fileName, attrs.size(), attrs.lastModifiedTime().toInstant());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean isAllowedImageFileName(String fileName) {
        return extensionFromFilename(fileName)
                .map(ALLOWED_EXTENSIONS::contains)
                .orElse(false);
    }

    public record StoredFileInfo(String fileName, long sizeBytes, Instant lastModified) {
    }

    public void validateMultipart(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }

        if (multipartFile.getSize() > fileConfig.getMaxUploadSizeBytes()) {
            throw new BadRequestException("Image file is too large");
        }

        byte[] header;
        try {
            header = readHeader(multipartFile);
        } catch (IOException e) {
            log.warn("Could not read upload header: {}", e.getMessage());
            throw new BadRequestException("Failed to read uploaded file");
        }

        if (header.length < 3) {
            throw new BadRequestException("Image file is too small or corrupted");
        }

        Optional<String> fromSignature = detectExtensionFromSignature(header, header.length);
        if (fromSignature.isPresent()) {
            return;
        }

        String rawMime = multipartFile.getContentType();
        String mime = rawMime == null ? "" : rawMime.trim().toLowerCase(Locale.ROOT);
        if (!mime.isEmpty() && !ALLOWED_MIME_TYPES.contains(mime)) {
            throw new BadRequestException(
                    "File must be an image (JPEG, PNG, GIF, WebP, BMP, HEIC, AVIF or TIFF). Got content type: "
                            + rawMime);
        }

        Optional<String> extOpt = extensionFromFilename(multipartFile.getOriginalFilename());
        if (extOpt.isPresent() && ALLOWED_EXTENSIONS.contains(extOpt.get())) {
            return;
        }

        throw new BadRequestException(
                "File must be a supported image format. "
                        + "If the file is JPG/PNG/etc., ensure the file is not corrupted and try again.");
    }

    private static byte[] readHeader(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            return in.readNBytes(FileHelper.SIGNATURE_READ_BYTES);
        }
    }

    /**
     * Расширение для сохранения: приоритет сигнатура → MIME → имя файла.
     */
    private static Optional<String> resolveExtension(MultipartFile file, byte[] header) {
        Optional<String> sig = detectExtensionFromSignature(header, header.length);
        if (sig.isPresent()) {
            return sig;
        }

        String mime = file.getContentType();
        if (mime != null) {
            String key = mime.trim().toLowerCase(Locale.ROOT);
            if (MIME_TO_EXTENSION.containsKey(key)) {
                return Optional.of(MIME_TO_EXTENSION.get(key));
            }
        }

        return extensionFromFilename(file.getOriginalFilename());
    }

    private static Optional<String> extensionFromFilename(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return Optional.empty();
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        if (ALLOWED_EXTENSIONS.contains(ext)) {
            return Optional.of(ext);
        }
        return Optional.empty();
    }

    private static Optional<String> detectExtensionFromSignature(byte[] b, int len) {
        if (len >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Optional.of(".jpg");
        }

        if (len >= 8
                && b[0] == (byte) 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return Optional.of(".png");
        }

        if (len >= 6 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8'
                && (b[4] == '7' || b[4] == '9') && b[5] == 'a') {
            return Optional.of(".gif");
        }

        if (len >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return Optional.of(".webp");
        }

        if (len >= 2 && b[0] == 'B' && b[1] == 'M') {
            return Optional.of(".bmp");
        }

        if (len >= 12 && b[4] == 'f' && b[5] == 't' && b[6] == 'y' && b[7] == 'p') {
            String brand = new String(b, 8, Math.min(4, len - 8), StandardCharsets.US_ASCII);
            if (brand.startsWith("heic") || brand.startsWith("heix")
                    || brand.startsWith("mif1") || brand.startsWith("msf1")) {
                return Optional.of(".heic");
            }
            if (brand.startsWith("avif")) {
                return Optional.of(".avif");
            }
        }

        if (len >= 4) {
            boolean leTiff = b[0] == 'I' && b[1] == 'I' && b[2] == '*' && b[3] == 0;
            boolean beTiff = b[0] == 'M' && b[1] == 'M' && b[2] == 0 && b[3] == '*';
            if (leTiff || beTiff) {
                return Optional.of(".tiff");
            }
        }

        return Optional.empty();
    }
}
