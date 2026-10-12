package ru.miem.depgraph;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
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
    private static final Set<String> ALLOWED_MODES = Set.of(MODE_LOCAL, MODE_REMOTE);

    private static final String DEFAULT_MODE = MODE_LOCAL;
    private static final String DEFAULT_VERSION = "latest";
    private static final String DEFAULT_OUTPUT = "graph.png";
    private static final String DEFAULT_DEPTH = "3";

    private static final int MIN_DEPTH = 1;
    private static final int MAX_DEPTH = 100;

    private static final String[] ALLOWED_EXTENSIONS = {".png", ".svg", ".dot"};

    public static void main(String[] args) {
        try {
            AppConfig config = parseArgs(args);
            printConfig(config);
        } catch (IllegalArgumentException e) {
            System.err.println("Ошибка: " + e.getMessage());
            System.err.println();
            printUsage();
            System.exit(1);
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
        if (MODE_LOCAL.equals(mode)) {
            Path path = Path.of(value);
            if (!Files.exists(path)) {
                throw new IllegalArgumentException("Локальный репозиторий не найден: " + value);
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
        System.out.println("  " + MODE_ARG + " <local|remote> режим работы (по умолчанию: local)");
        System.out.println("  " + VERSION_ARG + " <версия>    версия пакета (по умолчанию: latest)");
        System.out.println("  " + OUTPUT_ARG + " <файл>       имя файла графа .png/.svg/.dot (по умолчанию: graph.png)");
        System.out.println("  " + DEPTH_ARG + " <число>       максимальная глубина анализа 1..100 (по умолчанию: 3)");
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