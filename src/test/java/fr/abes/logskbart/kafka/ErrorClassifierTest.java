package fr.abes.logskbart.kafka;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorClassifierTest {

    @Test
    @DisplayName("Un message de doublon contenant les données KBART est classé parmi les autres erreurs")
    void classifyDuplicateErrorAsOtherError() {
        String message = "Plusieurs ppn électroniques (290245540 OU 289331811) ont le même score. "
                + "[ publication title : Petite Histoire / publication_type : monograph ]";

        ErrorCategory category = ErrorClassifier.classify(message);

        assertEquals(ErrorCategory.AUTRE_ERREUR, category);
    }

    @Test
    @DisplayName("Le marqueur de doublon est reconnu quelle que soit sa position dans le message")
    void classifyErrorWhenDuplicateMarkerIsInsideMessage() {
        String message = "Préfixe technique - publication title : Un titre - détail de l'erreur";

        ErrorCategory category = ErrorClassifier.classify(message);

        assertEquals(ErrorCategory.AUTRE_ERREUR, category);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            "Format du fichier incorrect",
            "ISSN invalide à la ligne 12",
            "Colonne publication_title manquante",
            "Date de publication incorrecte"
    })
    @DisplayName("Un message sans marqueur de doublon est classé comme erreur 400")
    void classifyValidationErrorAs400Error(String message) {
        ErrorCategory category = ErrorClassifier.classify(message);

        assertEquals(ErrorCategory.ERREUR_400, category);
    }
}
