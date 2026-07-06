package fr.abes.logskbart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.logskbart.service.EmailService;
import fr.abes.logskbart.service.LogsService;
import fr.abes.logskbart.utils.UtilsMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;

class LogsListenerTest {

    private LogsListener logsListener;

    @TempDir
    Path tempDir;

    private Path tempLogDir;

    @BeforeEach
    void setUp() throws Exception {
        // Create the instance with null/mock dependencies - only extractField and appendToCandidatsDoublons are tested
        logsListener = new LogsListener(
                new ObjectMapper(),
                null, // UtilsMapper - not used in tested methods
                null, // LogsService - not used in tested methods
                Map.of(),
                null, // Executor - not used in tested methods
                null  // EmailService - not used in tested methods
        );

        // Create tempLog directory in the working directory for appendToCandidatsDoublons tests
        tempLogDir = Path.of("tempLog");
        if (!Files.exists(tempLogDir)) {
            Files.createDirectory(tempLogDir);
        }
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up test files
        Path candidatsDoublons = Path.of("tempLog", "CandidatsDoublons.txt");
        Files.deleteIfExists(candidatsDoublons);
        Path badFile = Path.of("tempLog", "TEST_PROVIDER_PACKAGE_2025-11-02.bad");
        Files.deleteIfExists(badFile);
        Path badFileForce = Path.of("tempLog", "TEST_PROVIDER_PACKAGE_2025-11-02_FORCE.bad");
        Files.deleteIfExists(badFileForce);
    }

    @Test
    @DisplayName("Test extractField : extraction nature de l'erreur (texte avant la parenthese)")
    void testExtractFieldNatureErreur() {
        String message = "Plusieurs ppn électroniques (290245540 OU 289331811) ont le même score. [ publication title : Titre / publication_type : monograph ]";
        String result = logsListener.extractField(message, "^(.+?)\\(");
        assertNotNull(result);
        assertEquals("Plusieurs ppn électroniques", result.trim());
    }

    @Test
    @DisplayName("Test extractField : extraction des PPNs entre parentheses")
    void testExtractFieldPpns() {
        String message = "Plusieurs ppn électroniques (290245540 OU 289331811) ont le même score. [ publication title : Titre ]";
        String result = logsListener.extractField(message, "\\(([^)]+)\\)");
        assertNotNull(result);
        assertEquals("290245540 OU 289331811", result);
    }

    @Test
    @DisplayName("Test extractField : extraction du titre")
    void testExtractFieldTitre() {
        String message = "Plusieurs ppn électroniques (290245540) ont le même score. [ publication title : Petite Histoire Des Faits Économiques / publication_type : monograph / online_identifier : 9782200626297 / print_identifier : 9782200622572 ]";
        String result = logsListener.extractField(message, "publication title : (.+?)(?: /| \\])");
        assertNotNull(result);
        assertEquals("Petite Histoire Des Faits Économiques", result);
    }

    @Test
    @DisplayName("Test extractField : extraction du type de ressource")
    void testExtractFieldTypeRessource() {
        String message = "Plusieurs ppn électroniques (290245540) [ publication title : Titre / publication_type : monograph / online_identifier : 978xxx ]";
        String result = logsListener.extractField(message, "publication_type : (.+?)(?: /| \\])");
        assertNotNull(result);
        assertEquals("monograph", result);
    }

    @Test
    @DisplayName("Test extractField : extraction de l'id online")
    void testExtractFieldIdOnline() {
        String message = "[ publication title : Titre / publication_type : monograph / online_identifier : 9782200626297 / print_identifier : 9782200622572 ]";
        String result = logsListener.extractField(message, "online_identifier : (.+?)(?: /| \\])");
        assertNotNull(result);
        assertEquals("9782200626297", result);
    }

    @Test
    @DisplayName("Test extractField : extraction de l'id imprime")
    void testExtractFieldIdImprime() {
        String message = "[ publication title : Titre / publication_type : monograph / online_identifier : 9782200626297 / print_identifier : 9782200622572 ]";
        String result = logsListener.extractField(message, "print_identifier : (.+?)(?: /| \\])");
        assertNotNull(result);
        assertEquals("9782200622572", result);
    }

    @Test
    @DisplayName("Test extractField : retourne null si le champ n'est pas trouve")
    void testExtractFieldNotFound() {
        String message = "Erreur de connexion CBS";
        String result = logsListener.extractField(message, "publication title : (.+?)(?: /| \\])");
        assertNull(result);
    }

    @Test
    @DisplayName("Test extractField : titre sans online_identifier ni print_identifier")
    void testExtractFieldTitreOnly() {
        String message = "Plusieurs ppn imprimés (123 OU 456) ont été trouvés. [ publication title : Mon Titre ]";
        String titre = logsListener.extractField(message, "publication title : (.+?)(?: /| \\])");
        assertNotNull(titre);
        assertEquals("Mon Titre", titre);

        String idOnline = logsListener.extractField(message, "online_identifier : (.+?)(?: /| \\])");
        assertNull(idOnline);
    }

    @Test
    @DisplayName("Test appendToCandidatsDoublons : creation du fichier avec en-tete depuis un .bad")
    void testAppendToCandidatsDoublonsCreation() throws IOException {
        String filename = "TEST_PROVIDER_PACKAGE_2025-11-02.tsv";
        // Create a .bad file with test content
        Path badFile = Path.of("tempLog", filename.replace(".tsv", ".bad"));
        String badContent = "LINE\tMESSAGE\t\n" +
                "1615\tPlusieurs ppn électroniques (290245540 OU 289331811) ont le même score. [ publication title : Petite Histoire / publication_type : monograph / online_identifier : 9782200626297 / print_identifier : 9782200622572 ]\n";
        Files.write(badFile, badContent.getBytes());

        logsListener.appendToCandidatsDoublons(filename);

        Path candidatsDoublons = Path.of("tempLog", "CandidatsDoublons.txt");
        assertTrue(Files.exists(candidatsDoublons));

        List<String> lines = Files.readAllLines(candidatsDoublons);
        // 1 header + 1 data line
        assertEquals(2, lines.size());
        assertEquals("Nom du bouquet\tNature de l'erreur\trequête WinIBW\tTitre\tType de ressource\tId online\tId imprimé", lines.get(0));

        String dataLine = lines.get(1);
        assertTrue(dataLine.contains("TEST_PROVIDER_PACKAGE_2025-11-02"), "Doit contenir le nom du bouquet");
        assertTrue(dataLine.contains("che ppn 290245540 OU 289331811"), "Doit contenir la requete WinIBW");
        assertTrue(dataLine.contains("Petite Histoire"), "Doit contenir le titre");
        assertTrue(dataLine.contains("monograph"), "Doit contenir le type de ressource");
        assertTrue(dataLine.contains("9782200626297"), "Doit contenir l'id online");
        assertTrue(dataLine.contains("9782200622572"), "Doit contenir l'id imprime");
    }

    @Test
    @DisplayName("Test appendToCandidatsDoublons : ajout de lignes a un fichier existant")
    void testAppendToCandidatsDoublonsAppend() throws IOException {
        // First call creates the file
        String filename1 = "TEST_PROVIDER_PACKAGE_2025-11-02.tsv";
        Path badFile1 = Path.of("tempLog", filename1.replace(".tsv", ".bad"));
        String badContent1 = "LINE\tMESSAGE\t\n" +
                "1615\tPlusieurs ppn électroniques (290245540) [ publication title : Titre1 / publication_type : monograph / online_identifier : 111 / print_identifier : 222 ]\n";
        Files.write(badFile1, badContent1.getBytes());
        logsListener.appendToCandidatsDoublons(filename1);
        Files.delete(badFile1);

        // Second call appends to the existing file
        String filename2 = "TEST_PROVIDER_OTHER_2025-11-03.tsv";
        Path badFile2 = Path.of("tempLog", filename2.replace(".tsv", ".bad"));
        String badContent2 = "LINE\tMESSAGE\t\n" +
                "42\tPlusieurs ppn imprimés (123456789) [ publication title : Titre2 / publication_type : serial / online_identifier : 333 / print_identifier : 444 ]\n";
        Files.write(badFile2, badContent2.getBytes());
        logsListener.appendToCandidatsDoublons(filename2);

        Path candidatsDoublons = Path.of("tempLog", "CandidatsDoublons.txt");
        assertTrue(Files.exists(candidatsDoublons));

        List<String> lines = Files.readAllLines(candidatsDoublons);
        // 1 header + 2 data lines
        assertEquals(3, lines.size());
        assertEquals("Nom du bouquet\tNature de l'erreur\trequête WinIBW\tTitre\tType de ressource\tId online\tId imprimé", lines.get(0));
        assertTrue(lines.get(1).contains("TEST_PROVIDER_PACKAGE_2025-11-02"), "Ligne 1 doit contenir le premier bouquet");
        assertTrue(lines.get(1).contains("Titre1"), "Ligne 1 doit contenir Titre1");
        assertTrue(lines.get(2).contains("TEST_PROVIDER_OTHER_2025-11-03"), "Ligne 2 doit contenir le deuxieme bouquet");
        assertTrue(lines.get(2).contains("Titre2"), "Ligne 2 doit contenir Titre2");
    }

    @Test
    @DisplayName("Test appendToCandidatsDoublons : lignes sans format structure sont ignorees")
    void testAppendToCandidatsDoublonsSkipNonStructuredLines() throws IOException {
        String filename = "TEST_PROVIDER_PACKAGE_2025-11-02.tsv";
        Path badFile = Path.of("tempLog", filename.replace(".tsv", ".bad"));
        String badContent = "LINE\tMESSAGE\t\n" +
                "1\tErreur de connexion CBS\n" +
                "2\tFormat du fichier incorrect\n" +
                "3\tPlusieurs ppn électroniques (290245540) [ publication title : Titre / publication_type : monograph / online_identifier : 111 / print_identifier : 222 ]\n";
        Files.write(badFile, badContent.getBytes());

        logsListener.appendToCandidatsDoublons(filename);

        Path candidatsDoublons = Path.of("tempLog", "CandidatsDoublons.txt");
        assertTrue(Files.exists(candidatsDoublons));

        List<String> lines = Files.readAllLines(candidatsDoublons);
        // 1 header + 1 data line (only the structured line should be included)
        assertEquals(2, lines.size());
        assertTrue(lines.get(1).contains("Titre"));
        assertFalse(lines.get(1).contains("Erreur de connexion"));
        assertFalse(lines.get(1).contains("Format du fichier"));
    }

    @Test
    @DisplayName("Test appendToCandidatsDoublons : fichier .bad avec suffixe _FORCE")
    void testAppendToCandidatsDoublonsWithForceSuffix() throws IOException {
        String filename = "TEST_PROVIDER_PACKAGE_2025-11-02_FORCE.tsv";
        Path badFile = Path.of("tempLog", filename.replace(".tsv", ".bad"));
        String badContent = "LINE\tMESSAGE\t\n" +
                "1\tPlusieurs ppn électroniques (290245540) [ publication title : TitreForce / publication_type : monograph / online_identifier : 111 / print_identifier : 222 ]\n";
        Files.write(badFile, badContent.getBytes());

        logsListener.appendToCandidatsDoublons(filename);

        Path candidatsDoublons = Path.of("tempLog", "CandidatsDoublons.txt");
        assertTrue(Files.exists(candidatsDoublons));

        List<String> lines = Files.readAllLines(candidatsDoublons);
        assertEquals(2, lines.size());
        // The bouquet name should NOT contain _FORCE
        assertTrue(lines.get(1).contains("TEST_PROVIDER_PACKAGE_2025-11-02"), "Le nom du bouquet ne doit pas contenir _FORCE");
        assertFalse(lines.get(1).contains("_FORCE"), "Le nom du bouquet ne doit pas contenir _FORCE");
    }

    @Test
    @DisplayName("Test appendToCandidatsDoublons : fichier .bad inexistant ne fait rien")
    void testAppendToCandidatsDoublonsNoBadFile() throws IOException {
        String filename = "NONEXISTENT_FILE_2025-01-01.tsv";

        logsListener.appendToCandidatsDoublons(filename);

        Path candidatsDoublons = Path.of("tempLog", "CandidatsDoublons.txt");
        assertFalse(Files.exists(candidatsDoublons));
    }
}
