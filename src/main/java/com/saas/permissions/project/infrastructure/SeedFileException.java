package com.saas.permissions.project.infrastructure;

public class SeedFileException extends RuntimeException {

    public SeedFileException(String fileName) {
        super("Seed file not found: " + fileName);
    }

    public SeedFileException(String fileName, Throwable cause) {
        super("Failed to read seed file: " + fileName, cause);
    }

    public SeedFileException(String fileName, String line, int expectedFields, int actualFields) {
        super("Seed file " + fileName + " expects " + expectedFields + " fields but found "
                + actualFields + " in line: " + line);
    }

    public SeedFileException(int emptyColumn, String line) {
        super("Empty required field at column " + emptyColumn + " in line: " + line);
    }
}
