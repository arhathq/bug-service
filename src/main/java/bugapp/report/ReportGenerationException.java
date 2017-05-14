package bugapp.report;

/**
 * Exception that can be thrown during report generation
 */
class ReportGenerationException extends RuntimeException {
    /**
     *
     */
    ReportGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
