package ru.miem.depgraph;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Main {

    private static final String PACKAGE_ARG = "--package";
    private static final String REPO_ARG = "--repo";
    private static final String MODE_ARG = "--mode";
    private static final String VERSION_ARG = "--version";
    private static final String OUTPUT_ARG = "--output";
    private static final String DEPTH_ARG = "--depth";

    private static final Set<String> KNOWN_OPTIONS = Set.of(
            PACKAGE_ARG, REPO_ARG, MODE_ARG, VERSION_ARG, OUTPUT_ARG, DEPTH_ARG
    );

    private static final String MODE_LOCAL = "local";
    private static final String MODE_REMOTE = "remote";
    private static final String MODE_TEST = "test";
    private static final Set<String> ALLOWED_MODES = Set.of(MODE_LOCAL, MODE_REMOTE, MODE_TEST);

    private static final String DEFAULT_MODE = MODE_LOCAL;
    private static final String DEFAULT_VERSION = "latest";
    private static final String DEFAULT_OUTPUT = "graph.png";
    private static final String DEFAULT_DEPTH = "3";

    private static final int MIN_DEPTH = 1;
    private static final int MAX_DEPTH = 100;
    private static final int PREFIX_LENGTH = 2;

    private static final String[] ALLOWED_EXTENSIONS = {".png", ".svg", ".dot"};

    public static void main(String[] args) {
        try {
            AppConfig config = parseArgs(args);
            printConfig(config);
            processDependencies(config);
        } catch (IllegalArgumentException e) {
            System.err.println("Ошибка: " + e.getMessage());
            System.err.println();
            printUsage();
            System.exit(1);
        }
    }

    private static void processDependencies(AppConfig config) {
        Map<String, List<String>> graph;
        if (MODE_TEST.equals(config.mode())) {
            String data = readFile(config.repo());
            graph = parseTestRepo(data);
        } else {
            String data = readRepository(config.repo(), config.mode());
            graph = parseApkIndex(data);
        }

        String startNode = config.packageName();
        if (!graph.containsKey(startNode)) {
            throw new IllegalArgumentException("Пакет " + startNode + " не найден в репозитории");
        }

        System.out.println("=== Граф зависимостей для " + startNode + " ===");
        dfs(startNode, 0, config.depth(), graph, new ArrayList<>());
        System.out.println("=======================================");
    }

    private static void dfs(String node, int depth, int maxDepth,
                            Map<String, List<String>> graph, List<String> path) {
        if (depth > maxDepth) {
            return;
        }
        if (path.contains(node)) {
            String indent = "  ".repeat(depth);
            String cyclePath = String.join(" -> ", path) + " -> " + node;
            System.out.println(indent + "[Цикл] " + node + " (путь: " + cyclePath + ")");
            return;
        }

        String indent = "  ".repeat(depth);
        System.out.println(indent + node);

        path.add(node);
        List<String> deps = graph.getOrDefault(node, Collections.emptyList());
        for (String dep : deps) {
            dfs(dep, depth + 1, maxDepth, graph, path);
        }
        path.remove(path.size() - 1);
    }

    private static Map<String, List<String>> parseTestRepo(String data) {
        Map<String, List<String>> graph = new HashMap<>();
        for (String line : data.split("\\r?\\n")) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(":");
            if (parts.length < 2) {
                continue;
            }
            String pkg = parts[0].trim();
            List<String> deps = new ArrayList<>();
            for (String d : parts[1].trim().split("\\s+")) {
                if (!d.isEmpty()) {
                    deps.add(d);
                }
            }
            graph.put(pkg, deps);
        }
        return graph;
    }

    private static Map<String, List<String>> parseApkIndex(String data) {
        Map<String, List<String>> graph = new HashMap<>();
        String[] blocks = data.split("\\r?\\n\\r?\\n");
        for (String block : blocks) {
            String pkgName = null;
            String deps = null;
            for (String line : block.split("\\r?\\n")) {
                if (line.startsWith("P:")) {
                    pkgName = line.substring(PREFIX_LENGTH).trim();
                }
                if (line.startsWith("D:")) {
                    deps = line.substring(PREFIX_LENGTH).trim();
                }
            }
            if (pkgName != null) {
                List<String> depList = new ArrayList<>();
                if (deps != null) {
                    for (String d : deps.split(",")) {
                        depList.add(d.trim());
                    }
                }
                graph.put(pkgName, depList);
            }
        }
        return graph;
    }

    private static String readFile(String filePath) {
        try {
            return Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Ошибка чтения файла: " + e.getMessage());
        }
    }

    private static String readRepository(String repoPath, String mode) {
        if (MODE_LOCAL.equals(mode)) {
            return readFile(repoPath);
        } else {
            return fetchRemoteData(repoPath);
        }
    }

    private static String fetchRemoteData(String urlString) {
        try {
            URL url = URI.create(urlString).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            try (InputStream is = connection.getInputStream()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Ошибка загрузки по URL: " + e.getMessage());
        }
    }

    private static AppConfig parseArgs(String[] args) {
        Map<String, String> values = collectArgs(args);

        String packageName = values.get(PACKAGE_ARG);
        validatePackage(packageName);

        String mode = values.getOrDefault(MODE_ARG, DEFAULT_MODE);
        validateMode(mode);

        String repo = values.get(REPO_ARG);
        validateRepo(repo, mode);

        String version = values.getOrDefault(VERSION_ARG, DEFAULT_VERSION);
        String output = values.getOrDefault(OUTPUT_ARG, DEFAULT_OUTPUT);
        validateOutput(output);

        int depth = parseDepth(values.getOrDefault(DEPTH_ARG, DEFAULT_DEPTH));

        return new AppConfig(packageName, repo, mode, version, output, depth);
    }

    private static Map<String, String> collectArgs(String[] args) {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            String option = args[i];
            if (!KNOWN_OPTIONS.contains(option)) {
                throw new IllegalArgumentException("Неизвестный параметр: " + option);
            }
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("Не задано значение для параметра: " + option);
            }
            result.put(option, args[i + 1]);
        }
        return result;
    }

    private static void validatePackage(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Не задано имя анализируемого пакета (" + PACKAGE_ARG + ")");
        }
        if (!value.matches("[a-zA-Z0-9_.-]+")) {
            throw new IllegalArgumentException("Недопустимое имя пакета: " + value);
        }
    }

    private static void validateMode(String value) {
        if (!ALLOWED_MODES.contains(value)) {
            throw new IllegalArgumentException(
                    "Недопустимый режим: " + value + ". Допустимые: " + ALLOWED_MODES);
        }
    }

    private static void validateRepo(String value, String mode) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Не задан путь к репозиторию (" + REPO_ARG + ")");
        }
        if (MODE_LOCAL.equals(mode) || MODE_TEST.equals(mode)) {
            Path path = Path.of(value);
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("Файл не найден: " + value);
            }
        } else if (!value.startsWith("http://") && !value.startsWith("https://")) {
            throw new IllegalArgumentException(
                    "URL репозитория должен начинаться с http:// или https://: " + value);
        }
    }

    private static void validateOutput(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Не задано имя выходного файла (" + OUTPUT_ARG + ")");
        }
        boolean valid = false;
        for (String ext : ALLOWED_EXTENSIONS) {
            if (value.endsWith(ext)) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new IllegalArgumentException(
                    "Расширение файла графа должно быть .png, .svg или .dot: " + value);
        }
    }

    private static int parseDepth(String value) {
        int depth;
        try {
            depth = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Глубина должна быть целым числом: " + value);
        }
        if (depth < MIN_DEPTH || depth > MAX_DEPTH) {
            throw new IllegalArgumentException(
                    "Глубина должна быть в диапазоне " + MIN_DEPTH + ".." + MAX_DEPTH + ": " + depth);
        }
        return depth;
    }

    private static void printConfig(AppConfig config) {
        System.out.println("=== Параметры запуска ===");
        System.out.println("package = " + config.packageName());
        System.out.println("repo    = " + config.repo());
        System.out.println("mode    = " + config.mode());
        System.out.println("version = " + config.version());
        System.out.println("output  = " + config.output());
        System.out.println("depth   = " + config.depth());
        System.out.println("=========================");
    }

    private static void printUsage() {
        System.out.println("Использование:");
        System.out.println("  java -cp out ru.miem.depgraph.Main [опции]");
        System.out.println();
        System.out.println("Опции:");
        System.out.println("  " + PACKAGE_ARG + " <имя>       имя анализируемого пакета (обязательно)");
        System.out.println("  " + REPO_ARG + " <путь|URL>     путь к файлу репозитория или URL (обязательно)");
        System.out.println("  " + MODE_ARG + " <режим>       local, remote или test (по умолчанию: local)");
        System.out.println("  " + VERSION_ARG + " <версия>    версия пакета (по умолчанию: latest)");
        System.out.println("  " + OUTPUT_ARG + " <файл>       имя файла графа .png/.svg/.dot " +
                "(по умолчанию: graph.png)");
        System.out.println("  " + DEPTH_ARG + " <число>       максимальная глубина анализа 1..100 " +
                "(по умолчанию: 3)");
    }

    private record AppConfig(
            String packageName,
            String repo,
            String mode,
            String version,
            String output,
            int depth
    ) {
    }
}