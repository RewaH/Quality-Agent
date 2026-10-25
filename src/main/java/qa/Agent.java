package qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Local Selenium QA runner for the login story. It requires no AI service,
 * network API key, or model account.
 */
public class Agent {

    static final Path OUT = Path.of("out");
    static final ObjectMapper JSON = new ObjectMapper();
    static final By USERNAME = By.cssSelector("[data-test='username']");
    static final By PASSWORD = By.cssSelector("[data-test='password']");
    static final By LOGIN = By.cssSelector("[data-test='login-button']");
    static final By ERROR = By.cssSelector("[data-test='error']");
    static final By ERROR_CLOSE = By.cssSelector("[data-test='error-button']");

    record TestCase(String id, String title, Consumer<WebDriver> action) {}
    record TestResult(String id, String title, boolean passed, String details) {}

    static void write(Path file, String content) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
        System.out.println("  wrote " + file);
    }

    static WebDriver newDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1366,900");
        return new ChromeDriver(options);
    }

    static WebDriverWait waitFor(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    static void login(WebDriver driver, String username, String password) {
        driver.findElement(USERNAME).sendKeys(username);
        driver.findElement(PASSWORD).sendKeys(password);
        driver.findElement(LOGIN).click();
    }

    static String errorText(WebDriver driver) {
        return waitFor(driver).until(ExpectedConditions.visibilityOfElementLocated(ERROR)).getText();
    }

    static List<TestCase> loginTests() {
        List<TestCase> tests = new ArrayList<>();
        tests.add(new TestCase("TC-01", "Valid credentials open the inventory", driver -> {
            login(driver, "standard_user", "secret_sauce");
            waitFor(driver).until(ExpectedConditions.urlContains("inventory.html"));
            require(driver.getCurrentUrl().contains("inventory.html"), "Expected inventory page after valid login");
        }));
        tests.add(new TestCase("TC-02", "Invalid password shows an error", driver -> {
            login(driver, "standard_user", "wrong_password");
            require(errorText(driver).contains("Username and password do not match"), "Expected invalid-credentials error");
            require(!driver.getCurrentUrl().contains("inventory.html"), "Invalid login must stay on the login page");
        }));
        tests.add(new TestCase("TC-03", "Locked-out user sees a locked message", driver -> {
            login(driver, "locked_out_user", "secret_sauce");
            require(errorText(driver).toLowerCase().contains("locked out"), "Expected locked-out account error");
        }));
        tests.add(new TestCase("TC-04", "Empty username shows a required error", driver -> {
            login(driver, "", "secret_sauce");
            require(errorText(driver).contains("Username is required"), "Expected username-required error");
        }));
        tests.add(new TestCase("TC-05", "Empty password shows a required error", driver -> {
            login(driver, "standard_user", "");
            require(errorText(driver).contains("Password is required"), "Expected password-required error");
        }));
        tests.add(new TestCase("TC-06", "Login error can be dismissed", driver -> {
            login(driver, "", "secret_sauce");
            errorText(driver);
            driver.findElement(ERROR_CLOSE).click();
            waitFor(driver).until(ExpectedConditions.invisibilityOfElementLocated(ERROR));
        }));
        return tests;
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static String inventory(WebDriver driver) throws Exception {
        String script = """
                return JSON.stringify(Array.from(document.querySelectorAll(
                  'input,button,select,textarea,a,[role=button],h1,h2,h3,label,[data-test],[data-testid]'))
                  .filter(e => { const r = e.getBoundingClientRect(); return r.width > 0 || r.height > 0; })
                  .map(e => ({tag:e.tagName.toLowerCase(), id:e.id || null,
                    name:e.getAttribute('name'), dataTest:e.getAttribute('data-test') || e.getAttribute('data-testid'),
                    type:e.getAttribute('type'), text:(e.innerText || e.value || '').trim().slice(0,80)})));
                """;
        String raw = (String) ((JavascriptExecutor) driver).executeScript(script);
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(JSON.readTree(raw));
    }

    static TestResult runTest(String url, TestCase test) {
        WebDriver driver = null;
        try {
            driver = newDriver();
            driver.get(url);
            test.action().accept(driver);
            return new TestResult(test.id(), test.title(), true, "Passed");
        } catch (Exception | AssertionError error) {
            String details = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
            if (driver != null) {
                try {
                    Path screenshot = OUT.resolve("screenshots").resolve(test.id() + ".png");
                    Files.createDirectories(screenshot.getParent());
                    Files.write(screenshot, ((org.openqa.selenium.TakesScreenshot) driver)
                            .getScreenshotAs(org.openqa.selenium.OutputType.BYTES));
                } catch (Exception screenshotError) {
                    details += " (screenshot could not be saved: " + screenshotError.getMessage() + ")";
                }
            }
            return new TestResult(test.id(), test.title(), false, details);
        } finally {
            if (driver != null) driver.quit();
        }
    }

    static String planJson(List<TestCase> tests) throws Exception {
        List<String> plan = tests.stream().map(test -> "{\"id\":\"" + test.id()
                + "\",\"title\":\"" + test.title() + "\"}").toList();
        return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(JSON.readTree("[" + String.join(",", plan) + "]"));
    }

    static String report(List<TestResult> results) {
        long passed = results.stream().filter(TestResult::passed).count();
        StringBuilder report = new StringBuilder("# Local login test report\n\n");
        report.append("Results: ").append(passed).append(" passed, ")
                .append(results.size() - passed).append(" failed.\n\n");
        for (TestResult result : results) {
            report.append("## ").append(result.id()).append(" — ").append(result.title()).append("\n\n")
                    .append("**Status:** ").append(result.passed() ? "PASS" : "FAIL")
                    .append("\n\n").append(result.details()).append("\n\n");
        }
        return report.toString();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: mvn compile exec:java -Dexec.args=\"<story.md> <page url>\"");
            System.exit(1);
        }

        Path storyPath = Path.of(args[0]);
        if (!Files.isRegularFile(storyPath)) {
            System.err.println("Story file not found: " + storyPath);
            System.exit(1);
        }
        String story = Files.readString(storyPath).toLowerCase();
        if (!story.contains("login")) {
            System.err.println("This local runner currently supports login stories only.");
            System.exit(1);
        }
        String url = args[1];
        List<TestCase> tests = loginTests();

        System.out.println("1/3 Inspecting page elements with Selenium...");
        WebDriver driver = newDriver();
        try {
            driver.get(url);
            write(OUT.resolve("element-inventory.json"), inventory(driver));
        } finally {
            driver.quit();
        }

        System.out.println("2/3 Saving the local login test plan...");
        write(OUT.resolve("test-plan.json"), planJson(tests));

        System.out.println("3/3 Running local Selenium login checks...");
        List<TestResult> results = new ArrayList<>();
        for (TestCase test : tests) {
            TestResult result = runTest(url, test);
            results.add(result);
            System.out.println("  " + result.id() + " " + (result.passed() ? "PASS" : "FAIL") + " — " + result.title());
        }
        write(OUT.resolve("triage-report.md"), report(results));
        System.out.println("\nDone. Review the local results in ./out.");
    }
}