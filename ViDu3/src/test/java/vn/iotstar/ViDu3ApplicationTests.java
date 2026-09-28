package vn.iotstar;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import vn.iotstar.entity.OtpToken;
import vn.iotstar.entity.Product;
import vn.iotstar.entity.User;
import vn.iotstar.repository.OtpTokenRepository;
import vn.iotstar.repository.ProductRepository;
import vn.iotstar.repository.UserRepository;

import java.math.BigDecimal;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ViDu3ApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OtpTokenRepository otpTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String extractCsrf(String html) {
        Pattern pattern = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalStateException("CSRF token not found in response HTML");
    }

    @Test
    void testPublicEndpoints() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        for (String path : new String[]{"/", "/login", "/register", "/forgot-password", "/verify-otp", "/reset-password"}) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + port + path))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), "Path " + path + " should be publicly accessible");
        }
    }

    @Test
    void testRegistrationAndOtpVerificationFlow() throws Exception {
        CookieManager cm = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder().cookieHandler(cm).followRedirects(HttpClient.Redirect.NEVER).build();

        // 1. Get register page to obtain CSRF token
        HttpRequest getReg = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/register")).GET().build();
        HttpResponse<String> regPage = client.send(getReg, HttpResponse.BodyHandlers.ofString());
        String csrf = extractCsrf(regPage.body());

        // 2. Submit registration
        String username = "new_student_" + System.currentTimeMillis();
        String email = username + "@example.com";
        String regData = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                + "&email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&fullName=" + URLEncoder.encode("Sinh Viên Mới", StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode("123456", StandardCharsets.UTF_8)
                + "&confirmPassword=" + URLEncoder.encode("123456", StandardCharsets.UTF_8)
                + "&_csrf=" + URLEncoder.encode(csrf, StandardCharsets.UTF_8);

        HttpRequest postReg = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/register"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(regData))
                .build();
        HttpResponse<String> regRes = client.send(postReg, HttpResponse.BodyHandlers.ofString());
        assertEquals(302, regRes.statusCode());
        assertTrue(regRes.headers().firstValue("Location").orElse("").contains("/verify-otp"));

        // 3. User is initially NOT enabled
        User savedUser = userRepository.findByEmail(email).orElse(null);
        assertNotNull(savedUser);
        assertFalse(savedUser.isEnabled(), "Newly registered user must not be enabled until OTP is verified");

        // 4. Verify OTP Token created in database
        OtpToken token = otpTokenRepository.findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc(email, "REGISTER").orElse(null);
        assertNotNull(token);
        assertFalse(token.isUsed());
        assertEquals(0, token.getAttempts());

        // Test invalid OTP attempt
        HttpRequest getVerify = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/verify-otp?email=" + email)).GET().build();
        HttpResponse<String> verifyPage = client.send(getVerify, HttpResponse.BodyHandlers.ofString());
        String verifyCsrf = extractCsrf(verifyPage.body());

        String badVerifyData = "email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&otp=999999"
                + "&_csrf=" + URLEncoder.encode(verifyCsrf, StandardCharsets.UTF_8);
        HttpResponse<String> badVerifyRes = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/verify-otp"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(badVerifyData))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertTrue(badVerifyRes.body().contains("OTP không hợp lệ"));

        // Refresh token from DB to verify attempt was tracked
        token = otpTokenRepository.findById(token.getId()).orElseThrow();
        assertEquals(1, token.getAttempts(), "Attempts counter should increment on failed verification");

        // 5. Test valid OTP verification using a known OTP
        String knownOtp = "123456";
        token.setOtpHash(passwordEncoder.encode(knownOtp));
        otpTokenRepository.save(token);

        String goodVerifyData = "email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&otp=" + knownOtp
                + "&_csrf=" + URLEncoder.encode(verifyCsrf, StandardCharsets.UTF_8);
        HttpResponse<String> goodVerifyRes = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/verify-otp"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(goodVerifyData))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(302, goodVerifyRes.statusCode());
        assertTrue(goodVerifyRes.headers().firstValue("Location").orElse("").contains("/login"));

        // 6. User should now be ENABLED!
        User verifiedUser = userRepository.findByEmail(email).orElseThrow();
        assertTrue(verifiedUser.isEnabled(), "User must be enabled after OTP verification");
    }

    @Test
    void testForgotPasswordAndResetPasswordFlow() throws Exception {
        CookieManager cm = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder().cookieHandler(cm).followRedirects(HttpClient.Redirect.NEVER).build();

        // 1. Submit forgot password for existing user "trungnh@hcmute.edu.vn"
        HttpRequest getForgot = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/forgot-password")).GET().build();
        String csrf = extractCsrf(client.send(getForgot, HttpResponse.BodyHandlers.ofString()).body());

        String forgotData = "email=" + URLEncoder.encode("trungnh@hcmute.edu.vn", StandardCharsets.UTF_8)
                + "&_csrf=" + URLEncoder.encode(csrf, StandardCharsets.UTF_8);
        HttpResponse<String> forgotRes = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/forgot-password"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(forgotData))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(302, forgotRes.statusCode());
        assertTrue(forgotRes.headers().firstValue("Location").orElse("").contains("/reset-password"));

        // 2. Fetch reset token from database and configure a known OTP
        OtpToken token = otpTokenRepository.findTopByEmailAndTypeAndUsedFalseOrderByCreatedAtDesc("trungnh@hcmute.edu.vn", "RESET_PASSWORD").orElseThrow();
        String resetOtp = "654321";
        token.setOtpHash(passwordEncoder.encode(resetOtp));
        otpTokenRepository.save(token);

        // 3. Submit reset password
        HttpRequest getReset = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/reset-password")).GET().build();
        String resetCsrf = extractCsrf(client.send(getReset, HttpResponse.BodyHandlers.ofString()).body());

        String resetData = "email=" + URLEncoder.encode("trungnh@hcmute.edu.vn", StandardCharsets.UTF_8)
                + "&otp=" + resetOtp
                + "&password=newPassword123"
                + "&confirmPassword=newPassword123"
                + "&_csrf=" + URLEncoder.encode(resetCsrf, StandardCharsets.UTF_8);

        HttpResponse<String> resetRes = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/reset-password"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(resetData))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(302, resetRes.statusCode());
        assertTrue(resetRes.headers().firstValue("Location").orElse("").contains("/login"));

        // 4. Verify user can now log in with the new password
        HttpRequest getLogin = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/login")).GET().build();
        String loginCsrf = extractCsrf(client.send(getLogin, HttpResponse.BodyHandlers.ofString()).body());

        String loginData = "username=" + URLEncoder.encode("trungnh", StandardCharsets.UTF_8)
                + "&password=newPassword123"
                + "&_csrf=" + URLEncoder.encode(loginCsrf, StandardCharsets.UTF_8);
        HttpResponse<String> loginRes = client.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(loginData))
                .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(302, loginRes.statusCode());
        assertTrue(loginRes.headers().firstValue("Location").orElse("").endsWith("/"));
    }

    @Test
    void testUserManagementRoleProtection() throws Exception {
        CookieManager cmUser = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient clientUser = HttpClient.newBuilder().cookieHandler(cmUser).followRedirects(HttpClient.Redirect.NEVER).build();

        // 1. Regular user login (user02 / 123456)
        HttpRequest getLogin = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/login")).GET().build();
        String csrf = extractCsrf(clientUser.send(getLogin, HttpResponse.BodyHandlers.ofString()).body());

        String loginData = "username=user02&password=123456&_csrf=" + URLEncoder.encode(csrf, StandardCharsets.UTF_8);
        clientUser.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(loginData))
                .build(), HttpResponse.BodyHandlers.ofString());

        // Regular user accessing /users -> 403 Forbidden
        HttpRequest getUsers = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/users")).GET().build();
        HttpResponse<String> userAccessRes = clientUser.send(getUsers, HttpResponse.BodyHandlers.ofString());
        assertTrue(userAccessRes.statusCode() == 403 || userAccessRes.statusCode() == 302, "Non-admin must not access /users");

        // 2. Admin login (admin / 123456)
        CookieManager cmAdmin = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient clientAdmin = HttpClient.newBuilder().cookieHandler(cmAdmin).followRedirects(HttpClient.Redirect.NEVER).build();

        String adminCsrf = extractCsrf(clientAdmin.send(getLogin, HttpResponse.BodyHandlers.ofString()).body());
        String adminLoginData = "username=admin&password=123456&_csrf=" + URLEncoder.encode(adminCsrf, StandardCharsets.UTF_8);
        clientAdmin.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(adminLoginData))
                .build(), HttpResponse.BodyHandlers.ofString());

        // Admin accessing /users -> 200 OK
        HttpResponse<String> adminAccessRes = clientAdmin.send(getUsers, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, adminAccessRes.statusCode());
        assertTrue(adminAccessRes.body().contains("Quản lý Users"));
    }

    @Test
    void testProductOwnershipRuleAtServer() throws Exception {
        // User trungnh owns products 1 and 2.
        // User user02 owns product 3.
        CookieManager cmUser2 = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient clientUser2 = HttpClient.newBuilder().cookieHandler(cmUser2).followRedirects(HttpClient.Redirect.NEVER).build();

        // 1. Login as user02
        HttpRequest getLogin = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/login")).GET().build();
        String csrf = extractCsrf(clientUser2.send(getLogin, HttpResponse.BodyHandlers.ofString()).body());

        String loginData = "username=user02&password=123456&_csrf=" + URLEncoder.encode(csrf, StandardCharsets.UTF_8);
        clientUser2.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(loginData))
                .build(), HttpResponse.BodyHandlers.ofString());

        // Find a product owned by trungnh
        User trungnh = userRepository.findByUsername("trungnh").orElseThrow();
        Product productOfTrung = productRepository.findAll().stream()
                .filter(p -> p.getUser().getId().equals(trungnh.getId()))
                .findFirst().orElseThrow();

        // 2. user02 tries to GET /products/edit/{id} of trungnh's product -> blocked
        HttpRequest getEditOther = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/products/edit/" + productOfTrung.getId()))
                .GET().build();
        HttpResponse<String> editOtherRes = clientUser2.send(getEditOther, HttpResponse.BodyHandlers.ofString());
        assertEquals(302, editOtherRes.statusCode(), "Server must redirect/block editing other user's product");
        assertTrue(editOtherRes.headers().firstValue("Location").orElse("").endsWith("/products"));

        // 3. user02 tries to POST /products/delete/{id} of trungnh's product -> blocked
        HttpRequest getProducts = HttpRequest.newBuilder().uri(URI.create("http://localhost:" + port + "/products")).GET().build();
        String postLoginCsrf = extractCsrf(clientUser2.send(getProducts, HttpResponse.BodyHandlers.ofString()).body());
        String delData = "_csrf=" + URLEncoder.encode(postLoginCsrf, StandardCharsets.UTF_8);
        HttpRequest postDeleteOther = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/products/delete/" + productOfTrung.getId()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(delData))
                .build();
        HttpResponse<String> delOtherRes = clientUser2.send(postDeleteOther, HttpResponse.BodyHandlers.ofString());
        assertEquals(302, delOtherRes.statusCode());
        assertTrue(productRepository.existsById(productOfTrung.getId()), "Product must NOT be deleted by non-owner");

        // 4. Admin CAN delete or edit productOfTrung
        CookieManager cmAdmin = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient clientAdmin = HttpClient.newBuilder().cookieHandler(cmAdmin).followRedirects(HttpClient.Redirect.NEVER).build();

        String adminCsrf = extractCsrf(clientAdmin.send(getLogin, HttpResponse.BodyHandlers.ofString()).body());
        clientAdmin.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("username=admin&password=123456&_csrf=" + URLEncoder.encode(adminCsrf, StandardCharsets.UTF_8)))
                .build(), HttpResponse.BodyHandlers.ofString());

        HttpRequest adminGetEdit = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/products/edit/" + productOfTrung.getId()))
                .GET().build();
        HttpResponse<String> adminEditRes = clientAdmin.send(adminGetEdit, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, adminEditRes.statusCode(), "Admin must have permission to edit any product");
    }
}
