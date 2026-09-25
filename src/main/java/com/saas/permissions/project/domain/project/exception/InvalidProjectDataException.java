package com.saas.permissions.project.domain.project.exception;

import com.saas.permissions.shared.domain.exception.InvalidDataException;

public class InvalidProjectDataException extends InvalidDataException {
    public InvalidProjectDataException(String field, String reason) {
        super("Invalid project data: '" + field + "' " + reason);
    }
}
