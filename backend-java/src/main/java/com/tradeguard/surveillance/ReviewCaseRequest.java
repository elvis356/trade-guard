package com.tradeguard.surveillance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewCaseRequest(boolean approve,
                               @NotBlank @Size(max = 500) String note) {
}
