import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class GradleWrapperBootstrap {
    private GradleWrapperBootstrap() {
    }

    public static void main(String[] args) throws Exception {
        Path projectRoot = Paths.get("").toAbsolutePath().normalize();
        Path propertiesPath = projectRoot.resolve("gradle/wrapper/gradle-wrapper.properties");
        if (!Files.exists(propertiesPath)) {
            throw new IllegalStateException("Missing gradle-wrapper.properties: " + propertiesPath);
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(propertiesPath)) {
            properties.load(input);
        }

        String distributionUrl = require(properties, "distributionUrl").replace("\\:", ":");
        String distributionFileName = distributionUrl.substring(distributionUrl.lastIndexOf('/') + 1);
        String gradleDirName = distributionFileName.replace("-bin.zip", "").replace("-all.zip", "");

        Path wrapperHome = projectRoot.resolve(".gradle-wrapper");
        Files.createDirectories(wrapperHome);
        Path zipPath = wrapperHome.resolve(distributionFileName);
        Path installRoot = wrapperHome.resolve(gradleDirName);
        Path gradleExecutable = installRoot.resolve(isWindows() ? "bin/gradle.bat" : "bin/gradle");

        if (!Files.exists(gradleExecutable)) {
            download(distributionUrl, zipPath);
            unzip(zipPath, wrapperHome);
        }

        if (!Files.exists(gradleExecutable)) {
            Path nested = findGradleExecutable(wrapperHome);
            if (nested != null) {
                gradleExecutable = nested;
            }
        }

        if (!Files.exists(gradleExecutable)) {
            throw new IllegalStateException("Gradle executable was not found after extraction.");
        }

        if (!isWindows()) {
            gradleExecutable.toFile().setExecutable(true);
        }

        List<String> command = new ArrayList<>();
        command.add(gradleExecutable.toAbsolutePath().toString());
        for (String arg : args) {
            command.add(arg);
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(projectRoot.toFile());
        builder.inheritIO();
        Process process = builder.start();
        System.exit(process.waitFor());
    }

    private static String require(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing property: " + key);
        }
        return value.trim();
    }

    private static void download(String url, Path zipPath) throws IOException, InterruptedException {
        if (Files.exists(zipPath) && Files.size(zipPath) > 0L) {
            return;
        }

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .GET()
                .build();

        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(zipPath));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Failed to download Gradle distribution. HTTP " + response.statusCode());
        }
    }

    private static void unzip(Path zipFile, Path targetDir) throws IOException {
        try (InputStream fileIn = Files.newInputStream(zipFile); ZipInputStream zipIn = new ZipInputStream(fileIn)) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                Path resolved = targetDir.resolve(entry.getName()).normalize();
                if (!resolved.startsWith(targetDir)) {
                    throw new IOException("Zip entry escapes target directory: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                } else {
                    Files.createDirectories(resolved.getParent());
                    try (OutputStream out = Files.newOutputStream(resolved)) {
                        zipIn.transferTo(out);
                    }
                    if (!isWindows() && resolved.toString().replace('\\', '/').contains("/bin/")) {
                        resolved.toFile().setExecutable(true);
                    }
                }
                zipIn.closeEntry();
            }
        }
    }

    private static Path findGradleExecutable(Path root) throws IOException {
        final String expected = isWindows() ? "gradle.bat" : "gradle";
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase(expected))
                    .filter(path -> path.toString().replace('\\', '/').contains("/bin/"))
                    .findFirst()
                    .orElse(null);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
