package fr.abes.logskbart.service;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CandidatsDoublonsServiceTest {

    private static final List<String> HEADERS = List.of(
            "PPN",
            "Commande WinIBW",
            "Titre",
            "Type de ressource",
            "ISSN imprimé",
            "ISSN en ligne",
            "Nom du bouquet",
            "Nature de l'erreur"
    );

    @TempDir
    Path tempDir;

    private BadReportService badReportService;

    @BeforeEach
    void setUp() throws IOException {
        badReportService = new BadReportService(tempDir.toString());
        Files.createDirectories(tempDir.resolve("bad"));
    }

    @Test
    void createsAWorkbookWithExpectedColumnsAndCandidateData() throws IOException {
        String filename = "TEST_PROVIDER_PACKAGE_2026-07-16.tsv";
        writeOtherReport(filename,
                "1615\tPlusieurs ppn électroniques (290245540 OU 289331811) ont le même score. "
                        + "[ publication title : Petite Histoire / publication_type : monograph "
                        + "/ online_identifier : 9782200626297 / print_identifier : 9782200622572 ]");
        CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);

        boolean appended = service.append(filename);

        assertTrue(appended);
        try (Workbook workbook = WorkbookFactory.create(service.electronicWorkbookPath(filename).toFile())) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("CandidatsDoublons", sheet.getSheetName());
            assertEquals(HEADERS, values(sheet.getRow(0)));
            assertEquals(List.of(
                    "290245540 OU 289331811",
                    "che ppn 290245540 OU 289331811",
                    "Petite Histoire",
                    "monograph",
                    "9782200622572",
                    "9782200626297",
                    "TEST_PROVIDER_PACKAGE_2026-07-16",
                    "Plusieurs ppn électroniques"
            ), values(sheet.getRow(1)));
            IntStream.range(0, HEADERS.size())
                    .forEach(index -> {
                        assertEquals(CellType.STRING, sheet.getRow(1).getCell(index).getCellType());
                        assertEquals("@", sheet.getRow(1).getCell(index).getCellStyle().getDataFormatString());
                    });
        }
    }

    @Test
    void createsBouquetReportsAndPreservesHistoricalGlobalFiles() throws IOException {
        Path historicalTxt = tempDir.resolve("CandidatsDoublons.txt");
        Path historicalXlsx = tempDir.resolve("CandidatsDoublons.xlsx");
        Files.writeString(historicalTxt, "historique à conserver");
        Files.writeString(historicalXlsx, "ancien classeur à conserver");
        CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);

        writeOtherReport("FIRST.tsv",
                "1\tPlusieurs ppn imprimés (111111111) ont été trouvés. [ publication title : Titre 1 "
                        + "/ publication_type : serial / online_identifier : 1111-1111 "
                        + "/ print_identifier : 2222-2222 ]");
        service.append("FIRST.tsv");
        writeOtherReport("SECOND.tsv",
                "2\tPlusieurs ppn électroniques (222222222) ont le même score. [ publication title : Titre 2 "
                        + "/ publication_type : monograph / online_identifier : 3333-3333 "
                        + "/ print_identifier : 4444-4444 ]");
        service.append("SECOND.tsv");

        assertTrue(Files.exists(service.printedWorkbookPath("FIRST.tsv")));
        assertTrue(Files.exists(service.electronicWorkbookPath("SECOND.tsv")));
        assertEquals("historique à conserver", Files.readString(historicalTxt));
        assertEquals("ancien classeur à conserver", Files.readString(historicalXlsx));
    }

    @Test
    void separatesElectronicAndPrintedCandidatesForTheProcessedBouquet() throws IOException {
        String filename = "TEST_PROVIDER_PACKAGE_2026-07-16.tsv";
        writeOtherReport(filename,
                "1\tPlusieurs ppn électroniques (111111111 OU 222222222) ont le même score. "
                        + "[ publication title : Titre électronique / publication_type : serial ]"
                        + System.lineSeparator()
                        + "2\tPlusieurs ppn imprimés (333333333 OU 444444444) ont été trouvés. "
                        + "[ publication title : Titre imprimé / publication_type : monograph ]");
        CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);

        assertTrue(service.append(filename));

        Path electronic = tempDir.resolve("bad")
                .resolve("TEST_PROVIDER_PACKAGE_2026-07-16_candidats_electroniques.xlsx");
        Path printed = tempDir.resolve("bad")
                .resolve("TEST_PROVIDER_PACKAGE_2026-07-16_candidats_imprimes.xlsx");
        assertTrue(Files.exists(electronic));
        assertTrue(Files.exists(printed));
        assertEquals("Titre électronique", workbookValue(electronic, 1, 2));
        assertEquals("Titre imprimé", workbookValue(printed, 1, 2));
        assertFalse(Files.exists(tempDir.resolve("CandidatsDoublons.xlsx")));
    }

    @Test
    void removesForceAndBypassSuffixesFromBouquetNames() throws IOException {
        CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);
        writeOtherReport("FIRST_FORCE.tsv",
                "1\tPlusieurs ppn électroniques (111111111) [ publication title : Titre 1 ]");
        service.append("FIRST_FORCE.tsv");
        writeOtherReport("SECOND_BYPASS.tsv",
                "2\tPlusieurs ppn électroniques (222222222) [ publication title : Titre 2 ]");
        service.append("SECOND_BYPASS.tsv");

        assertEquals("FIRST", workbookValue(
                service.electronicWorkbookPath("FIRST_FORCE.tsv"), 1, 6));
        assertEquals("SECOND", workbookValue(
                service.electronicWorkbookPath("SECOND_BYPASS.tsv"), 1, 6));
    }

    @Test
    void doesNotCreateWorkbookWhenReportContainsNoStructuredCandidate() throws IOException {
        writeOtherReport("INVALID.tsv", "1\tErreur technique sans données KBART");
        CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);

        assertFalse(service.append("INVALID.tsv"));
        assertFalse(Files.exists(service.electronicWorkbookPath("INVALID.tsv")));
        assertFalse(Files.exists(service.printedWorkbookPath("INVALID.tsv")));
    }

    @Test
    void formatsHeaderFreezesFirstRowAndEnablesFilter() throws IOException {
        String filename = "FORMATTED.tsv";
        writeOtherReport(filename, "1\tPlusieurs ppn électroniques (111111111) [ publication title : Un titre ]");
        CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);
        service.append(filename);

        try (Workbook workbook = WorkbookFactory.create(service.electronicWorkbookPath(filename).toFile())) {
            XSSFSheet sheet = (XSSFSheet) workbook.getSheetAt(0);
            assertNotNull(sheet.getPaneInformation());
            assertTrue(sheet.getPaneInformation().isFreezePane());
            assertEquals(1, sheet.getPaneInformation().getHorizontalSplitPosition());
            assertTrue(sheet.getCTWorksheet().isSetAutoFilter());

            Font headerFont = workbook.getFontAt(sheet.getRow(0).getCell(0).getCellStyle().getFontIndex());
            assertTrue(headerFont.getBold());
            assertEquals(IndexedColors.WHITE.getIndex(), headerFont.getColor());
            assertTrue(sheet.getColumnWidth(2) >= 40 * 256);
            IntStream.range(0, HEADERS.size())
                    .forEach(index -> assertTrue(sheet.getColumnWidth(index) <= 60 * 256));
        }
    }

    @Test
    void doesNotAppendCandidateAlreadyPresentAndDoesNotRewriteWorkbook() throws IOException {
        String filename = "TEST_PROVIDER_PACKAGE_2026-07-16.tsv";
        String candidate = "1\tPlusieurs ppn électroniques (111111111) [ publication title : Titre 1 "
                + "/ publication_type : serial / online_identifier : 1111-1111 "
                + "/ print_identifier : 2222-2222 ]";
        writeOtherReport(filename, candidate);
        CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);

        assertTrue(service.append(filename));
        FileTime marker = FileTime.from(Instant.parse("2020-01-01T00:00:00Z"));
        Files.setLastModifiedTime(service.electronicWorkbookPath(filename), marker);

        assertFalse(service.append(filename));
        assertEquals(marker, Files.getLastModifiedTime(service.electronicWorkbookPath(filename)));
        assertFalse(Files.exists(service.electronicWorkbookPath(filename).resolveSibling(
                service.electronicWorkbookPath(filename).getFileName() + ".tmp")));
        try (Workbook workbook = WorkbookFactory.create(service.electronicWorkbookPath(filename).toFile())) {
            assertEquals(1, workbook.getSheetAt(0).getLastRowNum());
        }
    }

    @Test
    void appendsOnlyOnceWhenReportContainsTheSameCandidateTwice() throws IOException {
        String filename = "TEST_PROVIDER_PACKAGE_2026-07-16.tsv";
        String candidate = "1\tPlusieurs ppn électroniques (111111111) [ publication title : Titre 1 "
                + "/ publication_type : serial / online_identifier : 1111-1111 "
                + "/ print_identifier : 2222-2222 ]";
        Files.writeString(
                badReportService.otherReportPath(filename),
                "LINE\tMESSAGE\t" + System.lineSeparator()
                        + candidate + System.lineSeparator()
                        + candidate + System.lineSeparator()
        );
        CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);

        assertTrue(service.append(filename));
        try (Workbook workbook = WorkbookFactory.create(service.electronicWorkbookPath(filename).toFile())) {
            assertEquals(1, workbook.getSheetAt(0).getLastRowNum());
        }
    }

    @Test
    void appendsOnlyNewCandidatesWhenWorkbookContainsExistingRows() throws IOException {
        String filename = "SAME.tsv";
        CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);
        writeOtherReport(filename,
                "1\tPlusieurs ppn électroniques (111111111) [ publication title : Existant "
                        + "/ publication_type : serial / online_identifier : 1111-1111 "
                        + "/ print_identifier : 2222-2222 ]");
        assertTrue(service.append(filename));

        Files.writeString(
                badReportService.otherReportPath(filename),
                "LINE\tMESSAGE\t" + System.lineSeparator()
                        + "1\tPlusieurs ppn électroniques (111111111) [ publication title : Existant "
                        + "/ publication_type : serial / online_identifier : 1111-1111 "
                        + "/ print_identifier : 2222-2222 ]" + System.lineSeparator()
                        + "2\tPlusieurs ppn électroniques (222222222) [ publication title : Nouveau "
                        + "/ publication_type : monograph / online_identifier : 3333-3333 "
                        + "/ print_identifier : 4444-4444 ]" + System.lineSeparator()
        );

        assertTrue(service.append(filename));
        try (Workbook workbook = WorkbookFactory.create(service.electronicWorkbookPath(filename).toFile())) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(2, sheet.getLastRowNum());
            assertEquals("111111111", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("222222222", sheet.getRow(2).getCell(0).getStringCellValue());
        }
    }

    @Test
    void treatsMissingEmptyAndSpacePaddedCellsAsTheSameNormalizedCandidate() throws IOException {
        String filename = "TEST_PROVIDER_PACKAGE_2026-07-16.tsv";
        CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);
        writeExistingCandidateWorkbook(service, filename, List.of(
                " 111111111 ",
                " che ppn 111111111 ",
                " Titre 1 ",
                "",
                "",
                "",
                " TEST_PROVIDER_PACKAGE_2026-07-16 ",
                " Plusieurs ppn électroniques "
        ), 3);
        writeOtherReport(filename,
                "1\tPlusieurs ppn électroniques (111111111) [ publication title : Titre 1 ]");
        FileTime marker = FileTime.from(Instant.parse("2020-01-01T00:00:00Z"));
        Files.setLastModifiedTime(service.electronicWorkbookPath(filename), marker);

        assertFalse(service.append(filename));
        assertEquals(marker, Files.getLastModifiedTime(service.electronicWorkbookPath(filename)));
        assertFalse(Files.exists(service.electronicWorkbookPath(filename).resolveSibling(
                service.electronicWorkbookPath(filename).getFileName() + ".tmp")));
    }

    @Test
    void appendsCandidateWhenAValueDiffersOnlyByCase() throws IOException {
        String filename = "TEST_PROVIDER_PACKAGE_2026-07-16.tsv";
        CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);
        writeOtherReport(filename,
                "1\tPlusieurs ppn électroniques (111111111) [ publication title : Titre 1 ]");
        assertTrue(service.append(filename));

        writeOtherReport(filename,
                "1\tPlusieurs ppn électroniques (111111111) [ publication title : titre 1 ]");

        assertTrue(service.append(filename));
        try (Workbook workbook = WorkbookFactory.create(service.electronicWorkbookPath(filename).toFile())) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(2, sheet.getLastRowNum());
            assertEquals("Titre 1", sheet.getRow(1).getCell(2).getStringCellValue());
            assertEquals("titre 1", sheet.getRow(2).getCell(2).getStringCellValue());
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7})
    void appendsCandidateWhenAnyOfTheEightKeyColumnsDiffers(int differentColumn) throws IOException {
        String filename = "TEST_PROVIDER_PACKAGE_2026-07-16.tsv";
        CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);
        List<String> existingValues = new ArrayList<>(List.of(
                "111111111",
                "che ppn 111111111",
                "Titre 1",
                "serial",
                "2222-2222",
                "1111-1111",
                "TEST_PROVIDER_PACKAGE_2026-07-16",
                "Plusieurs ppn électroniques"
        ));
        existingValues.set(differentColumn, "valeur différente");
        writeExistingCandidateWorkbook(service, filename, existingValues, -1);
        writeOtherReport(filename,
                "1\tPlusieurs ppn électroniques (111111111) [ publication title : Titre 1 "
                        + "/ publication_type : serial / online_identifier : 1111-1111 "
                        + "/ print_identifier : 2222-2222 ]");

        assertTrue(service.append(filename));
        try (Workbook workbook = WorkbookFactory.create(service.electronicWorkbookPath(filename).toFile())) {
            assertEquals(2, workbook.getSheetAt(0).getLastRowNum());
        }
    }

    private void writeOtherReport(String filename, String line) throws IOException {
        Files.writeString(
                badReportService.otherReportPath(filename),
                "LINE\tMESSAGE\t" + System.lineSeparator() + line + System.lineSeparator()
        );
    }

    private void writeExistingCandidateWorkbook(CandidatsDoublonsService service,
                                                String filename,
                                                List<String> existingValues,
                                                int missingCellIndex) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("CandidatsDoublons");
            Row header = sheet.createRow(0);
            IntStream.range(0, HEADERS.size())
                    .forEach(index -> header.createCell(index).setCellValue(HEADERS.get(index)));
            Row existingRow = sheet.createRow(1);
            IntStream.range(0, existingValues.size())
                    .filter(index -> index != missingCellIndex)
                    .forEach(index -> existingRow.createCell(index).setCellValue(existingValues.get(index)));
            Path path = service.electronicWorkbookPath(filename);
            Files.createDirectories(path.getParent());
            try (var output = Files.newOutputStream(path)) {
                workbook.write(output);
            }
        }
    }

    private List<String> values(Row row) {
        return IntStream.range(0, HEADERS.size())
                .mapToObj(index -> row.getCell(index).getStringCellValue())
                .toList();
    }

    private String workbookValue(Path path, int row, int column) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(path.toFile())) {
            return workbook.getSheetAt(0).getRow(row).getCell(column).getStringCellValue();
        }
    }
}
