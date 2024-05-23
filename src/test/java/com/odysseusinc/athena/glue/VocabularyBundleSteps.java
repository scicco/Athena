package com.odysseusinc.athena.glue;

import com.odysseusinc.athena.model.athena.DownloadBundle;
import com.odysseusinc.athena.model.security.AthenaUser;
import com.odysseusinc.athena.repositories.v5history.VocabularyReleaseVersionRepository;
import com.odysseusinc.athena.service.VocabularyReleaseVersionService;
import com.odysseusinc.athena.service.VocabularyService;
import com.odysseusinc.athena.service.VocabularyServiceV5;
import com.odysseusinc.athena.service.writer.FileHelper;
import com.odysseusinc.athena.util.CDMVersion;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class VocabularyBundleSteps {


    //TODO this is not correct, the authorization should be done
    public static final AthenaUser MOCK_USER = new AthenaUser(1L);
    //TODO that is hard coded id of the vocabularies to download
    private static final List<Integer> TEST_VOCABULARIES = Arrays.asList(18, 125, 21, 78, 71, 72, 66, 2, 127, 1, 0, 90, 70, 34);

    @Autowired
    private World world;

    @Autowired
    private VocabularyService vocabularyService;


    @Autowired
    private VocabularyReleaseVersionRepository vocabularyReleaseVersionRepository;


    @Autowired
    private VocabularyReleaseVersionService vocabularyReleaseVersionService;

    @Autowired
    private FileHelper fileHelper;


    @Autowired
    @Qualifier("dataSourceAthenaV5History")
    private DataSource v5HistoryDataSource;

    @Autowired
    protected VocabularyServiceV5 vocabularyServiceV5;

    public final QueryRunner queryRunner = new QueryRunner();

    @Then("user import vocabulary from the {string} schema")
    public void userImportVocabulary(String schema) {
        importNewVersion("public", schema);
    }

    @When("user generates a bundle for current version")
    public void bundleForCurrent() {
        String bundleName = String.format("%s-%s", "Bundle", "current");
        Integer currentVersion = vocabularyServiceV5.getReleaseVocabularyVersionId();
        DownloadBundle bundle = vocabularyService.saveBundle(
                bundleName, TEST_VOCABULARIES,
                MOCK_USER, CDMVersion.V5, currentVersion, false, null);
        world.setCursor(() -> generateAndDownload(bundle, MOCK_USER));
    }

    @When("user copy {string} bundle with {string} and generate it")
    public void copyAndGenerate(String idRef, String name) {
        Long id = Long.valueOf(world.ref(idRef));
        DownloadBundle bundle = vocabularyService.copyBundle(id, name, MOCK_USER);
        world.setCursor(() -> generateAndDownload(bundle, MOCK_USER));
    }

    @When("user generates a {int} version bundle")
    public void bundleForVersion(Integer version) {
        String bundleName = String.format("%s-%s", "Bundle", version);
        DownloadBundle bundle = vocabularyService.saveBundle(
                bundleName, TEST_VOCABULARIES,
                MOCK_USER, CDMVersion.V5, version, false, null);
        world.setCursor(() -> generateAndDownload(bundle, MOCK_USER));
    }

    @When("user compare with a {int} version bundle")
    public void compareWith(Integer version) throws IOException {
        String bundleName = String.format("%s-%s", "Bundle", version);
        DownloadBundle bundle = vocabularyService.saveBundle(
                bundleName, TEST_VOCABULARIES,
                MOCK_USER, CDMVersion.V5, version, false, null);
        List<FileInfo> fileInfos1 = (List<FileInfo>)world.getCursor();
        List<FileInfo> fileInfos2 = generateAndDownload(bundle, MOCK_USER);
        world.setCursor(() -> compareFiles(fileInfos1, fileInfos2));

    }

    @When("user get vocabulary release version")
    public void vocabularyReleaseVersion() {
        world.setCursor(() -> vocabularyReleaseVersionService.getCurrentFormatted());
    }

    @When("user generates delta bundle for versions: {int} and {int}")
    public void bundleForDelta(Integer version, Integer deltaVersion) {

        String bundleName = String.format("%s-%s", "Bundle", version);
        DownloadBundle bundle = vocabularyService.saveBundle(
                bundleName, TEST_VOCABULARIES,
                MOCK_USER, CDMVersion.V5, version, true, deltaVersion);
        world.setCursor(() -> generateAndDownload(bundle, MOCK_USER));
    }

    @When("run {string} script on {string} schema")
    public void run(String script, String schema) {
        String path = world.ref(script);
        runScript(schema, path);
    }


    @When("user inspects list of vocabulary release version")
    public void inspectReleaseVersions() {
        world.setCursor(() ->
                new ArrayList<>(vocabularyReleaseVersionRepository.findAll())
        );
    }

    @When("user inspects list of bundles")
    public void inspectBundles() {
        world.setCursor(() ->
                new ArrayList<>(vocabularyService.getDownloadHistory(MOCK_USER))
        );
    }


    @When("user compare and inspect schemas {string} and {string}")
    public void compareAndInspectSchemas(String schema1, String schema2) {
        world.setCursor(() ->
                compareSchemas(schema1, schema2)
        );
    }

    private List<FileInfo> generateAndDownload(DownloadBundle bundle, AthenaUser user) throws IOException {
        vocabularyService.generateBundle(bundle, user);
        String zipPath = fileHelper.getZipPath(bundle.getUuid());

        Path tempDir = Files.createTempDirectory(UUID.randomUUID().toString());
        List<FileInfo> unzip = unzip(zipPath, tempDir.toString(), FileInfo::fromPath);
        return unzip;
    }

    public <T> List<T> unzip(String zipFilePath, String destDir, Function<String, T> fun) throws IOException {
        List<T> result = new ArrayList<>();
        try (ZipArchiveInputStream zipIn = new ZipArchiveInputStream(Files.newInputStream(Paths.get(zipFilePath)))) {
            ArchiveEntry entry = zipIn.getNextEntry();
            while (entry != null) {
                String filePath = destDir + File.separator + entry.getName();
                if (!entry.isDirectory()) {
                    extractFile(zipIn, filePath);
                    Optional.ofNullable(fun.apply(filePath)).ifPresent(result::add);
                }
                entry = zipIn.getNextEntry();
            }
        }
        return result;
    }

    public static void extractFile(InputStream input, String outputPath) throws IOException {
        try (OutputStream output = Files.newOutputStream(Paths.get(outputPath))) {
            IOUtils.copy(input, output);
        }
    }


    public void runScript(String schema, String filePath) {
        try (Connection conn = v5HistoryDataSource.getConnection()) {
            queryRunner.execute(conn, "SET search_path TO " + schema);
            queryRunner.execute(conn, new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8));
            log.info("SQL script executed successfully: '{}' in schema '{}'", filePath, schema);
        } catch (IOException e) {
            throw new AssertionFailedError(MessageFormat.format("Error reading SQL script from file: {}", filePath), e);
        } catch (SQLException e) {
            throw new AssertionFailedError(MessageFormat.format("Error running SQL script file: {}", filePath), e);
        }
    }

    /**
     * This import_new_version is not directly used in Java code; instead, it is part of the update vocabulary release process.
     */
    public void importNewVersion(String target, String source) {
        try (Connection conn = v5HistoryDataSource.getConnection()) {
            queryRunner.execute(conn, "SELECT import_new_version(?,?)", target, source);
            log.info("Import version executed successfully: from {} to {}", source, target);
        } catch (SQLException e) {
            throw new AssertionFailedError(MessageFormat.format("Error importing version: from {} to {}", source, target), e);
        }
    }

    public List<SchemaComparisonResult> compareSchemas(String schema1, String schema2) {
        try (Connection conn = v5HistoryDataSource.getConnection();) {
            List<SchemaComparisonResult> results =
                    queryRunner.query(conn, "SELECT * FROM public.compare_schemas(?, ?)", new BeanListHandler<>(SchemaComparisonResult.class), schema1, schema2);
            log.info("Schema comparison executed successfully: {} and {}", schema1, schema2);
            return results;
        } catch (SQLException e) {
            throw new AssertionFailedError("Error comparing schemas:", e);
        }
    }

    private List<FileCompare> compareFiles(List<FileInfo> info1, List<FileInfo> info2) {
        Map<String, FileInfo> fileInfoMap1 = info1.stream().collect(Collectors.toMap(
                FileInfo::getName,
                fileInfo -> fileInfo
        ));

        return info2.stream().map(i2 -> {
            FileInfo fileInfo1 = fileInfoMap1.get(i2.getName());
            return new FileCompare(i2.getName(), fileInfo1.getRows() - i2.getRows());
        }).collect(Collectors.toList());
    }


    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class SchemaComparisonResult {
        private String name;
        private int amount1;
        private int amount2;
        private int missing1;
        private int missing2;
    }

    @AllArgsConstructor
    @Getter
    public static class FileInfo {
        private final String name;
        private final String ext;
        private final Path path;
        private final long size;
        private final long rows;

        public static FileInfo fromPath(String pathString) {
            return fromPath(Paths.get(pathString));
        }

        public static FileInfo fromPath(Path path) {
            try {
                String fileName = path.getFileName().toString();
                String name = fileName.substring(0, fileName.lastIndexOf('.'));
                String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
                long size = Files.size(path);
                long lineCount = extension.equals("csv") ?
                        Files.lines(path).count() -1 :
                        Files.lines(path).count();
                return new FileInfo(name, extension, path, size, lineCount);
            } catch (IOException e) {
                throw new AssertionFailedError(MessageFormat.format("Error processing file: {}", path), e);
            }
        }
    }

    @AllArgsConstructor
    @Getter
    public static class FileCompare {
        private final String name;
        private final long diff;
    }

}

