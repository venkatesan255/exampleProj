import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class OracleFSCMHelper {

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final int maxRetries = 3;

    public OracleFSCMHelper(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(45));
    }

    // -------------------------------
    // Action Methods
    // -------------------------------

    public void clickElementAndWait(By locator) {
        retryClick(locator);
        waitForPageToLoad();
    }

    public void enterTextAndWait(By locator, String text) {
        retrySendKeys(locator, text);
        waitForPageToLoad();
    }

    public void waitForElementVisible(By locator) {
        wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public void waitForElementInvisible(By locator) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    public void waitForElementStalenessAndReappear(By locator) {
        WebElement oldElement = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        wait.until(ExpectedConditions.stalenessOf(oldElement));
        wait.until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    public void waitForPageToLoad() {
        try {
            // Wait for document ready
            boolean docReady = false;
            for (int i = 0; i < 10; i++) {
                String state = (String) ((JavascriptExecutor) driver).executeScript("return document.readyState");
                if ("complete".equals(state)) {
                    docReady = true;
                    break;
                }
                Thread.sleep(500);
            }

            if (!docReady) {
                System.err.println("Document not ready after wait period.");
            }

            // Wait for jQuery AJAX calls to finish
            if (isJQueryDefined()) {
                for (int i = 0; i < 10; i++) {
                    Long activeRequests = (Long) ((JavascriptExecutor) driver).executeScript("return jQuery.active");
                    if (activeRequests == 0) {
                        break;
                    }
                    Thread.sleep(500);
                }
            }

            // Wait for Oracle Fusion ADF busy indicator to disappear
            waitForElementInvisible(By.cssSelector("div.AFBusyWait"));

        } catch (Exception e) {
            System.err.println("Error while waiting for page to load: " + e.getMessage());
        }
    }

    // -------------------------------
    // Retry Utilities
    // -------------------------------

    private void retryClick(By locator) {
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
                element.click();
                return;
            } catch (StaleElementReferenceException | ElementClickInterceptedException | ElementNotInteractableException | TimeoutException e) {
                attempts++;
                pause(1000);
                if (attempts == maxRetries) {
                    throw new RuntimeException("Click failed after retries: " + locator, e);
                }
            }
        }
    }

    private void retrySendKeys(By locator, String value) {
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
                element.clear();
                element.sendKeys(value);
                return;
            } catch (StaleElementReferenceException | ElementNotInteractableException | TimeoutException e) {
                attempts++;
                pause(1000);
                if (attempts == maxRetries) {
                    throw new RuntimeException("SendKeys failed after retries: " + locator, e);
                }
            }
        }
    }

    // -------------------------------
    // Helper Methods
    // -------------------------------

    private boolean isJQueryDefined() {
        try {
            Object result = ((JavascriptExecutor) driver).executeScript("return typeof jQuery != 'undefined'");
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            return false;
        }
    }

    private void pause(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}


WebDriver driver = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), new ChromeOptions().addArguments("--headless"));
OracleFSCMHelper helper = new OracleFSCMHelper(driver);

helper.enterTextAndWait(By.id("username"), "yourUser");
helper.enterTextAndWait(By.id("password"), "yourPass");
helper.clickElementAndWait(By.id("LoginButton"));

helper.clickElementAndWait(By.xpath("//a[text()='Invoices']"));
helper.enterTextAndWait(By.id("invoiceNumberField"), "INV-999");
helper.clickElementAndWait(By.id("searchButton"));

helper.waitForElementVisible(By.id("searchResultsTable"));

