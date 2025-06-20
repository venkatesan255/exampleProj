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




// InvoicePage.java
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class InvoicePage {
    private WebDriver driver;
    private WebDriverWait wait;

    @FindBy(id = "pt1:r1:0:rt:1:r2:0:dynamicRegion1:1:AP1:q1:valueLOVId::content")
    private WebElement supplierLOVInput;

    @FindBy(id = "pt1:r1:0:rt:1:r2:0:dynamicRegion1:1:AP1:q1:valueLOVId::lovIconId")
    private WebElement supplierLOVSearchIcon;

    public InvoicePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(45));
        PageFactory.initElements(driver, this);
    }

    public void selectSupplierFromLOV(String supplierName) {
        supplierLOVInput.clear();
        supplierLOVSearchIcon.click();

        LOVPopupPage lovPopup = new LOVPopupPage(driver);
        lovPopup.searchAndSelectValue(supplierName);

        wait.until(ExpectedConditions.attributeToBe(supplierLOVInput, "value", supplierName));
        System.out.println("Supplier LOV populated with: " + supplierName);
    }
}

// LOVPopupPage.java
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class LOVPopupPage {
    private WebDriver driver;
    private WebDriverWait wait;

    @FindBy(xpath = "//div[contains(@class,'AFModalGlassPane') and @aria-modal='true']")
    private WebElement lovPopupContainer;

    @FindBy(xpath = "//label[text()='Name']//following::input[1]")
    private WebElement popupSearchInput;

    @FindBy(xpath = "//button[text()='Search']")
    private WebElement popupSearchButton;

    @FindBy(xpath = "//table[contains(@class,'AFDataTable')]")
    private WebElement searchResultsTable;

    @FindBy(xpath = "//button[text()='OK']")
    private WebElement popupOKButton;

    public LOVPopupPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(45));
        PageFactory.initElements(driver, this);
    }

    public void searchAndSelectValue(String value) {
        switchToLOVIframeIfPresent();

        wait.until(ExpectedConditions.visibilityOf(lovPopupContainer));
        wait.until(ExpectedConditions.elementToBeClickable(popupSearchInput)).clear();
        popupSearchInput.sendKeys(value);
        wait.until(ExpectedConditions.elementToBeClickable(popupSearchButton)).click();

        wait.until(ExpectedConditions.attributeToBe(searchResultsTable, "aria-busy", "false"));
        By resultRowLocator = By.xpath("//table[contains(@class,'AFDataTable')]//td[text()='" + value + "']");
        WebElement desiredValue = wait.until(ExpectedConditions.elementToBeClickable(resultRowLocator));
        desiredValue.click();

        List<WebElement> okButtons = driver.findElements(By.xpath("//button[text()='OK']"));
        if (!okButtons.isEmpty() && okButtons.get(0).isDisplayed()) {
            okButtons.get(0).click();
        }

        wait.until(ExpectedConditions.invisibilityOf(lovPopupContainer));
        driver.switchTo().defaultContent();
    }

    private void switchToLOVIframeIfPresent() {
        List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
        for (WebElement frame : iframes) {
            driver.switchTo().frame(frame);
            if (!driver.findElements(By.xpath("//div[contains(@class,'AFModalGlassPane')]"))
                    .isEmpty()) {
                System.out.println("Switched to LOV iframe.");
                return;
            }
            driver.switchTo().defaultContent();
        }
    }
}

// LOVHandler.java
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LOVHandler {
    private WebDriver driver;
    private WebDriverWait wait;

    public LOVHandler(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(45));
    }

    public void setLovValueByTypeAndTab(By lovInputLocator, String valueToSelect) {
        WebElement lovInput = wait.until(ExpectedConditions.elementToBeClickable(lovInputLocator));
        lovInput.clear();
        lovInput.sendKeys(valueToSelect);
        lovInput.sendKeys(Keys.TAB);
        waitForPageToLoad();
    }

    public void waitForPageToLoad() {
        try {
            wait.until(ExpectedConditions.jsReturnsValue("return document.readyState === 'complete'"));
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                By.xpath("//div[contains(@id,'_afrLoop') and @aria-busy='true']")
            ));
        } catch (Exception e) {
            System.err.println("Page load wait failed: " + e.getMessage());
        }
    }
}



// --- In your main Page Object (e.g., InvoicePage.java) ---
public class InvoicePage {
    private WebDriver driver;
    private WebDriverWait wait;

    @FindBy(id = "pt1:r1:0:rt:1:r2:0:dynamicRegion1:1:AP1:q1:valueLOVId::content") // Example LOV input ID
    private WebElement supplierLOVInput;

    @FindBy(id = "pt1:r1:0:rt:1:r2:0:dynamicRegion1:1:AP1:q1:valueLOVId::lovIconId") // Example LOV search icon ID
    private WebElement supplierLOVSearchIcon;

    public InvoicePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(45));
        PageFactory.initElements(driver, this);
    }

    public void selectSupplierFromLOV(String supplierName) {
        supplierLOVInput.clear();
        supplierLOVSearchIcon.click(); // Click the search icon to open the LOV popup

        // Now, hand over control to the LOV popup handler
        LOVPopupPage lovPopup = new LOVPopupPage(driver);
        lovPopup.searchAndSelectValue(supplierName); // This method will handle waits within the popup
        
        // After selection and popup closure, wait for the main page to reflect the change
        // For example, verify the supplier input field is populated with the correct value
        wait.until(ExpectedConditions.attributeToBe(supplierLOVInput, "value", supplierName));
        System.out.println("Supplier LOV populated with: " + supplierName);
        
        // Additional wait if other elements on the main page are affected
        // waitForPageToLoad(); // General page load helper
    }

    // Include the waitForPageToLoad method here as well
    public void waitForPageToLoad() { /* ... as defined previously ... */ }
}

// --- In a separate LOV Popup Page Object (e.g., LOVPopupPage.java) ---
public class LOVPopupPage {
    private WebDriver driver;
    private WebDriverWait wait;

    // Common locators for LOV popups in Oracle FSCM (these are examples, verify with your app)
    @FindBy(xpath = "//div[contains(@class,'AFModalGlassPane') and @aria-modal='true']") // Locator for the modal popup container
    private WebElement lovPopupContainer;

    @FindBy(xpath = "//label[text()='Name']//following::input[1]") // Example: Search input field for 'Name' within the popup
    private WebElement popupSearchInput;

    @FindBy(xpath = "//button[text()='Search']") // Example: Search button within the popup
    private WebElement popupSearchButton;

    @FindBy(xpath = "//table[contains(@class,'AFDataTable')]") // Example: Locator for the results table within the popup
    private WebElement searchResultsTable;

    @FindBy(xpath = "//button[text()='OK']") // Example: OK/Select button within the popup
    private WebElement popupOKButton;

    public LOVPopupPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(45));
        PageFactory.initElements(driver, this);
    }

    /**
     * Searches for a value within the LOV popup and selects it.
     * @param value The value to search for and select.
     */
    public void searchAndSelectValue(String value) {
        // Step 1: Wait for the LOV popup to be visible
        wait.until(ExpectedConditions.visibilityOf(lovPopupContainer));
        System.out.println("LOV popup is visible.");

        // Step 2: Enter search criteria in the popup's search field
        wait.until(ExpectedConditions.elementToBeClickable(popupSearchInput));
        popupSearchInput.clear();
        popupSearchInput.sendKeys(value);
        System.out.println("Entered '" + value + "' into LOV popup search field.");

        // Step 3: Click the search button within the popup
        wait.until(ExpectedConditions.elementToBeClickable(popupSearchButton));
        popupSearchButton.click();
        System.out.println("Clicked LOV popup Search button.");

        // Step 4: Wait for search results to load.
        // This is a crucial AJAX wait. You might need to wait for a loading spinner
        // within the popup to disappear, or for the results table rows to appear.
        wait.until(ExpectedConditions.visibilityOf(searchResultsTable));
        wait.until(ExpectedConditions.not(ExpectedConditions.attributeContains(searchResultsTable, "aria-busy", "true"))); // ADF specific busy indicator
        // Or wait for a specific row containing the text to be visible
        By resultRowLocator = By.xpath("//table[contains(@class,'AFDataTable')]//td[text()='" + value + "']");
        wait.until(ExpectedConditions.visibilityOfElementLocated(resultRowLocator));
        System.out.println("LOV search results loaded.");

        // Step 5: Select the desired value from the results table
        // Find the specific row/element that contains the desired value and click it
        WebElement desiredValueElement = wait.until(ExpectedConditions.elementToBeClickable(resultRowLocator));
        desiredValueElement.click();
        System.out.println("Clicked on desired value '" + value + "' in LOV popup results.");
        
        // Step 6: Click the OK/Select button within the popup
        // This might be part of the row itself, or a separate button
        if (popupOKButton != null) { // Some LOVs auto-close on row click, others need an OK
            wait.until(ExpectedConditions.elementToBeClickable(popupOKButton));
            popupOKButton.click();
            System.out.println("Clicked LOV popup OK button.");
        }

        // Step 7: Wait for the popup to close.
        wait.until(ExpectedConditions.invisibilityOf(lovPopupContainer));
        System.out.println("LOV popup closed.");
    }
}

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

public class LOVHandler {

    private WebDriver driver;
    private WebDriverWait wait;

    public LOVHandler(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(45));
    }

    /**
     * Attempts to set an LOV value by typing and then tabbing out.
     * Use this if the LOV supports direct entry and auto-validation.
     * @param lovInputLocator Locator for the LOV input field.
     * @param valueToSelect The text value to enter.
     */
    public void setLovValueByTypeAndTab(By lovInputLocator, String valueToSelect) {
        WebElement lovInput = wait.until(ExpectedConditions.elementToBeClickable(lovInputLocator));
        lovInput.clear();
        lovInput.sendKeys(valueToSelect);
        lovInput.sendKeys(Keys.TAB); // Simulate Tab key to trigger validation/selection

        // Crucial: Wait for the AJAX call to complete and the page to stabilize
        // This might involve waiting for a loading spinner to disappear,
        // or for an expected element that appears after selection to become visible.
        waitForPageToLoad(); // Or more specific waits
        System.out.println("Set LOV '" + lovInputLocator + "' with value: " + valueToSelect);
    }

    // Include the waitForPageToLoad method from the previous example, as it's essential here.
    public void waitForPageToLoad() {
        try {
            wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
            // Add other specific waits like invisibilityOfElementLocated for common loading spinners
            // if you have identified them.
            // Example: wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("//div[contains(@id,'_afrLoop') and @aria-busy='true']")));
            // This is a common ADF loading indicator. You might need to refine the locator.
        } catch (Exception e) {
            System.err.println("Error during waitForPageToLoad: " + e.getMessage());
        }
    }
}


import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.devtools.DevTools;
import org.openqa.selenium.devtools.v126.network.Network; // Check your exact version (e.g., v126, v127, etc.)
import org.openqa.selenium.devtools.v126.network.model.RequestId;
import org.openqa.selenium.devtools.v126.network.model.RequestWillBeSent;
import org.openqa.selenium.devtools.v126.network.model.ResponseReceived;
import org.openqa.selenium.devtools.v126.network.model.LoadingFinished;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.Optional; // For DevTools API

public class LOVPopupPage {
    private WebDriver driver;
    private WebDriverWait wait;
    private DevTools devTools;

    // Use ConcurrentHashSet if multiple threads interact, otherwise HashSet is fine for single-threaded Selenium
    private Set<RequestId> pendingXhrRequests = new HashSet<>();
    private final String ADF_XHR_URL_PART = "/faces/oracle/"; // You might need to adjust this based on your XHR URLs

    // Primary Locator for the LOV popup container
    private By lovPopupContainerLocator = By.cssSelector("div[id*='popup-container']");

    // Locators for elements *within* the LOV popup
    private By popupSearchInputLocator = By.xpath(".//label[text()='Name']//following::input[1]");
    private By popupSearchButtonLocator = By.xpath(".//button[text()='Search']");
    private By searchResultsTableLocator = By.xpath(".//table[contains(@class,'AFDataTable')]");
    private By popupOKButtonLocator = By.xpath(".//button[text()='OK']");


    public LOVPopupPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(45));

        // Important: Only proceed with DevTools if the driver is a ChromeDriver (or EdgeDriver)
        if (driver instanceof ChromeDriver) {
            this.devTools = ((ChromeDriver) driver).getDevTools();
            this.devTools.createSession();
            this.devTools.send(Network.enable(Optional.empty(), Optional.empty(), Optional.empty()));

            // Listen for network events
            this.devTools.addListener(Network.requestWillBeSent(), request -> {
                if (request.getRequest().getUrl().contains(ADF_XHR_URL_PART) && request.getType().toString().equalsIgnoreCase("XHR")) {
                    pendingXhrRequests.add(request.getRequestId());
                    // System.out.println("XHR Request started: " + request.getRequest().getUrl());
                }
            });

            this.devTools.addListener(Network.responseReceived(), response -> {
                if (response.getType().toString().equalsIgnoreCase("XHR") && pendingXhrRequests.contains(response.getRequestId())) {
                    // XHR request completed its response. We can remove it here or wait for LoadingFinished
                    // For more robust "loading finished", it's better to wait for LoadingFinished event.
                    // pendingXhrRequests.remove(response.getRequestId()); // Might remove too early
                }
            });

            this.devTools.addListener(Network.loadingFinished(), loadingFinished -> {
                // Ensure it's an XHR request we were tracking
                if (pendingXhrRequests.contains(loadingFinished.getRequestId())) {
                    pendingXhrRequests.remove(loadingFinished.getRequestId());
                    // System.out.println("XHR Request finished: " + loadingFinished.getRequestId());
                }
            });

        } else {
            System.out.println("Warning: DevTools API for XHR monitoring is only supported for Chrome/Edge drivers. Using fallback waits.");
            this.devTools = null; // No DevTools for this driver type
        }
    }

    /**
     * Waits for all relevant XHR requests to complete.
     */
    private void waitForAdfAjaxToComplete() {
        if (devTools == null) {
            System.out.println("DevTools not initialized, skipping XHR wait. Relying on explicit element waits.");
            // Fallback: If DevTools isn't available, fall back to waiting for page ready state
            // and possibly other UI changes, or just rely on the subsequent element waits.
            // You might add a Thread.sleep(500) here as a last resort if flakiness persists without visible indicators.
            try {
                wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
            } catch (Exception e) {
                System.out.println("Document ready state wait timed out: " + e.getMessage());
            }
            return;
        }

        try {
            // Wait for pendingXhrRequests set to become empty
            wait.until(d -> {
                // System.out.println("Pending XHR Requests: " + pendingXhrRequests.size());
                return pendingXhrRequests.isEmpty();
            });
            System.out.println("All relevant XHR requests completed.");

            // Add a small, immediate wait for the browser to process the DOM updates after AJAX completion
            // This is crucial even after XHRs are done, as browser rendering takes a moment.
            Thread.sleep(200); // 200ms is often enough

        } catch (Exception e) {
            System.err.println("XHR AJAX completion wait timed out or failed: " + e.getMessage());
            // It's possible for the XHRs to complete too fast, or for a non-tracked XHR to cause a delay.
            // If the next step (waiting for the element) passes, this timeout might be acceptable.
        }
    }

    /**
     * Searches for a value within the LOV popup and selects it.
     * @param value The value to search for and select.
     */
    public void searchAndSelectValue(String value) {
        // Step 1: Wait for the LOV popup to be visible using the primary container locator
        WebElement lovPopupContainer = wait.until(ExpectedConditions.visibilityOfElementLocated(lovPopupContainerLocator));
        System.out.println("LOV popup is visible using locator: " + lovPopupContainerLocator);

        // Crucial: Wait for any initial AJAX calls after popup appears
        // The popup itself might trigger an XHR to load its initial content
        waitForAdfAjaxToComplete();

        // Step 2: Enter search criteria in the popup's search field
        WebElement popupSearchInput = wait.until(ExpectedConditions.elementToBeClickable(lovPopupContainer.findElement(popupSearchInputLocator)));
        popupSearchInput.clear();
        popupSearchInput.sendKeys(value);
        System.out.println("Entered '" + value + "' into LOV popup search field.");

        // Step 3: Click the search button within the popup
        WebElement popupSearchButton = wait.until(ExpectedConditions.elementToBeClickable(lovPopupContainer.findElement(popupSearchButtonLocator)));
        popupSearchButton.click();
        System.out.println("Clicked LOV popup Search button.");

        // Step 4: Wait for search results to load.
        // This is a crucial AJAX wait after clicking search.
        waitForAdfAjaxToComplete(); // Call our helper method

        // Now, wait for the actual results to appear (most reliable *after* AJAX completion)
        By resultRowLocator = By.xpath(".//table[contains(@class,'AFDataTable')]//td[text()='" + value + "']");
        WebElement desiredValueElement = wait.until(ExpectedConditions.elementToBeClickable(lovPopupContainer.findElement(resultRowLocator)));
        System.out.println("LOV search results loaded and element visible.");

        // Step 5: Select the desired value from the results table
        desiredValueElement.click();
        System.out.println("Clicked on desired value '" + value + "' in LOV popup results.");

        // Step 6: Click the OK/Select button within the popup if it exists and is needed
        if (driver.findElements(popupOKButtonLocator).size() > 0) {
            WebElement popupOKButton = wait.until(ExpectedConditions.elementToBeClickable(lovPopupContainer.findElement(popupOKButtonLocator)));
            popupOKButton.click();
            System.out.println("Clicked LOV popup OK button.");
        } else {
             System.out.println("No 'OK' button found; assuming LOV auto-closes on selection.");
        }

        // Step 7: Wait for the popup to close.
        wait.until(ExpectedConditions.invisibilityOfElementLocated(lovPopupContainerLocator));
        System.out.println("LOV popup closed.");

        // Optionally, wait for a final AJAX call on the main page after the popup closes.
        waitForAdfAjaxToComplete();
    }

    // You'll need to call this to gracefully shut down the DevTools session
    public void closeDevTools() {
        if (devTools != null) {
            devTools.close();
        }
    }
}
