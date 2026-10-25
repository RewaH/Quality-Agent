package generated;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Hand-written, human-owned foundation. The agent only generates test classes
 * that extend this, so driver setup, waits, and failure screenshots stay under
 * the engineer's control (and out of the model's hands).
 */
public abstract class BaseTest {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected String baseUrl = System.getProperty("baseUrl", "https://www.saucedemo.com");

    @BeforeMethod
    public void setUp() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1366,900");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        driver.get(baseUrl);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        try {
            if (driver != null && result.getStatus() == ITestResult.FAILURE) {
                File shot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
                Path dest = Path.of("out", "screenshots", result.getName() + ".png");
                Files.createDirectories(dest.getParent());
                Files.copy(shot.toPath(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ignored) {
            // never let screenshot problems mask the real failure
        } finally {
            if (driver != null) driver.quit();
        }
    }
}
