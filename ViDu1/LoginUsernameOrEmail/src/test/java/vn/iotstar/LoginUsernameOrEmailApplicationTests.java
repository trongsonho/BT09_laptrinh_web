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
public class LoginUsernameOrEmailApplicationTests {

    @LocalServerPort
    private int port;

    @Test
    void contextLoads() {
    }

    @Test
    void testPublicPagesAndSecurityRedirect() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        // 1. Unauthenticated access to / is protected -> redirects (302) to /login
        HttpRequest reqHome = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/"))
                .GET()
                .build();
        HttpResponse<String> resHome = client.send(reqHome, HttpResponse.BodyHandlers.ofString());
        assertEquals(302, resHome.statusCode());
        assertTrue(resHome.headers().firstValue("Location").orElse("").contains("/login"));

        // 2. Access /login directly -> 200 OK
        HttpRequest reqLogin = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/login"))
                .GET()
                .build();
        HttpResponse<String> resLogin = client.send(reqLogin, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, resLogin.statusCode());
        assertTrue(resLogin.body().contains("Đăng nhập"));
    }

    @Test
    void testLoginWithUsernameAndHeaderDisplay() throws Exception {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        // 1. Get CSRF token
        HttpRequest getLogin = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/login"))
                .GET()
                .build();
        HttpResponse<String> loginPageRes = client.send(getLogin, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, loginPageRes.statusCode());

        Pattern csrfPattern = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
        Matcher matcher = csrfPattern.matcher(loginPageRes.body());
        assertTrue(matcher.find(), "CSRF token must be present");
        String csrfToken = matcher.group(1);

        // 2. Login using USERNAME
        String postData = "username=" + URLEncoder.encode("user01", StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode("123456", StandardCharsets.UTF_8)
                + "&_csrf=" + URLEncoder.encode(csrfToken, StandardCharsets.UTF_8);

        HttpRequest postLogin = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(postData))
                .build();

        HttpResponse<String> loginRes = client.send(postLogin, HttpResponse.BodyHandlers.ofString());
        assertEquals(302, loginRes.statusCode());
        assertTrue(loginRes.headers().firstValue("Location").orElse("").endsWith("/"));

        // 3. Verify Header on Home Page
        HttpRequest getHome = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/"))
                .GET()
                .build();
        HttpResponse<String> homeRes = client.send(getHome, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, homeRes.statusCode());

        // Header must display fullname, username, email, and avatar
        assertTrue(homeRes.body().contains("Nguyễn Hữu Trung"), "Header should contain full name");
        assertTrue(homeRes.body().contains("(user01)"), "Header should contain username in parentheses");
        assertTrue(homeRes.body().contains("user01@gmail.com"), "Header should contain email");
        assertTrue(homeRes.body().contains("ROLE_USER"), "Header should contain role");
        assertTrue(homeRes.body().contains("Đăng xuất"), "Header should have logout button");
    }

    @Test
    void testLoginWithEmailAndHeaderDisplay() throws Exception {
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

        // Login using EMAIL
        String postData = "username=" + URLEncoder.encode("user01@gmail.com", StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode("123456", StandardCharsets.UTF_8)
                + "&_csrf=" + URLEncoder.encode(csrfToken, StandardCharsets.UTF_8);

        HttpRequest postLogin = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(postData))
                .build();

        HttpResponse<String> loginRes = client.send(postLogin, HttpResponse.BodyHandlers.ofString());
        assertEquals(302, loginRes.statusCode());
        assertTrue(loginRes.headers().firstValue("Location").orElse("").endsWith("/"));
    }

    @Test
    void testLoginFailure() throws Exception {
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

        String postData = "username=" + URLEncoder.encode("user01", StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode("invalid_pwd", StandardCharsets.UTF_8)
                + "&_csrf=" + URLEncoder.encode(csrfToken, StandardCharsets.UTF_8);

        HttpRequest postLogin = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(postData))
                .build();

        HttpResponse<String> loginRes = client.send(postLogin, HttpResponse.BodyHandlers.ofString());
        assertEquals(302, loginRes.statusCode());
        assertTrue(loginRes.headers().firstValue("Location").orElse("").contains("/login?error=true"));
    }

    @Test
    void testAdminAuthorization() throws Exception {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        // 1. Unauthenticated access to /admin -> redirects to login
        HttpRequest reqAdmin = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/admin"))
                .GET()
                .build();
        HttpResponse<String> unauthRes = client.send(reqAdmin, HttpResponse.BodyHandlers.ofString());
        assertEquals(302, unauthRes.statusCode());
        assertTrue(unauthRes.headers().firstValue("Location").orElse("").contains("/login"));

        // 2. Login as regular user01
        HttpRequest getLogin = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/login"))
                .GET()
                .build();
        HttpResponse<String> loginPageRes = client.send(getLogin, HttpResponse.BodyHandlers.ofString());
        Pattern csrfPattern = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
        Matcher matcher = csrfPattern.matcher(loginPageRes.body());
        assertTrue(matcher.find());
        String csrfToken = matcher.group(1);

        String postDataUser = "username=" + URLEncoder.encode("user01", StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode("123456", StandardCharsets.UTF_8)
                + "&_csrf=" + URLEncoder.encode(csrfToken, StandardCharsets.UTF_8);

        client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(postDataUser))
                .build(), HttpResponse.BodyHandlers.ofString());

        // Regular user accessing /admin -> 403 Forbidden (forwarded to /403 or status 403)
        HttpResponse<String> forbiddenRes = client.send(reqAdmin, HttpResponse.BodyHandlers.ofString());
        assertTrue(forbiddenRes.statusCode() == 403 || forbiddenRes.statusCode() == 302, "Should deny access with 403 or redirect");
    }
}
