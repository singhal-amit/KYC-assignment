package org.example;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.jsoup.Jsoup;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AkLegSenateScraper {
    // Base URL for member pages
    private static final String BASE = "https://akleg.gov/";

    public static void main(String[] args) throws IOException {
        // Path to chromedriver must be on PATH or set with webdriver.chrome.driver system property
        // Example: System.setProperty("webdriver.chrome.driver", "/path/to/chromedriver");
        // We'll run headless.
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(8));

        String listUrl = "https://akleg.gov/senate.php";
        driver.get(listUrl);

        // find profile links for current members: href contains "legislator.php?id=" or links with "View More"
        List<WebElement> anchors = driver.findElements(By.xpath("//a[contains(@href,'legislator.php') or contains(@href,'legislator.php?id=') or contains(text(),'View More') or contains(@href,'?id=')]"));

        // we'll gather hrefs into a set to avoid duplicates
        Set<String> profileUrls = new LinkedHashSet<>();
        for (WebElement a : anchors) {
            try {
                String href = a.getAttribute("href");
                if (href == null || href.trim().isEmpty()) continue;
                // normalize relative links
                if (href.startsWith("/")) href = BASE.substring(0, BASE.length()-1) + href;
                // filter to legislator profile pages
                if (href.contains("legislator.php") || href.matches(".*/member.*") || href.matches(".*/legislator.*")) {
                    profileUrls.add(href);
                }
            } catch (Exception ignored) {}
        }

        // If none found via that xpath, try alternative: links containing "View More"
        if (profileUrls.isEmpty()) {
            List<WebElement> viewMore = driver.findElements(By.xpath("//a[contains(text(),'View More') or contains(text(),'view more')]"));
            for (WebElement a : viewMore) {
                String href = a.getAttribute("href");
                if (href != null && !href.trim().isEmpty()) {
                    if (href.startsWith("/")) href = BASE.substring(0, BASE.length()-1) + href;
                    profileUrls.add(href);
                }
            }
        }

        System.out.println("Found profile urls: " + profileUrls.size());

        JSONArray members = new JSONArray();

        // Visit each profile url and extract fields
        int index = 0;
        for (String purl : profileUrls) {
            index++;
            System.out.println("[" + index + "/" + profileUrls.size() + "] Visiting: " + purl);
            try {
                driver.get(purl);
                String pageHtml = driver.getPageSource();
                String visibleText = driver.findElement(By.tagName("body")).getText();

                // Name & Title: look for lines like "Senator\nJesse Bjorkman" or "Senator Jesse Bjorkman"
                String name = null;
                String title = null;
                // Try to find heading that contains 'Senator' or 'Representative'
                try {
                    List<WebElement> headings = driver.findElements(By.xpath("//h1|//h2|//h3|//h4"));
                    for (WebElement h : headings) {
                        String txt = h.getText();
                        if (txt != null && (txt.toLowerCase().contains("senator") || txt.toLowerCase().contains("representative") || txt.toLowerCase().contains("sen. "))) {
                            // split lines
                            String[] lines = txt.split("\\r?\\n");
                            if (lines.length >= 1) {
                                // sometimes it's "Senator\nName"
                                if (lines.length == 1) {
                                    String single = lines[0];
                                    // maybe "Senator Jesse Bjorkman" or "Senator\nName"
                                    if (single.toLowerCase().startsWith("senator") || single.toLowerCase().startsWith("representative")) {
                                        // extract title and name
                                        String[] parts = single.split("\\s+", 2);
                                        title = parts[0];
                                        if (parts.length > 1) name = parts[1].trim();
                                    }
                                } else {
                                    // multi-line: first likely title, second name
                                    title = lines[0].trim();
                                    name = lines[1].trim();
                                }
                                break;
                            }
                        }
                    }
                } catch (Exception ignored) {}

                // fallback: find first big heading text then find a word "Senator" in page's text
                if (name == null) {
                    // try regex: lines starting with "Senator" or "Representative"
                    Pattern p = Pattern.compile("(?m)^(Senator|Representative)\\s*\\n?\\s*(.+)$", Pattern.CASE_INSENSITIVE);
                    Matcher m = p.matcher(pageHtml);
                    if (m.find()) {
                        title = m.group(1).trim();
                        name = m.group(2).split("\\n")[0].trim();
                    } else {
                        // try grabbing the first H1/H2 text if present
                        try {
                            WebElement h1 = driver.findElement(By.xpath("//h1"));
                            String h1t = h1.getText();
                            if (h1t != null && !h1t.isBlank()) {
                                name = h1t.trim();
                            }
                        } catch (Exception ignored2) {}
                    }
                }

                // Email: find mailto link
                String email = null;
                try {
                    WebElement mail = driver.findElement(By.xpath("//a[starts-with(@href,'mailto:')]"));
                    if (mail != null) {
                        String href = mail.getAttribute("href");
                        if (href != null && href.startsWith("mailto:")) {
                            email = href.substring("mailto:".length()).trim();
                        } else {
                            email = mail.getText().trim();
                        }
                    }
                } catch (Exception ignored) {}

                // Party: try regex "Party: <word>"
                String party = findRegex(pageHtml, "Party:\\s*(.+)");
                if (party != null) party = party.split("\\n")[0].trim();

                // District -> use as Position
                String district = findRegex(pageHtml, "District:\\s*(.+)");
                if (district != null) district = district.split("\\n")[0].trim();

                // Session Contact address block and session phone:
                String address = null;
                String phone = null;
                // Regex attempt: Session Contact ... Phone: <number>
                Pattern sessionPat = Pattern.compile("Session Contact\\s*(.+?)\\s*Phone:\\s*([0-9\\-\\(\\)\\s]+)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
                Matcher sessionM = sessionPat.matcher(pageHtml);
                if (sessionM.find()) {
                    address = sessionM.group(1).replaceAll("\\s{2,}", " ").trim();
                    phone = sessionM.group(2).trim();
                } else {
                    // fallback: look for "Phone:" occurrences and choose first one after session contact
                    int idx = pageHtml.indexOf("Session Contact");
                    if (idx >= 0) {
                        int after = pageHtml.indexOf("Phone:", idx);
                        if (after >= 0) {
                            String chunk = pageHtml.substring(idx, Math.min(pageHtml.length(), idx + 600));
                            String addrCandidate = chunk.replaceAll("Session Contact", "").replaceAll("Phone:", "\nPhone:").trim();
                            address = addrCandidate.split("Phone:")[0].trim();
                            phone = findRegex(chunk, "Phone:\\s*([0-9\\-\\(\\)\\s]+)");
                        }
                    }
                }

                // if still null, try to pick first phone on the page
                if (phone == null) {
                    String p1 = findRegex(pageHtml, "Phone:\\s*([0-9\\-\\(\\)\\s]+)");
                    phone = p1;
                }

                // Title default
                if (title == null) title = "Senator";

                // Position: we'll set to district if available
                String position = district != null ? ("District " + district) : "";

                JSONObject member = new JSONObject();
                member.put("Name", name != null ? name : JSONObject.NULL);
                member.put("Title", title != null ? title : JSONObject.NULL);
                member.put("Position", position != null && !position.isBlank() ? position : JSONObject.NULL);
                member.put("Party", party != null ? party : JSONObject.NULL);
                member.put("Address", address != null ? Jsoup.parse(address).text() : JSONObject.NULL);
                member.put("Phone", phone != null ? phone : JSONObject.NULL);
                member.put("Email", email != null ? email : JSONObject.NULL);
                member.put("URL", purl);

                members.put(member);

                // small pause to be polite
                try { Thread.sleep(250); } catch (InterruptedException ignored) {}

            } catch (Exception e) {
                System.err.println("Failed on " + purl + " : " + e.getMessage());
            }
        }

        // write JSON to file
        try (FileWriter fw = new FileWriter("senate_members.json")) {
            fw.write(members.toString(2));
        }
        System.out.println("Wrote senate_members.json (" + members.length() + " members).");

        driver.quit();
    }

    private static String findRegex(String text, String pattern) {
        Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }
}
