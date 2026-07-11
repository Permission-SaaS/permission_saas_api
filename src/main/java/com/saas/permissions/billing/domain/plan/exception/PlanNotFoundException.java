package com.saas.permissions.billing.domain.plan.exception;

import com.saas.permissions.shared.domain.exception.ResourceNotFoundException;

public class PlanNotFoundException extends ResourceNotFoundException {
    public PlanNotFoundException() {
        super("Plan not found");
    }
}
