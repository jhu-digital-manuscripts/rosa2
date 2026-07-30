package rosa.archive.core;

import java.util.List;

/**
 * Result of a book consistency check, containing any errors and warnings found.
 *
 * @param errors   list of error messages (structural problems, missing data)
 * @param warnings list of warning messages (non-critical issues)
 * @param passed   whether the check passed without errors
 */
public record CheckResult(List<String> errors, List<String> warnings, boolean passed) {

    /**
     * Creates a passing result with no errors or warnings.
     *
     * @return an empty passing result
     */
    public static CheckResult passing() {
        return new CheckResult(List.of(), List.of(), true);
    }

    /**
     * Creates a failing result from the given errors and warnings.
     *
     * @param errors   error messages
     * @param warnings warning messages
     * @return a result that reflects whether errors were found
     */
    public static CheckResult of(List<String> errors, List<String> warnings) {
        return new CheckResult(
                List.copyOf(errors),
                List.copyOf(warnings),
                errors.isEmpty()
        );
    }
}
