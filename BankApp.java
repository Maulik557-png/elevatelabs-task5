import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Menu-driven bank simulation with PIN-based authentication and robust input validation.
 *
 * Features:
 * - Account (encapsulates account data, balance with BigDecimal, transaction history, and PIN hash)
 * - BankApp (driver with secure menu, input helpers, delays for smooth UX, and corner-case handling)
 *
 * Notes:
 * - Money arithmetic uses BigDecimal with scale 2 (cents).
 * - PINs are stored as SHA-256 hashes (not reversible).
 * - All operations require authentication where applicable.
 * - Delay() method simulates ATM-like smooth UI transitions.
 */
class Account {
    private final String accountNumber;
    private final String accountHolderName;
    private BigDecimal balance;
    private final List<String> transactionHistory;
    private final String pinHash; // SHA-256 hash of the PIN
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Account(String accountNumber, String accountHolderName, BigDecimal initialDeposit, String plainPin) {
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.balance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.transactionHistory = new ArrayList<>();
        this.pinHash = hashPin(plainPin);

        if (initialDeposit != null && initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
            deposit(initialDeposit, false); // internal deposit doesn't print messages
            addTransaction("Account created with initial deposit: " + formatMoney(initialDeposit));
        } else {
            addTransaction("Account created with no initial deposit.");
        }
    }

    private static String hashPin(String pin) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(pin.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public boolean verifyPin(String plainPin) {
        if (plainPin == null) return false;
        return hashPin(plainPin).equals(pinHash);
    }

    public synchronized boolean deposit(BigDecimal amount) {
        return deposit(amount, true);
    }

    private synchronized boolean deposit(BigDecimal amount, boolean verbose) {
        if (amount == null) {
            if (verbose) System.out.println("ERROR: Deposit amount cannot be null.");
            return false;
        }
        if (amount.scale() > 2) amount = amount.setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            if (verbose) System.out.println("ERROR: Deposit failed — amount must be greater than zero.");
            return false;
        }
        BigDecimal cap = new BigDecimal("1000000000.00");
        if (amount.compareTo(cap) > 0) {
            if (verbose) System.out.println("ERROR: Deposit exceeds allowed single-transaction limit.");
            return false;
        }

        balance = balance.add(amount).setScale(2, RoundingMode.HALF_UP);
        addTransaction("Deposited: " + formatMoney(amount) + " | Balance: " + formatMoney(balance));
        if (verbose) System.out.println("SUCCESS: Deposited " + formatMoney(amount));
        return true;
    }

    public synchronized boolean withdraw(BigDecimal amount) {
        if (amount == null) {
            System.out.println("ERROR: Withdrawal amount cannot be null.");
            return false;
        }
        if (amount.scale() > 2) amount = amount.setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("ERROR: Withdrawal failed — amount must be greater than zero.");
            return false;
        }
        BigDecimal cap = new BigDecimal("1000000000.00");
        if (amount.compareTo(cap) > 0) {
            System.out.println("ERROR: Withdrawal exceeds allowed single-transaction limit.");
            return false;
        }

        if (amount.compareTo(balance) > 0) {
            addTransaction("Failed withdrawal attempt: " + formatMoney(amount) + " | Balance: " + formatMoney(balance));
            System.out.println("ERROR: Withdrawal failed — insufficient funds.");
            return false;
        }

        balance = balance.subtract(amount).setScale(2, RoundingMode.HALF_UP);
        addTransaction("Withdrew: " + formatMoney(amount) + " | Balance: " + formatMoney(balance));
        System.out.println("SUCCESS: Withdrew " + formatMoney(amount));
        return true;
    }

    public synchronized BigDecimal getBalance() {
        return balance.setScale(2, RoundingMode.HALF_UP);
    }

    public synchronized List<String> getTransactionHistoryCopy() {
        return new ArrayList<>(transactionHistory);
    }

    private void addTransaction(String message) {
        String entry = "[" + LocalDateTime.now().format(TS_FORMAT) + "] " + message;
        synchronized (transactionHistory) {
            transactionHistory.add(entry);
        }
    }

    public void printAccountSummary() {
        System.out.println("\n--- Account Summary ---");
        System.out.println("Account Number : " + accountNumber);
        System.out.println("Account Holder : " + accountHolderName);
        System.out.println("Current Balance: " + formatMoney(getBalance()));
    }

    public void printTransactionHistory() {
        System.out.println("\n--- Transaction History for Account " + accountNumber + " (" + accountHolderName + ") ---");
        List<String> records = getTransactionHistoryCopy();
        if (records.isEmpty()) {
            System.out.println("No transactions found.");
        } else {
            for (String r : records) System.out.println(r);
        }
    }

    public String getAccountNumber() { return accountNumber; }
    public String getAccountHolderName() { return accountHolderName; }

    private static String formatMoney(BigDecimal amount) {
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
    public static String formatMoneyPublic(BigDecimal amount) {
        return formatMoney(amount);
    }
}

public class BankApp {
    private static final Map<String, Account> accounts = new HashMap<>();
    private static final Scanner SC = new Scanner(System.in, StandardCharsets.UTF_8);
    private static final int MAX_PIN_ATTEMPTS = 3;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            delay();
            System.out.println("\nShutting down bank application...");
        }));

        while (true) {
            delay();
            printMainMenu();
            int choice = readIntWithPrompt("Enter your choice: ");
            switch (choice) {
                case 1 -> createAccountFlow();
                case 2 -> { if (ensureAtLeastOneAccount()) depositFlow(); }
                case 3 -> { if (ensureAtLeastOneAccount()) withdrawFlow(); }
                case 4 -> { if (ensureAtLeastOneAccount()) balanceFlow(); }
                case 5 -> { if (ensureAtLeastOneAccount()) historyFlow(); }
                case 6 -> {
                    delay();
                    System.out.println("Goodbye — thank you for using Java Bank.");
                    SC.close();
                    return;
                }
                default -> { delay(); System.out.println("ERROR: Invalid choice. Please select a valid menu option."); }
            }
        }
    }

    private static void printMainMenu() {
        System.out.println("\n===== Java Bank =====");
        System.out.println("1. Create Account");
        System.out.println("2. Deposit Money");
        System.out.println("3. Withdraw Money");
        System.out.println("4. Check Balance");
        System.out.println("5. View Transaction History");
        System.out.println("6. Exit");
    }

    private static boolean ensureAtLeastOneAccount() {
        if (accounts.isEmpty()) {
            delay();
            System.out.println("INFO: No accounts found. Please create an account first (Option 1).");
            return false;
        }
        return true;
    }

    private static void createAccountFlow() {
        System.out.println("\n--- Create Account ---");
        delay();
        String accNum;
        while (true) {
            accNum = readNonEmptyString("Enter Account Number (alphanumeric, no spaces): ").trim();
            if (accNum.contains(" ")) {
                delay();
                System.out.println("ERROR: Account number cannot contain spaces.");
                continue;
            }
            if (accounts.containsKey(accNum)) {
                delay();
                System.out.println("ERROR: Account number already exists. Choose a different one.");
                continue;
            }
            break;
        }

        String name = readNonEmptyString("Enter Account Holder Name: ").trim();
        BigDecimal initialDeposit = readBigDecimalAllowZero("Enter Initial Deposit (0 allowed): ");

        // PIN setup
        String pin;
        while (true) {
            pin = readNonEmptyString("Set a 4-6 digit numeric PIN: ").trim();
            if (!pin.matches("\\d{4,6}")) {
                delay();
                System.out.println("ERROR: PIN must be 4 to 6 digits numeric.");
                continue;
            }
            String pinConfirm = readNonEmptyString("Confirm PIN: ").trim();
            if (!pin.equals(pinConfirm)) {
                delay();
                System.out.println("ERROR: PINs do not match. Try again.");
                continue;
            }
            break;
        }

        Account account = new Account(accNum, name, initialDeposit, pin);
        accounts.put(accNum, account);
        delay();
        System.out.println("SUCCESS: Account created for '" + name + "' with Account Number: " + accNum);
    }

    private static void depositFlow() {
        System.out.println("\n--- Deposit ---");
        delay();
        Account account = authenticateAccount();
        if (account == null) return;

        BigDecimal amount = readBigDecimalStrict("Enter deposit amount (greater than 0): ");
        if (amount == null) return;

        delay();
        account.deposit(amount);
    }

    private static void withdrawFlow() {
        System.out.println("\n--- Withdraw ---");
        delay();
        Account account = authenticateAccount();
        if (account == null) return;

        BigDecimal amount = readBigDecimalStrict("Enter withdrawal amount (greater than 0): ");
        if (amount == null) return;

        delay();
        account.withdraw(amount);
    }

    private static void balanceFlow() {
        System.out.println("\n--- Check Balance ---");
        delay();
        Account account = authenticateAccount();
        if (account == null) return;

        delay();
        account.printAccountSummary();
    }

    private static void historyFlow() {
        System.out.println("\n--- Transaction History ---");
        delay();
        Account account = authenticateAccount();
        if (account == null) return;

        delay();
        account.printTransactionHistory();
    }

    private static Account authenticateAccount() {
        String accNum = readNonEmptyString("Enter Account Number: ").trim();
        Account account = accounts.get(accNum);
        if (account == null) {
            delay();
            System.out.println("ERROR: Account not found. Please check the account number.");
            return null;
        }

        for (int attempt = 1; attempt <= MAX_PIN_ATTEMPTS; attempt++) {
            String pin = readNonEmptyString("Enter PIN (attempt " + attempt + "/" + MAX_PIN_ATTEMPTS + "): ").trim();
            if (account.verifyPin(pin)) {
                delay();
                return account;
            } else {
                delay();
                System.out.println("ERROR: Incorrect PIN.");
            }
        }
        delay();
        System.out.println("ERROR: Maximum PIN attempts exceeded. Operation aborted.");
        return null;
    }

    // -----------------------
    // Input helper utilities
    // -----------------------

    private static String readNonEmptyString(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line;
            try {
                line = SC.nextLine();
            } catch (NoSuchElementException | IllegalStateException e) {
                delay();
                System.out.println("ERROR: Input stream closed unexpectedly.");
                return "";
            }
            if (line == null || line.trim().isEmpty()) {
                delay();
                System.out.println("ERROR: Input cannot be empty. Try again.");
                continue;
            }
            return line;
        }
    }

    private static int readIntWithPrompt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line;
            try {
                line = SC.nextLine();
            } catch (NoSuchElementException | IllegalStateException e) {
                delay();
                System.out.println("ERROR: Input stream closed. Exiting.");
                return -1;
            }
            try {
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                delay();
                System.out.println("ERROR: Invalid number. Please enter a valid integer.");
            }
        }
    }

    private static BigDecimal readBigDecimalAllowZero(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = SC.nextLine();
            if (line == null) {
                delay();
                System.out.println("ERROR: Input closed unexpectedly.");
                return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            }
            line = line.trim();
            if (line.isEmpty()) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            try {
                BigDecimal bd = new BigDecimal(line).setScale(2, RoundingMode.HALF_UP);
                if (bd.compareTo(BigDecimal.ZERO) < 0) {
                    delay();
                    System.out.println("ERROR: Amount cannot be negative. Try again.");
                    continue;
                }
                return bd;
            } catch (NumberFormatException | ArithmeticException ex) {
                delay();
                System.out.println("ERROR: Invalid amount. Use numeric format (e.g., 1000.00).");
            }
        }
    }

    private static BigDecimal readBigDecimalStrict(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = SC.nextLine();
            if (line == null) {
                delay();
                System.out.println("ERROR: Input closed unexpectedly.");
                return null;
            }
            line = line.trim();
            if (line.isEmpty()) {
                delay();
                System.out.println("ERROR: Amount cannot be empty. Try again.");
                continue;
            }
            try {
                BigDecimal bd = new BigDecimal(line).setScale(2, RoundingMode.HALF_UP);
                if (bd.compareTo(BigDecimal.ZERO) <= 0) {
                    delay();
                    System.out.println("ERROR: Amount must be greater than zero.");
                    continue;
                }
                return bd;
            } catch (NumberFormatException | ArithmeticException ex) {
                delay();
                System.out.println("ERROR: Invalid amount. Use numeric format (e.g., 50.25).");
            }
        }
    }

    // -----------------------
    // Delay utility
    // -----------------------
    private static void delay() {
        try {
            Thread.sleep(450); // smooth ATM-like transition
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
