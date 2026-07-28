package fr.abes.logskbart.service;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
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
        try (Workbook workbook = WorkbookFactory.create(service.workbookPath().toFile())) {
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
    void appendsCandidatesAndPreservesHistoricalTxtFile() throws IOException {
        Path historicalTxt = tempDir.resolve("CandidatsDoublons.txt");
        Files.writeString(historicalTxt, "historique à conserver");
        CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);

        writeOtherReport("FIRST.tsv",
                "1\tDoublon imprimé (111111111) [ publication title : Titre 1 "
                        + "/ publication_type : serial / online_identifier : 1111-1111 "
                        + "/ print_identifier : 2222-2222 ]");
        service.append("FIRST.tsv");
        writeOtherReport("SECOND.tsv",
                "2\tDoublon électronique (222222222) [ publication title : Titre 2 "
                        + "/ publication_type : monograph / online_identifier : 3333-3333 "
                        + "/ print_identifier : 4444-4444 ]");
        service.append("SECOND.tsv");

        try (Workbook workbook = WorkbookFactory.create(service.workbookPath().toFile())) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(2, sheet.getLastRowNum());
            assertEquals("111111111", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("222222222", sheet.getRow(2).getCell(0).getStringCellValue());
        }
        assertEquals("historique à conserver", Files.readString(historicalTxt));
    }

    @Test
    void removesForceAndBypassSuffixesFromBouquetNames() throws IOException {
        CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);
        writeOtherReport("FIRST_FORCE.tsv",
                "1\tDoublon (111111111) [ publication title : Titre 1 ]");
        service.append("FIRST_FORCE.tsv");
        writeOtherReport("SECOND_BYPASS.tsv",
                "2\tDoublon (222222222) [ publication title : Titre 2 ]");
        service.append("SECOND_BYPASS.tsv");

        try (Workbook workbook = WorkbookFactory.create(service.workbookPath().toFile())) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals("FIRST", sheet.getRow(1).getCell(6).getStringCellValue());
            assertEquals("SECOND", sheet.getRow(2).getCell(6).getStringCellValue());
        }
    }

    @Test
    void doesNotCreateWorkbookWhenReportContainsNoStructuredCandidate() throws IOException {
        writeOtherReport("INVALID.tsv", "1\tErreur technique sans données KBART");
        CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);

        assertFalse(service.append("INVALID.tsv"));
        assertFalse(Files.exists(service.workbookPath()));
    }

    @Test
    void formatsHeaderFreezesFirstRowAndEnablesFilter() throws IOException {
        String filename = "FORMATTED.tsv";
        writeOtherReport(filename, "1\tDoublon (111111111) [ publication title : Un titre ]");
        CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);
        service.append(filename);

        try (Workbook workbook = WorkbookFactory.create(service.workbookPath().toFile())) {
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
        String candidate = "1\tDoublon (111111111) [ publication title : Titre 1 "
                + "/ publication_type : serial / online_identifier : 1111-1111 "
                + "/ print_identifier : 2222-2222 ]";
        writeOtherReport(filename, candidate);
        CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);

        assertTrue(service.append(filename));
        FileTime marker = FileTime.from(Instant.parse("2020-01-01T00:00:00Z"));
        Files.setLastModifiedTime(service.workbookPath(), marker);

        assertFalse(service.append(filename));
        assertEquals(marker, Files.getLastModifiedTime(service.workbookPath()));
        assertFalse(Files.exists(service.workbookPath().resolveSibling("CandidatsDoublons.xlsx.tmp")));
        try (Workbook workbook = WorkbookFactory.create(service.workbookPath().toFile())) {
            assertEquals(1, workbook.getSheetAt(0).getLastRowNum());
        }
    }

    @Test
    void appendsOnlyOnceWhenReportContainsTheSameCandidateTwice() throws IOException {
        String filename = "TEST_PROVIDER_PACKAGE_2026-07-16.tsv";
        String candidate = "1\tDoublon (111111111) [ publication title : Titre 1 "
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
        try (Workbook workbook = WorkbookFactory.create(service.workbookPath().toFile())) {
            assertEquals(1, workbook.getSheetAt(0).getLastRowNum());
        }
    }

    @Test
    void appendsOnlyNewCandidatesWhenWorkbookContainsExistingRows() throws IOException {
        CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);
        writeOtherReport("SAME.tsv",
                "1\tDoublon (111111111) [ publication title : Existant "
                        + "/ publication_type : serial / online_identifier : 1111-1111 "
                        + "/ print_identifier : 2222-2222 ]");
        assertTrue(service.append("SAME.tsv"));

        Files.writeString(
                badReportService.otherReportPath("SAME.tsv"),
                "LINE\tMESSAGE\t" + System.lineSeparator()
                        + "1\tDoublon (111111111) [ publication title : Existant "
                        + "/ publication_type : serial / online_identifier : 1111-1111 "
                        + "/ print_identifier : 2222-2222 ]" + System.lineSeparator()
                        + "2\tDoublon (222222222) [ publication title : Nouveau "
                        + "/ publication_type : monograph / online_identifier : 3333-3333 "
                        + "/ print_identifier : 4444-4444 ]" + System.lineSeparator()
        );

        assertTrue(service.append("SAME.tsv"));
        try (Workbook workbook = WorkbookFactory.create(service.workbookPath().toFile())) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(2, sheet.getLastRowNum());
            assertEquals("111111111", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("222222222", sheet.getRow(2).getCell(0).getStringCellValue());
        }
    }

    private void writeOtherReport(String filename, String line) throws IOException {
        Files.writeString(
                badReportService.otherReportPath(filename),
                "LINE\tMESSAGE\t" + System.lineSeparator() + line + System.lineSeparator()
        );
    }

    private List<String> values(Row row) {
        return IntStream.range(0, HEADERS.size())
                .mapToObj(index -> row.getCell(index).getStringCellValue())
                .toList();
    }
}
