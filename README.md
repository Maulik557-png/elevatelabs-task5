# Java Bank (Console Application)

A **console-based Banking System** written in **Java**.  
This program allows users to **create accounts, authenticate with a secure PIN, deposit money, withdraw funds, check balance, and view transaction history**.  
It demonstrates **Object-Oriented Programming (OOP), BigDecimal for precise money handling, SHA-256 PIN hashing, and robust CLI design**.

---

## Features

- **Create Account**
  - Requires **unique account number**, **account holder name**, **initial deposit**, and **secure PIN** (4–6 digits).
  - PINs are **stored securely using SHA-256 hashing**.
  - Initial deposits are validated and recorded in the transaction history.

- **Deposit Money**
  - Validates positive deposit amounts with a maximum cap (1 billion per transaction).
  - Automatically records deposits in transaction history.

- **Withdraw Money**
  - Validates sufficient balance before allowing withdrawals.
  - Records failed withdrawal attempts in history.

- **Check Balance**
  - Displays account summary including **account number, holder name, and current balance**.

- **Transaction History**
  - Shows all transactions with **timestamps** (using `LocalDateTime`).
  - Includes deposits, withdrawals, account creation, and failed attempts.

- **PIN-Based Security**
  - Every sensitive action (deposit, withdraw, balance check, transaction history) requires **account number + PIN authentication**.
  - Allows up to **3 attempts**, then blocks the operation.

- **Input Validation & Error Handling**
  - Prevents invalid or empty inputs.
  - Handles malformed amounts (negative values, too many decimals).
  - Ensures program never crashes due to invalid input.

- **User-Friendly CLI**
  - Menu-driven interface with clear numbered options.
  - Traditional `delay()` method (`Thread.sleep`) for smoother UX flow.

---

## Project Structure

```
JavaBank/
|- BankApp.java      # Main program with menu-driven CLI
|- README.md         # Project documentation
```

---

## How to Run

1. **Clone or download** the project.  

2. Open a terminal and navigate to the project folder.  

3. Compile and run the program:  

   ```bash
   javac BankApp.java
   java BankApp
   ```

---

## Example Usage

```bash
===== Java Bank =====
1. Create Account
2. Deposit Money
3. Withdraw Money
4. Check Balance
5. View Transaction History
6. Exit
Enter your choice: 1
```

**Creating an Account Example:**

```bash
--- Create Account ---
Enter Account Number (alphanumeric, no spaces): A101
Enter Account Holder Name: John Doe
Enter Initial Deposit (0 allowed): 500
Set a 4-6 digit numeric PIN: 1234
Confirm PIN: 1234
SUCCESS: Account created for 'John Doe' with Account Number: A101
```

**Depositing Money Example:**

```bash
--- Deposit ---
Enter Account Number: A101
Enter PIN (attempt 1/3): 1234
Enter deposit amount (greater than 0): 250
SUCCESS: Deposited $250.00
```

**Withdrawing Money Example:**

```bash
--- Withdraw ---
Enter Account Number: A101
Enter PIN (attempt 1/3): 1234
Enter withdrawal amount (greater than 0): 1000
ERROR: Withdrawal failed — insufficient funds.
```

**Transaction History Example:**

```bash
--- Transaction History ---
[2025-09-30 11:15:20] Account created with initial deposit: $500.00
[2025-09-30 11:16:05] Deposited: $250.00 | Balance: $750.00
[2025-09-30 11:17:10] Failed withdrawal attempt: $1000.00 | Balance: $750.00
```

---

## Concepts Showcased

- **Object-Oriented Programming (OOP)**
  - Encapsulation of account details in the `Account` class.
  - Clear separation between `Account` and `BankApp` logic.

- **Secure PIN Storage**
  - SHA-256 hashing ensures PINs are never stored in plain text.

- **BigDecimal for Money**
  - Accurate handling of financial transactions with scale fixed to 2 decimals.

- **Collections**
  - `HashMap` for storing accounts with account numbers as keys.
  - `ArrayList` for maintaining transaction history.

- **Date & Time API**
  - `LocalDateTime` with formatted timestamps for transactions.

- **Robust Input Handling**
  - Continuous prompting until valid input is received.
  - Defensive coding against nulls, empty strings, and closed input streams.

- **CLI UX Improvements**
  - Menu-driven navigation.
  - `delay()` method for smoother experience.

---

## Future Enhancements

- Implement **fund transfers** between accounts.
- Add **account persistence** (save and load data from a file or database).
- Provide **monthly interest calculation** for savings accounts.
- Introduce **admin mode** for managing multiple accounts.
- Develop a **GUI version** using Swing or JavaFX.

---
