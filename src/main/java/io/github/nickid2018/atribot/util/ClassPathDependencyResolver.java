package io.github.nickid2018.atribot.util;

import java.io.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ClassPathDependencyResolver {

    public static final File LIBRARY_PATH;

    static {
        String libraryPath = System.getenv("LIBRARY_PATH");
        if (libraryPath == null)
            libraryPath = System.getProperty("atribot.libraryPath");
        if (libraryPath == null)
            libraryPath = "libraries";
        LIBRARY_PATH = new File(libraryPath);
    }

    public static boolean inProductionEnvironment(Class<?> thisClass) {
        return thisClass
            .getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath()
            .endsWith(".jar");
    }

    public static void resolveCoreDependencies() throws Throwable {
        System.out.println("Loading dependencies for core classes...");

        String jarPath = ClassPathDependencyResolver.class
            .getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath();
        if (!jarPath.endsWith(".jar")) {
            System.err.println("The class is not running in a jar file! Is this a development environment?");
            System.exit(1);
        }

        URL[] urls = resolveDependencies(
            new File(jarPath),
            s -> {},
            System.out::println,
            System.err::println
        );

        // Add command line argument: --add-opens java.base/jdk.internal.loader=ALL-UNNAMED
        ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
        Class<?> builtinClassLoaderClass = systemLoader.getClass();
        MethodHandle addURL = MethodHandles
            .privateLookupIn(builtinClassLoaderClass, MethodHandles.lookup())
            .findVirtual(
                builtinClassLoaderClass,
                "appendClassPath",
                MethodType.methodType(void.class, String.class)
            );
        for (URL url : urls) {
            addURL.invoke(systemLoader, url.getFile());
        }
    }

    public static URL[] resolveDependencies(File jarFile, Consumer<String> debugService, Consumer<String> loggerService, Consumer<String> errorService) throws IOException {
        Set<File> parsedDependencies = new HashSet<>();
        parsedDependencies.add(jarFile);

        try (ZipFile zipFile = new ZipFile(jarFile)) {
            ZipEntry entry = zipFile.getEntry("META-INF/DEPENDENCIES");
            if (entry == null) {
                errorService.accept("No dependencies found in jar!");
                return new URL[0];
            }

            String depListStr = new String(zipFile.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
            String[] depList = depListStr.split("\n");
            for (String dep : depList) {
                if (dep.isEmpty())
                    continue;
                String[] depInfo = dep.split(":");
                String depGroup = depInfo[0];
                String depName = depInfo[1];
                String depVersion = depInfo[2];
                String depClassifier = depInfo.length > 3 ? depInfo[3] : null;

                debugService.accept(
                    depClassifier == null
                    ? "Checking dependency %s:%s:%s".formatted(depGroup, depName, depVersion)
                    : "Checking dependency %s:%s:%s:%s".formatted(depGroup, depName, depVersion, depClassifier)
                );

                String path = "%s/%s/%s/%s-%s%s.jar".formatted(
                    depGroup.replace('.', '/'),
                    depName,
                    depVersion,
                    depName,
                    depVersion,
                    depClassifier == null ? "" : "-" + depClassifier
                );

                File depFile = new File(LIBRARY_PATH, path);
                while (!checkFileValid(depFile.getAbsolutePath())) {
                    depFile.getParentFile().mkdirs();
                    URI md5 = URI.create("https://repo1.maven.org/maven2/%s.md5".formatted(path));
                    URI jar = URI.create("https://repo1.maven.org/maven2/%s".formatted(path));
                    loggerService.accept("Downloading dependency %s".formatted(dep));
                    try (
                        InputStream md5Stream = md5.toURL().openStream();
                        InputStream jarStream = jar.toURL().openStream()
                    ) {
                        depFile.createNewFile();
                        try (FileOutputStream fileStream = new FileOutputStream(depFile)) {
                            fileStream.write(jarStream.readAllBytes());
                        }
                        try (FileWriter writer = new FileWriter(depFile.getAbsolutePath() + ".md5")) {
                            writer.write(new String(md5Stream.readAllBytes(), StandardCharsets.UTF_8));
                        }
                    } catch (Exception e) {
                        errorService.accept("Error downloading dependency %s: %s:%s".formatted(
                            depFile.getName(),
                            e.getClass().getName(),
                            e.getMessage()
                        ));
                        depFile.delete();
                    }
                }

                debugService.accept("Dependency %s is valid".formatted(depFile.getName()));
                parsedDependencies.add(depFile);
            }
        }

        return parsedDependencies
            .stream()
            .map(File::toURI)
            .map(FunctionUtil.noException(URI::toURL))
            .toArray(URL[]::new);
    }

    private static boolean checkFileValid(String filePath) {
        File checksumFile = new File(filePath + ".md5");
        if (!checksumFile.exists())
            return false;
        File file = new File(filePath);
        if (!file.exists())
            return false;
        try (FileReader reader = new FileReader(checksumFile); InputStream stream = new FileInputStream(file)) {
            char[] md5Checksum = new char[32];
            if (reader.read(md5Checksum) != 32)
                return false;
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(stream.readAllBytes());
            byte[] digest = md5.digest();
            StringBuilder builder = new StringBuilder();
            for (byte b : digest) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString().equals(new String(md5Checksum));
        } catch (Exception e) {
            return false;
        }
    }
}
