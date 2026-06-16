package com.Sharan.job_search_agent.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;


@Slf4j
@Component
public class URLValidator {


    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);


    private static final Set<String> BLOCKED_DOMAINS = Set.of(
            "malware-site.com",
            "phishing-example.com",
            "spam-jobs.net"
    );

    private static final Set<String> PRIVATE_IP_PREFIXES = Set.of(
            "10.", "172.16.", "172.17.", "172.18.", "172.19.",
            "172.20.", "172.21.", "172.22.", "172.23.", "172.24.",
            "172.25.", "172.26.", "172.27.", "172.28.", "172.29.",
            "172.30.", "172.31.",
            "192.168.", "127.", "169.254.", "0."
    );


    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(REQUEST_TIMEOUT)
            .build();

    public UrlValidationResult validate(String rawUrl) {

        if (rawUrl == null || rawUrl.isBlank()) {
            return UrlValidationResult.invalid(rawUrl, 0, "URL is null or empty");
        }

        URI uri;
        try {
            uri = URI.create(rawUrl.trim());
        } catch (IllegalArgumentException e) {
            log.warn("Malformed URL rejected: {}", rawUrl);
            return UrlValidationResult.invalid(rawUrl, 0, "Malformed URL");
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            log.warn("Invalid URL scheme rejected: {} | url: {}", scheme, rawUrl);
            return UrlValidationResult.invalid(rawUrl, 0,
                    "Only http and https URLs are allowed");
        }

        String host = uri.getHost();
        if (host == null) {
            return UrlValidationResult.invalid(rawUrl, 0, "URL has no host");
        }

        if (isPrivateOrLocalhost(host)) {
            log.error("SSRF attempt blocked — private/localhost URL in job data: {}", rawUrl);
            return UrlValidationResult.invalid(rawUrl, 0,
                    "URL points to private/internal network");
        }

        String domain = extractDomain(host);
        if (BLOCKED_DOMAINS.contains(domain)) {
            log.warn("Blocked domain rejected: {}", domain);
            return UrlValidationResult.invalid(rawUrl, 0,
                    "Domain is on the blocklist");
        }

        return performHeadCheck(rawUrl, uri);
    }

    private UrlValidationResult performHeadCheck(String rawUrl, URI uri) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .header("User-Agent", "JobSearchAgent/1.0 LinkValidator")
                    .timeout(REQUEST_TIMEOUT)
                    .build();

            HttpResponse<Void> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            );

            int statusCode = response.statusCode();

            String finalUrl = response.uri().toString();

            log.debug("URL HEAD check | status: {} | final: {}", statusCode, finalUrl);
            if (statusCode >= 200 && statusCode < 400) {
                return UrlValidationResult.valid(finalUrl, statusCode);
            }

            if (statusCode == 403 || statusCode == 405) {
                log.debug("Server blocked HEAD request ({}), treating as valid: {}", statusCode, rawUrl);
                return UrlValidationResult.valid(rawUrl, statusCode);
            }

            log.warn("URL returned error status {} : {}", statusCode, rawUrl);
            return UrlValidationResult.invalid(rawUrl, statusCode,
                    "URL returned HTTP " + statusCode);

        } catch (java.net.http.HttpTimeoutException e) {
            log.warn("URL validation timeout for: {}", rawUrl);
            return UrlValidationResult.valid(rawUrl, 0);

        } catch (Exception e) {
            log.warn("URL validation failed for {}: {}", rawUrl, e.getMessage());
            return UrlValidationResult.invalid(rawUrl, 0,
                    "Could not reach URL: " + e.getMessage());
        }
    }

    private boolean isPrivateOrLocalhost(String host) {


        if (host.equalsIgnoreCase("localhost") ||
                host.equals("0.0.0.0") ||
                host.endsWith(".local")) {
            return true;
        }

        for (String prefix : PRIVATE_IP_PREFIXES) {
            if (host.startsWith(prefix)) {
                return true;
            }
        }

        try {
            InetAddress address = InetAddress.getByName(host);
            String resolvedIp = address.getHostAddress();

            for (String prefix : PRIVATE_IP_PREFIXES) {
                if (resolvedIp.startsWith(prefix)) {
                    log.error("DNS rebinding attack prevented — {} resolved to private IP: {}",
                            host, resolvedIp);
                    return true;
                }
            }

            if (address.isLoopbackAddress() ||
                    address.isSiteLocalAddress() ||
                    address.isLinkLocalAddress() ||
                    address.isAnyLocalAddress()) {
                return true;
            }

        } catch (Exception e) {
            log.warn("DNS resolution failed for host {}, rejecting: {}", host, e.getMessage());
            return true;
        }

        return false;
    }

    private String extractDomain(String host) {
        String[] parts = host.split("\\.");
        if (parts.length >= 2) {
            return parts[parts.length - 2] + "." + parts[parts.length - 1];
        }
        return host;
    }


    public static class UrlValidationResult {

        private final boolean valid;
        private final String finalUrl;
        private final int httpStatus;
        private final String reason;

        private UrlValidationResult(
                boolean valid, String finalUrl, int httpStatus, String reason) {
            this.valid = valid;
            this.finalUrl = finalUrl;
            this.httpStatus = httpStatus;
            this.reason = reason;
        }

        public static UrlValidationResult valid(String finalUrl, int httpStatus) {
            return new UrlValidationResult(true, finalUrl, httpStatus, null);
        }

        public static UrlValidationResult invalid(
                String originalUrl, int httpStatus, String reason) {
            return new UrlValidationResult(false, originalUrl, httpStatus, reason);
        }

        public boolean isValid()      { return valid; }
        public String finalUrl()      { return finalUrl; }
        public int httpStatus()       { return httpStatus; }
        public String reason()        { return reason; }
    }
}