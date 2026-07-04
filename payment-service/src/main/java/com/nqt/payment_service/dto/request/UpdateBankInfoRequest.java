package com.nqt.payment_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UpdateBankInfoRequest {
    @NotBlank(message = "Account holder name is required")
    private String accountHolderName;

    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "\\d{6,20}", message = "Account number must be 6-20 digits")
    private String accountNumber;

    @NotBlank(message = "Bank name is required")
    private String bankName;
}
