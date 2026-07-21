package fr.abes.logskbart.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CandidatsDoublonsService {

    private static final String SHEET_NAME = "CandidatsDoublons";
    private static final String WORKBOOK_FILENAME = "CandidatsDoublons.xlsx";
    private static final int[] COLUMN_WIDTHS = {22, 32, 60, 24, 20, 20, 40, 50};
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

    private final BadReportService badReportService;

    public CandidatsDoublonsService(BadReportService badReportService) {
        this.badReportService = badReportService;
    }

    public synchronized boolean append(String filename) throws IOException {
        List<CandidatDoublon> candidates = readCandidates(filename);
        if (candidates.isEmpty()) {
            return false;
        }

        Path temporaryPath = temporaryWorkbookPath();
        try (Workbook workbook = openWorkbook()) {
            Sheet sheet = workbook.getSheet(SHEET_NAME);
            if (sheet == null) {
                sheet = createSheet(workbook);
            }

            CellStyle textStyle = textStyle(workbook);
            int rowIndex = sheet.getLastRowNum() + 1;
            for (CandidatDoublon candidate : candidates) {
                writeRow(sheet.createRow(rowIndex++), candidate.values(), textStyle);
            }
            updateAutoFilter(sheet);
            writeTemporary(workbook, temporaryPath);
        }
        replaceWorkbook(temporaryPath);

        log.info("Candidats ajoutés dans {} pour le fichier {}", workbookPath(), filename);
        return true;
    }

    public Path workbookPath() {
        return badReportService.reportDirectory().resolve(WORKBOOK_FILENAME);
    }

    private List<CandidatDoublon> readCandidates(String filename) throws IOException {
        Path badFile = badReportService.otherReportPath(filename);
        if (!Files.exists(badFile)) {
            return List.of();
        }

        String bouquet = Path.of(filename).getFileName().toString()
                .replaceFirst("(?i)\\.tsv$", "")
                .replaceAll("_(FORCE|BYPASS)$", "");
        List<CandidatDoublon> candidates = new ArrayList<>();

        for (String line : Files.readAllLines(badFile, StandardCharsets.UTF_8)) {
            String[] parts = line.split("\\t", 2);
            if (parts.length < 2 || !parts[1].contains("publication title : ")) {
                continue;
            }

            String message = parts[1];
            String ppn = extract(message, "\\(([^)]+)\\)");
            candidates.add(new CandidatDoublon(
                    value(ppn),
                    ppn == null ? "" : "che ppn " + ppn,
                    value(extract(message, "publication title : (.+?)(?: /| \\])")),
                    value(extract(message, "publication_type : (.+?)(?: /| \\])")),
                    value(extract(message, "print_identifier : (.+?)(?: /| \\])")),
                    value(extract(message, "online_identifier : (.+?)(?: /| \\])")),
                    bouquet,
                    value(extract(message, "^(.+?)\\("))
            ));
        }
        return candidates;
    }

    private String extract(String message, String expression) {
        Matcher matcher = Pattern.compile(expression).matcher(message);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private void writeRow(Row row, List<String> values) {
        writeRow(row, values, null);
    }

    private void writeRow(Row row, List<String> values, CellStyle style) {
        for (int index = 0; index < values.size(); index++) {
            var cell = row.createCell(index);
            cell.setCellValue(values.get(index));
            if (style != null) {
                cell.setCellStyle(style);
            }
        }
    }

    private CellStyle textStyle(Workbook workbook) {
        short textFormat = workbook.createDataFormat().getFormat("@");
        for (int index = 0; index < workbook.getNumCellStyles(); index++) {
            CellStyle style = workbook.getCellStyleAt(index);
            if (style.getDataFormat() == textFormat) {
                return style;
            }
        }

        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(textFormat);
        return style;
    }

    private Workbook openWorkbook() throws IOException {
        return Files.exists(workbookPath())
                ? WorkbookFactory.create(workbookPath().toFile())
                : new XSSFWorkbook();
    }

    private Sheet createSheet(Workbook workbook) {
        Sheet sheet = workbook.createSheet(SHEET_NAME);
        Row header = sheet.createRow(0);
        writeRow(header, HEADERS);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.forEach(cell -> cell.setCellStyle(style));

        sheet.createFreezePane(0, 1);
        for (int index = 0; index < COLUMN_WIDTHS.length; index++) {
            sheet.setColumnWidth(index, COLUMN_WIDTHS[index] * 256);
        }
        return sheet;
    }

    private void updateAutoFilter(Sheet sheet) {
        if (sheet instanceof XSSFSheet xssfSheet && xssfSheet.getCTWorksheet().isSetAutoFilter()) {
            xssfSheet.getCTWorksheet().unsetAutoFilter();
        }
        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, HEADERS.size() - 1));
    }

    private Path temporaryWorkbookPath() throws IOException {
        Path workbookPath = workbookPath();
        Files.createDirectories(workbookPath.getParent());
        return workbookPath.resolveSibling(WORKBOOK_FILENAME + ".tmp");
    }

    private void writeTemporary(Workbook workbook, Path temporaryPath) throws IOException {
        try (OutputStream output = Files.newOutputStream(temporaryPath)) {
            workbook.write(output);
        }
    }

    private void replaceWorkbook(Path temporaryPath) throws IOException {
        try {
            Files.move(
                    temporaryPath,
                    workbookPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryPath, workbookPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private record CandidatDoublon(String ppn,
                                   String command,
                                   String title,
                                   String resourceType,
                                   String printIssn,
                                   String onlineIssn,
                                   String bouquet,
                                   String errorNature) {
        private List<String> values() {
            return List.of(ppn, command, title, resourceType, printIssn, onlineIssn, bouquet, errorNature);
        }
    }
}
