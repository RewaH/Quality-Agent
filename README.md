# QA Copilot — Local Selenium Login Checks

A local Java and Selenium runner for the login story in `login.md`. It opens the target page in headless Chrome, records the page's visible elements, and runs six predefined login checks. It does not call an AI provider or require an API key.

## Requirements

- Java 17 or newer
- Maven 3.9 or newer
- Google Chrome installed
- Internet access to the target website; Selenium Manager may download a matching ChromeDriver the first time

## Run on Windows PowerShell

From the project directory, run:

```powershell
mvn compile exec:java "-Dexec.args=login.md https://www.saucedemo.com"
```

No API key or account setup is needed. The current runner supports login stories and checks valid credentials, invalid password, locked account, empty username, empty password, and dismissing a login error.

## Results

The `out` directory contains:

- `element-inventory.json` — visible page controls and their locators
- `test-plan.json` — the local login checks
- `triage-report.md` — pass/fail summary for each check
- `screenshots/` — screenshots captured when checks fail

Review failures and screenshots before reporting a product defect; an environment issue or an incorrect test assumption can also cause a failure.