import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class OracleFSCMHelper {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(45);
    private final int RETRY_ATTEMPTS = 3;
    private final int RETRY_DELAY_MS = 1500;

    public OracleFSCMHelper(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, DEFAULT_TIMEOUT);
    }

    /**
     * Clicks an element with retries and waits for loading.
     */
    public void clickElementAndHandleLoading(By locator) {
        boolean success = false;
        int attempts = 0;
        while (!success && attempts < RETRY_ATTEMPTS) {
            try {
                WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
                scrollIntoView(element);
                element.click();
                waitForLoadingToFinish();
                success = true;
            } catch (StaleElementReferenceException | ElementClickInterceptedException | TimeoutException e) {
                attempts++;
                sleep(RETRY_DELAY_MS);
                if (attempts == RETRY_ATTEMPTS) {
                    throw new RuntimeException("Failed to click element after retries: " + locator, e);
                }
            }
        }
    }

    /**
     * Enters text into an input field with retries and waits for refresh.
     */
    public void enterTextAndHandleRefresh(By locator, String text) {
        int attempts = 0;
        boolean success = false;

        while (!success && attempts < RETRY_ATTEMPTS) {
            try {
                WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
                scrollIntoView(input);
                input.click();
                input.clear();
                input.sendKeys(text);

                if (!input.getAttribute("value").equals(text)) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].value = arguments[1];", input, text);
                }

                waitForLoadingToFinish();
                success = true;
            } catch (StaleElementReferenceException | TimeoutException e) {
                attempts++;
                sleep(RETRY_DELAY_MS);
                if (attempts == RETRY_ATTEMPTS) {
                    throw new RuntimeException("Failed to enter text after retries: " + locator, e);
                }
            }
        }
    }

    /**
     * Scrolls element into view.
     */
    public void scrollIntoView(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", element);
    }

    /**
     * Waits for page load and Fusion loading overlays to disappear.
     */
    public void waitForLoadingToFinish() {
        try {
            wait.until(new ExpectedCondition<Boolean>() {
                public Boolean apply(WebDriver driver) {
                    return ((JavascriptExecutor) driver).executeScript("return document.readyState").equals("complete");
                }
            });

            if (isJQueryDefined()) {
                wait.until(new ExpectedCondition<Boolean>() {
                    public Boolean apply(WebDriver driver) {
                        return ((JavascriptExecutor) driver).executeScript("return jQuery.active == 0").equals(true);
                    }
                });
            }

            wait.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector(".AFBusyWait, .fusionBusy, .loading-mask")));

        } catch (TimeoutException e) {
            System.err.println("Page or overlay load timeout: " + e.getMessage());
        }
    }

    /**
     * Waits for a component to reload after staleness.
     */
    public void waitForElementStalenessAndReappear(By locator) {
        WebElement oldElement = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        wait.until(ExpectedConditions.stalenessOf(oldElement));
        wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Checks if jQuery is defined.
     */
    public boolean isJQueryDefined() {
        try {
            Object result = ((JavascriptExecutor) driver).executeScript("return typeof jQuery != 'undefined'");
            return result != null && result.equals(true);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Utility sleep for retries.
     */
    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}
    }

    // Sample usage
    public static void main(String[] args) {
        System.setProperty("webdriver.chrome.driver", "/path/to/chromedriver");
        WebDriver driver = new ChromeDriver();
        driver.manage().window().maximize();

        OracleFSCMHelper fscm = new OracleFSCMHelper(driver);

        try {
            driver.get("https://your-oracle-fscm-instance.com");

            fscm.enterTextAndHandleRefresh(By.id("username"), "your_username");
            fscm.enterTextAndHandleRefresh(By.id("password"), "your_password");
            fscm.clickElementAndHandleLoading(By.id("LoginButton"));

            fscm.clickElementAndHandleLoading(By.xpath("//a[text()='Invoices']"));
            fscm.enterTextAndHandleRefresh(By.id("invoiceNumberField"), "INV-12345");
            fscm.clickElementAndHandleLoading(By.id("searchButton"));
            fscm.wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("searchResultsTable")));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }
}
