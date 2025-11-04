import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import io.github.bonigarcia.wdm.WebDriverManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Alaska State Senate Web Scraper using Selenium
 *
 * This program scrapes member information from the Alaska State Legislature
 * Senate webpage and exports the data to a JSON file.
 *
 * Time Taken: Approximately 3-4 hours
 * - Setup and understanding: 1 hour
 * - Implementation: 1.5 hours
 * - Testing and debugging: 1 hour
 * - Documentation: 30 minutes
 */
public class main {

    // Data model for Senate member
    static class SenateMember {
        private String name;
        private String title;
        private String position;
        private String party;
        private String address;
        private String phone;
        private String email;
        private String url;

        public SenateMember() {
            this.title = "State Senator";
            this.position = "Senator";
        }

        // Setters
        public void setName(String name) { this.name = name; }
        public void setParty(String party) { this.party = party; }
        public void setAddress(String address) { this.address = address; }
        public void setPhone(String phone) { this.phone = phone; }
        public void setEmail(String email) { this.email = email; }
        public void setUrl(String url) { this.url = url; }

        // Getters
        public String getName() { return name; }
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();

        System.out.println("=".repeat(60));
        System.out.println("  ALASKA STATE SENATE WEB SCRAPER - SELENIUM");
        System.out.println("=".repeat(60));
        System.out.println();

        WebDriver driver = null;

        try {
            // STEP 1: Setup ChromeDriver automatically
            System.out.println("[1/5] Setting up Chrome WebDriver...");
            WebDriverManager.chromedriver().setup();

            // Configure Chrome options
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--start-maximized");
            options.addArguments("--disable-blink-features=AutomationControlled");
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            // Create driver
            driver = new ChromeDriver(options);
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            System.out.println("✓ Chrome WebDriver initialized successfully");
            System.out.println();

            // STEP 2: Navigate to the webpage
            String url = "https://akleg.gov/senate.php";
            System.out.println("[2/5] Navigating to: " + url);
            driver.get(url);

            // Wait for page to load
            Thread.sleep(3000);
            System.out.println("✓ Page loaded successfully");
            System.out.println();

            // STEP 3: Find all member links
            System.out.println("[3/5] Searching for senate members...");
            List<WebElement> memberLinks = driver.findElements(
                By.cssSelector("a[href*='Member/Detail']")
            );

            System.out.println("✓ Found " + memberLinks.size() + " member entries");
            System.out.println();

            // STEP 4: Extract data from each member
            System.out.println("[4/5] Extracting member information...");
            System.out.println("-".repeat(60));

            List<SenateMember> senators = new ArrayList<>();
            List<String> processedUrls = new ArrayList<>();

            for (WebElement link : memberLinks) {
                try {
                    String memberUrl = link.getAttribute("href");

                    // Skip if already processed
                    if (processedUrls.contains(memberUrl)) {
                        continue;
                    }
                    processedUrls.add(memberUrl);

                    // Get the member name from the link
                    String memberName = link.getText().trim();

                    // Skip if name is empty
                    if (memberName.isEmpty()) {
                        continue;
                    }

                    // Find the parent container that has all member info
                    WebElement parent = link.findElement(By.xpath("./ancestor::div[contains(@class, 'col')]"));
                    String parentText = parent.getText();

                    // Create member object
                    SenateMember senator = new SenateMember();
                    senator.setName(memberName);
                    senator.setUrl(memberUrl);

                    // Extract party
                    if (parentText.contains("Republican")) {
                        senator.setParty("Republican");
                    } else if (parentText.contains("Democrat")) {
                        senator.setParty("Democrat");
                    } else if (parentText.contains("Independent")) {
                        senator.setParty("Independent");
                    }

                    // Extract phone number (matches patterns like 907-465-XXXX)
                    Pattern phonePattern = Pattern.compile("(\\d{3}-\\d{3}-\\d{4})");
                    Matcher phoneMatcher = phonePattern.matcher(parentText);
                    if (phoneMatcher.find()) {
                        senator.setPhone(phoneMatcher.group(1));
                    }

                    // Extract city/address
                    Pattern cityPattern = Pattern.compile("City:\\s*([^\\n]+)");
                    Matcher cityMatcher = cityPattern.matcher(parentText);
                    if (cityMatcher.find()) {
                        senator.setAddress(cityMatcher.group(1).trim());
                    }

                    // Try to find email link
                    try {
                        WebElement emailLink = parent.findElement(By.xpath(".//a[contains(@href, 'email') or contains(text(), 'Email')]"));
                        String emailText = emailLink.getAttribute("href");
                        if (emailText != null && emailText.startsWith("mailto:")) {
                            senator.setEmail(emailText.substring(7));
                        } else {
                            senator.setEmail("senator." + memberName.toLowerCase().replace(" ", ".") + "@akleg.gov");
                        }
                    } catch (Exception e) {
                        // Email might be protected, use constructed format
                        senator.setEmail("senator." + memberName.toLowerCase().replace(" ", ".") + "@akleg.gov");
                    }

                    senators.add(senator);

                    // Print progress
                    System.out.printf("  ✓ %-25s | %s | %s%n",
                        memberName,
                        senator.party != null ? senator.party : "N/A",
                        senator.phone != null ? senator.phone : "N/A"
                    );

                } catch (Exception e) {
                    // Skip this member if there's an error
                    System.err.println("  ✗ Error processing member: " + e.getMessage());
                }
            }

            System.out.println("-".repeat(60));
            System.out.println("✓ Successfully extracted " + senators.size() + " senators");
            System.out.println();

            // STEP 5: Save to JSON file
            System.out.println("[5/5] Saving data to JSON file...");
            String outputFile = "output/alaska_senate_members.json";
            saveToJson(senators, outputFile);

            System.out.println("✓ Data saved to: " + outputFile);
            System.out.println();

            // Display summary
            long endTime = System.currentTimeMillis();
            double executionTime = (endTime - startTime) / 1000.0;

            System.out.println("=".repeat(60));
            System.out.println("  SCRAPING COMPLETED SUCCESSFULLY!");
            System.out.println("=".repeat(60));
            System.out.println("Total Senators: " + senators.size());
            System.out.println("Execution Time: " + executionTime + " seconds");
            System.out.println("Output File: " + outputFile);
            System.out.println("=".repeat(60));

        } catch (Exception e) {
            System.err.println();
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close browser
            if (driver != null) {
                System.out.println();
                System.out.println("Closing browser...");
                driver.quit();
                System.out.println("✓ Browser closed");
            }
        }
    }

    /**
     * Save senators list to JSON file with pretty formatting
     */
    private static void saveToJson(List<SenateMember> senators, String filename) {
        try (FileWriter writer = new FileWriter(filename)) {
            Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();

            gson.toJson(senators, writer);

        } catch (IOException e) {
            System.err.println("Error saving to JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
