package fr.abes.logskbart.kafka;

public final class ErrorClassifier {

    private static final String OTHER_ERROR_MARKER = "publication title : ";

    private ErrorClassifier() {
    }

    public static ErrorCategory classify(String message) {
        return message != null && message.contains(OTHER_ERROR_MARKER)
                ? ErrorCategory.AUTRE_ERREUR
                : ErrorCategory.ERREUR_400;
    }
}
