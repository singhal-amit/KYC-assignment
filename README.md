# KYC Web Scraping Assignment

> Automated extraction of public representative data from **Alaska Senate Website** and transformation into **KYC-compliant JSON format**.

---

## 🚀 Overview

This project is a **Java‑based automation tool** that scrapes structured public identity and contact information from:

```
https://akleg.gov/senate.php
```

It navigates each member profile, extracts key attributes (Name, Party, Title, District, Email, Phone, Address, Page URL), and exports the results to:

```
senate_members.json
```

This output simulates a **KYC (Know Your Customer)** dataset format useful for identity records and compliance workflows.

---

## 🧰 Tech Stack

| Category       | Technology         |
| -------------- | ------------------ |
| Language       | Java (JDK 20+)     |
| Build Tool     | Maven              |
| Automation     | Selenium WebDriver |
| Browser Driver | ChromeDriver       |
| Output Format  | JSON               |

---

## 📁 Project Structure

```
KYC_Assignment/
│
├── pom.xml                      # Maven dependencies & build config
├── senate_members.json          # Output file after scraping
├── README.md                    # Project documentation
│
└── src/
    └── main/
        └── java/
            └── org/
                └── example/
                    └── AkLegSenateScraper.java   # Main scraper script
```

---

## ⚙️ Installation & Setup

### ✅ Step 1 — Verify Environment

```bash
java -version
mvn -version
```

### ✅ Step 2 — Install Dependencies

Maven automatically installs libraries

```bash
mvn clean install
```

### ✅ Step 3 — Configure ChromeDriver

Ensure ChromeDriver is installed and added to PATH.

Example (Windows):
Add `chromedriver.exe` to PATH in System Environment Variables.

### ✅ Step 4 — Run the Program

```bash
mvn exec:java -Dexec.mainClass=org.example.AkLegSenateScraper
```

**Output file generated:** `senate_members.json`

---

## 📦 Sample JSON Output

```json
[
  {
    "Name": "JESSE BJORKMAN",
    "Title": "SENATOR",
    "Party": "Republican",
    "Position": "District D",
    "Address": "State Capitol Room 427 Juneau AK, 99801",
    "Phone": "907-465-2828",
    "Email": "Senator.Jesse.Bjorkman@akleg.gov",
    "URL": "https://akleg.gov/legislator.php?id=bjk"
  }
]
```

---

## ⏱️ Time Spent

| Activity                           | Duration |
| ---------------------------------- | -------- |
| Analyzing website & data selectors | 10 min   |
| Writing Java + Selenium scraper    | 60 min   |
| Debugging & refinement             | 10 min   |
| Documentation & formatting         | 20 min   |

**🟩 Total: 2 hours**

---

## ✅ Status

This assignment is **completed successfully** with:

* ✅ Live web scraping
* ✅ JSON export
* ✅ Professional documentation
* ✅ Reproducible execution

---

### 📎 Notes

* The scraper respects polite scraping delays.
* Only publicly available data is collected.
* Can be extended for CSV / DB insertion / API upload.
