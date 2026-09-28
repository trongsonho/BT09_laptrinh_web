package vn.iotstar;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class LoginEmailApplicationTests {

    @LocalServerPort
    private int port;

    @Test
    void contextLoads() {
    }

    @Test
    void testPublicLoginAndHome() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest reqHome = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/"))
                .GET()
                .build();
        HttpResponse<String> resHome = client.send(reqHome, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resHome.statusCode());
        assertTrue(resHome.body().contains("Trang chủ"));

        HttpRequest reqLogin = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/login"))
                .GET()
                .build();
        HttpResponse<String> resLogin = client.send(reqLogin, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resLogin.statusCode());
        assertTrue(resLogin.body().contains("Đăng nhập"));
    }

    @Test
    void testUnauthenticatedAccessToDashboardRedirectsToLogin() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/dashboard"))
                .GET()
                .build();
        HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString());
        assertEquals(302, res.statusCode());
        assertTrue(res.headers().firstValue("Location").orElse("").contains("/login"));
    }

    @Test
    void testLoginWithAdminCredentialsAndAccessDashboard() throws Exception {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        // 1. Get login page to retrieve CSRF token
        HttpRequest getLogin = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/login"))
                .GET()
                .build();
        HttpResponse<String> loginPageRes = client.send(getLogin, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, loginPageRes.statusCode());

        // Extract CSRF token
        Pattern csrfPattern = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
        Matcher matcher = csrfPattern.matcher(loginPageRes.body());
        assertTrue(matcher.find(), "CSRF token should be found in login form");
        String csrfToken = matcher.group(1);

        // 2. Perform POST /login with admin credentials
        String postData = "email=" + URLEncoder.encode("trungnh@hcmute.edu.vn", StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode("123456", StandardCharsets.UTF_8)
                + "&_csrf=" + URLEncoder.encode(csrfToken, StandardCharsets.UTF_8);

        HttpRequest postLogin = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(postData))
                .build();

        HttpResponse<String> loginPostRes = client.send(postLogin, HttpResponse.BodyHandlers.ofString());
        assertEquals(302, loginPostRes.statusCode());
        assertTrue(loginPostRes.headers().firstValue("Location").orElse("").contains("/dashboard"));

        // 3. Access dashboard with the authenticated session
        HttpRequest getDashboard = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/dashboard"))
                .GET()
                .build();
        HttpResponse<String> dashboardRes = client.send(getDashboard, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, dashboardRes.statusCode());
        assertTrue(dashboardRes.body().contains("Admin Dashboard"));
        assertTrue(dashboardRes.body().contains("trungnh@hcmute.edu.vn"));
    }

    @Test
    void testLoginWithWrongPasswordFails() throws Exception {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        HttpRequest getLogin = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/login"))
                .GET()
                .build();
        HttpResponse<String> loginPageRes = client.send(getLogin, HttpResponse.BodyHandlers.ofString());

        Pattern csrfPattern = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
        Matcher matcher = csrfPattern.matcher(loginPageRes.body());
        assertTrue(matcher.find());
        String csrfToken = matcher.group(1);

        String postData = "email=" + URLEncoder.encode("trungnh@hcmute.edu.vn", StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode("wrong_password", StandardCharsets.UTF_8)
                + "&_csrf=" + URLEncoder.encode(csrfToken, StandardCharsets.UTF_8);

        HttpRequest postLogin = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(postData))
                .build();

        HttpResponse<String> loginPostRes = client.send(postLogin, HttpResponse.BodyHandlers.ofString());
        assertEquals(302, loginPostRes.statusCode());
        assertTrue(loginPostRes.headers().firstValue("Location").orElse("").contains("/login?error=true"));
    }
}
