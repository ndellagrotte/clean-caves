package com.personthecat.cleancaves.io;

import com.personthecat.cleancaves.CaveInit;
import com.personthecat.cleancaves.util.Result;

import javax.annotation.CheckReturnValue;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.personthecat.cleancaves.util.CommonMethods.runEx;
import static com.personthecat.cleancaves.util.CommonMethods.runExF;

/**
 * File operations that report failure through {@link Result} instead of throwing, so that callers
 * are prompted -- via {@code @CheckReturnValue} -- to decide what a failure means.
 */
public final class SafeFileIO {

    private SafeFileIO() {}

    /** Ensures that the input path refers to a directory, creating one if nothing is found. */
    @CheckReturnValue
    public static Result<IOException> ensureDirExists(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (final IOException e) {
            return Result.of(e);
        }
        return Result.ok();
    }

    /** Creates a directory and all of its parents, without testing first. */
    @CheckReturnValue
    public static Result<IOException> mkdirs(Path dir) {
        return ensureDirExists(dir);
    }

    /** Checks whether the given path exists, neatly throwing {@code error} if it cannot be read. */
    public static boolean fileExists(Path file, String error) {
        try {
            return Files.exists(file);
        } catch (final SecurityException e) {
            throw runEx(error);
        }
    }

    /** Copies a file into the specified directory, keeping its name. */
    @CheckReturnValue
    public static Result<IOException> copy(Path file, Path toDir) {
        try {
            Files.copy(file, toDir.resolve(file.getFileName().toString()));
        } catch (final IOException e) {
            return Result.of(e);
        }
        return Result.ok();
    }

    /** Copies a file to the backup directory. */
    public static void backup(Path file) {
        backup(file, false);
    }

    /** Copies (or moves) a file to the backup directory, rotating any existing backups. */
    public static void backup(Path file, boolean move) {
        final Path backupDir = CaveInit.backupDir();
        if (!fileExists(backupDir, "Unable to handle backup directory.")) {
            mkdirs(backupDir).throwIfPresent();
        }
        final String name = file.getFileName().toString();
        final Path backup = backupDir.resolve(name);
        new BackupHelper(name).cycle(backupDir);
        if (fileExists(backup, "Unable to handle existing backup file.")) {
            throw runExF("Could not rename backups: {}", name);
        }
        try {
            if (move) {
                Files.move(file, backup);
            } else {
                Files.copy(file, backup, StandardCopyOption.COPY_ATTRIBUTES);
            }
        } catch (final IOException e) {
            throw runExF("Error {} {} to backups", move ? "moving" : "copying", name);
        }
    }

    /** Renames a file when given a top-level name only. */
    public static void rename(Path file, String name) {
        final Path renamed = file.resolveSibling(name);
        try {
            Files.move(file, renamed);
        } catch (final IOException e) {
            throw runExF("Cannot rename: {}", renamed);
        }
    }

    /** Lists the immediate contents of a directory. Empty when the path is not a readable dir. */
    @CheckReturnValue
    public static Optional<List<Path>> listFiles(Path dir) {
        return listFiles(dir, f -> true);
    }

    @CheckReturnValue
    public static Optional<List<Path>> listFiles(Path dir, Predicate<Path> filter) {
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }
        try (final Stream<Path> contents = Files.list(dir)) {
            return Optional.of(contents.filter(filter).sorted().toList());
        } catch (final IOException | UncheckedIOException ignored) {
            return Optional.empty();
        }
    }

    /** Depth-first search for the first file matching the given filter. */
    @CheckReturnValue
    public static Optional<Path> getFileRecursive(Path dir, Predicate<Path> filter) {
        for (final Path f : listFiles(dir).orElse(List.of())) {
            if (Files.isDirectory(f)) {
                final Optional<Path> found = getFileRecursive(f, filter);
                if (found.isPresent()) {
                    return found;
                }
            } else if (filter.test(f)) {
                return Optional.of(f);
            }
        }
        return Optional.empty();
    }

    /** Attempts to retrieve the contents of the input file. */
    public static Optional<List<String>> safeContents(Path file) {
        try {
            return Optional.of(Files.readAllLines(file));
        } catch (final IOException ignored) {
            return Optional.empty();
        }
    }

    /** Retrieves an asset from the jar file. */
    @CheckReturnValue
    public static Optional<InputStream> getResource(String path) {
        final String absolute = path.startsWith("/") ? path : "/" + path;
        return Optional.ofNullable(SafeFileIO.class.getResourceAsStream(absolute));
    }

    /** Retrieves an asset from the jar file, failing loudly when it is absent. */
    public static InputStream getRequiredResource(String path) {
        return getResource(path)
            .orElseThrow(() -> runExF("The required file \"{}\" was not present in the jar.", path));
    }

    /**
     * Renumbers existing backups so that the newest can take the un-suffixed name:
     * {@code name.cave -> name (1).cave -> name (2).cave}, stopping at the first gap.
     */
    private static final class BackupHelper {
        private final String base;
        private final String ext;
        private final Pattern pattern;

        BackupHelper(String name) {
            final int dotIndex = name.indexOf('.');
            if (dotIndex > 0) {
                this.base = name.substring(0, dotIndex);
                this.ext = name.substring(dotIndex);
            } else {
                this.base = name;
                this.ext = "";
            }
            this.pattern = Pattern.compile(Pattern.quote(this.base) + "(\\s\\((\\d+)\\))?" + Pattern.quote(this.ext));
        }

        void cycle(Path backupDir) {
            final List<Path> matching =
                new ArrayList<>(listFiles(backupDir, this::matches).orElse(List.of()));
            matching.sort(Comparator.comparingInt(this::getNumber));
            final int end = this.getFirstGap(matching);
            for (int i = end - 1; i >= 0; i--) {
                final Path f = matching.get(i);
                final Path renamed = f.resolveSibling(this.base + " (" + (i + 1) + ")" + this.ext);
                try {
                    Files.move(f, renamed);
                } catch (final IOException e) {
                    throw runExF("Could not increment backup: {}", f.getFileName());
                }
            }
        }

        boolean matches(Path file) {
            return this.pattern.matcher(file.getFileName().toString()).matches();
        }

        int getNumber(Path file) {
            final String name = file.getFileName().toString();
            final Matcher matcher = this.pattern.matcher(name);
            if (!matcher.find()) {
                throw runExF("Backup deleted externally: {}", name);
            }
            final String group = matcher.group(2);
            return group == null ? 0 : Integer.parseInt(group);
        }

        int getFirstGap(List<Path> files) {
            int lastNum = 0;
            for (final Path f : files) {
                final int num = this.getNumber(f) + 1;
                if (num - lastNum > 1) {
                    return lastNum;
                }
                lastNum = num;
            }
            return files.size();
        }
    }
}
