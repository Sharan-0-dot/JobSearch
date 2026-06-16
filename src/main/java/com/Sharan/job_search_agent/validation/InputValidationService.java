package com.Sharan.job_search_agent.validation;


import io.micrometer.core.instrument.Counter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;


@Slf4j
@Service
public class InputValidationService {


    private static final int MAX_INPUT_LENGTH = 2000;

    private static final Pattern INSTRUCTION_OVERRIDE_PATTERN = Pattern.compile(
            "(?i)(ignore|disregard|forget|override|bypass|skip)\\s+" +
                    "(previous|prior|above|all|your|the)\\s+" +
                    "(instructions?|prompts?|rules?|constraints?|guidelines?|context)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ROLE_HIJACKING_PATTERN = Pattern.compile(
            "(?i)(you are now|act as|pretend (to be|you are)|" +
                    "your (new )?role is|you must now|from now on you|" +
                    "new instructions?|your new (instructions?|persona|identity))",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SYSTEM_PROMPT_EXTRACTION_PATTERN = Pattern.compile(
            "(?i)(reveal|show|print|display|output|repeat|tell me|what (is|are))\\s+" +
                    "(your )?(system (prompt|message)|instructions?|" +
                    "initial prompt|base prompt|hidden (prompt|instructions?))",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern JAILBREAK_PATTERN = Pattern.compile(
            "(?i)(jailbreak|dan mode|developer mode|unrestricted mode|" +
                    "no restrictions|without restrictions|ignore (all )?filters|" +
                    "disable (safety|filter|restriction)|evil (mode|ai)|" +
                    "do anything now)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern CODE_INJECTION_PATTERN = Pattern.compile(
            "(?i)(<script|</script|javascript:|on\\w+\\s*=|" +
                    "DROP\\s+TABLE|DELETE\\s+FROM|INSERT\\s+INTO|" +
                    "UNION\\s+SELECT|exec\\s*\\(|eval\\s*\\()",
            Pattern.CASE_INSENSITIVE
    );

    private static final List<PatternRule> INJECTION_RULES = List.of(
            new PatternRule(INSTRUCTION_OVERRIDE_PATTERN,  "Instruction override attempt detected"),
            new PatternRule(ROLE_HIJACKING_PATTERN,        "Role hijacking attempt detected"),
            new PatternRule(SYSTEM_PROMPT_EXTRACTION_PATTERN, "System prompt extraction attempt detected"),
            new PatternRule(JAILBREAK_PATTERN,             "Jailbreak attempt detected"),
            new PatternRule(CODE_INJECTION_PATTERN,        "Code injection attempt detected")
    );

    private final Counter validationFailureCounter;

    public InputValidationService(
            @Qualifier("validationFailureCounter") Counter validationFailureCounter) {
        this.validationFailureCounter = validationFailureCounter;
    }

    public ValidationResult validate(String input) {

        if (input == null || input.isBlank()) {
            return ValidationResult.fail("Input cannot be empty");
        }

        if (input.length() > MAX_INPUT_LENGTH) {
            log.warn("Input too long: {} chars (max {})", input.length(), MAX_INPUT_LENGTH);
            recordFailure("input_too_long");
            return ValidationResult.fail(
                    "Input too long. Maximum " + MAX_INPUT_LENGTH + " characters allowed.");
        }

        for (PatternRule rule : INJECTION_RULES) {
            if (rule.pattern().matcher(input).find()) {
                log.warn("Prompt injection detected — reason: {} | input preview: {}",
                        rule.reason(),
                        input.substring(0, Math.min(input.length(), 100)));

                recordFailure("prompt_injection");
                return ValidationResult.fail(
                        "Your input contains patterns that cannot be processed. " +
                                "Please rephrase your job search query.");
            }
        }

        String sanitized = sanitize(input);
        return ValidationResult.pass(sanitized);
    }


    private String sanitize(String input) {
        return input
                .trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[\\p{Cntrl}]", "");
    }

    private void recordFailure(String reason) {
        validationFailureCounter.increment();
        log.debug("Validation failure recorded | reason: {}", reason);
    }


    private record PatternRule(Pattern pattern, String reason) {}

    public static class ValidationResult {

        private final boolean valid;
        private final String sanitizedInput;
        private final String rejectionReason;

        private ValidationResult(boolean valid, String sanitizedInput, String rejectionReason) {
            this.valid = valid;
            this.sanitizedInput = sanitizedInput;
            this.rejectionReason = rejectionReason;
        }

        public static ValidationResult pass(String sanitizedInput) {
            return new ValidationResult(true, sanitizedInput, null);
        }

        public static ValidationResult fail(String reason) {
            return new ValidationResult(false, null, reason);
        }

        public boolean isValid()            { return valid; }
        public String sanitizedInput()      { return sanitizedInput; }
        public String rejectionReason()     { return rejectionReason; }
    }
}