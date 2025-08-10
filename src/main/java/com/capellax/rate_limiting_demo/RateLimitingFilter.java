    package com.capellax.rate_limiting_demo;

    import io.github.bucket4j.Bandwidth;
    import io.github.bucket4j.Bucket;
    import io.github.bucket4j.Refill;
    import jakarta.servlet.*;
    import jakarta.servlet.http.HttpServletRequest;
    import jakarta.servlet.http.HttpServletResponse;
    import org.springframework.http.HttpStatus;
    import org.springframework.stereotype.Component;

    import java.io.IOException;
    import java.time.Duration;
    import java.util.concurrent.ConcurrentHashMap;

    @Component
    public class RateLimitingFilter implements Filter {

        private final ConcurrentHashMap<String, Bucket> cache = new ConcurrentHashMap<>();

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String clientIp = httpRequest.getRemoteAddr();

            Bucket bucket = cache.computeIfAbsent(clientIp, this::createNewBucket);

            if (bucket.tryConsume(1)) {
                chain.doFilter(request, response);
            } else {
                httpResponse.setContentType("application/json");
                httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                httpResponse.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
                httpResponse.getWriter().flush();
            }
        }

        private Bucket createNewBucket(String clientIp) {
            Bandwidth limit = Bandwidth.classic(15, Refill.intervally(10, Duration.ofSeconds(1)));
            return Bucket.builder()
                    .addLimit(limit)
                    .build();
        }
    }
