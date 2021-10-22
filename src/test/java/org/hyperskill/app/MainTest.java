package org.hyperskill.app;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainTest {
    private static ByteArrayOutputStream output;
    private static ByteArrayInputStream input;
    private static final PrintStream DEFAULT_STDOUT = System.out;
    private static final InputStream DEFAULT_STDIN = System.in;

    private final String databaseFileName = "card.s3db";
    private final String tempDatabaseFileName = "tempDatabase.s3db";
    private final String[] args = {"-fileName", databaseFileName};
    private final Map<String, String> correctData = new HashMap<>();

    private final Pattern cardNumberPattern = Pattern.compile("^400000\\d{10}$", Pattern.MULTILINE);
    private final Pattern pinPattern = Pattern.compile("^\\d{4}$", Pattern.MULTILINE);

    private static Connection connection;

    private void provideInput(String data) {
        input = new ByteArrayInputStream(data.getBytes());
        System.setIn(input);
    }

    private String getOutput() {
        return output.toString();
    }

    @BeforeEach
    public void setUpStreams() {
        output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));
    }

    @AfterEach
    public void rollbackChangesToStdout() {
        System.setOut(DEFAULT_STDOUT);
    }

    @AfterEach
    public void rollbackChangesToStdin() {
        System.setIn(DEFAULT_STDIN);
    }

    @Test
    public void test1_checkDatabaseFile() {
        try {
            Files.deleteIfExists(Paths.get(tempDatabaseFileName));
            Files.deleteIfExists(Paths.get(databaseFileName));
        } catch (Exception ignored) {
        }

        provideInput("0");
        Main.main(args);

        File file = new File(databaseFileName);

        Assertions.assertTrue(file.exists(), "You should create a database file " +
                "named " + databaseFileName + ". The file name should be taken from the command line arguments.\n" +
                "The database file shouldn't be deleted after stopping the program!");
    }

    @Test
    public void test2_checkConnection() {
        provideInput("0");
        Main.main(args);

        getConnection();
        closeConnection();
    }

    @Test
    public void test3_checkIfTableExists() {
        provideInput("0");
        Main.main(args);

        try {
            ResultSet resultSet = getConnection().createStatement().executeQuery(
                    "SELECT name FROM sqlite_master WHERE type ='table' AND name NOT LIKE 'sqlite_%';");
            while (resultSet.next()) {
                if (resultSet.getString("name").equals("card")) {
                    closeConnection();
                    return;
                }
            }
        } catch (SQLException e) {
            closeConnection();
            Assertions.fail("Can't execute a query in your database! Make sure that your database isn't broken and you close your connection at the end of the program!");
            return;
        }

        closeConnection();
        Assertions.fail("Your database doesn't have a table named 'card'");
        Assertions.assertNull(connection);
    }

    @Test
    public void test4_checkColumns() {
        provideInput("0");
        Main.main(args);

        try {

            ResultSet resultSet = getConnection().createStatement().executeQuery("PRAGMA table_info(card);");
            Map<String, String> columns = new HashMap<>();

            while (resultSet.next()) {
                columns.put(resultSet.getString("name").toLowerCase(), resultSet.getString("type").toUpperCase());
            }

            String[][] correctColumns = {
                    {"id", "INTEGER", "INT"},
                    {"number", "TEXT", "VARCHAR"},
                    {"pin", "TEXT", "VARCHAR"},
                    {"balance", "INTEGER", "INT"}};

            for (String[] correctColumn : correctColumns) {
                String errorMessage = "Can't find '" + correctColumn[0] + "' column with '" + correctColumn[1] + "' type.\n" +
                        "Your table should have columns described in " +
                        "the stage instructions.";
                if (!columns.containsKey(correctColumn[0])) {
                    Assertions.fail(errorMessage);
                    return;
                } else if (!columns.get(correctColumn[0]).contains(correctColumn[1]) && !columns.get(correctColumn[0]).contains(correctColumn[2])) {
                    Assertions.fail(errorMessage);
                    return;
                }
            }
        } catch (SQLException e) {
            Assertions.fail("Can't connect to the database!");
            return;
        }

        closeConnection();
        Assertions.assertNull(connection);
    }

    @Test
    public void test5_checkAddingRowsToTheTable() {
        deleteAllRows();

        provideInput("1\n0");
        Main.main(args);

        if (!getData(getOutput())) {
            Assertions.fail("You should output card number and PIN like in example\n" +
                    "Or it doesn't pass the Luhn algorithm");
            return;
        }

        try {

            ResultSet resultSet = getConnection().createStatement().executeQuery("SELECT * FROM card");
            Map<String, String> userData = new HashMap<>();

            while (resultSet.next()) {
                System.out.println(resultSet.getString("number"));
                if (resultSet.getString("number") == null) {
                    Assertions.fail("The card number shouldn't be null in the database!");
                    return;
                }
                if (resultSet.getInt("balance") != 0) {
                    Assertions.fail("Default balance value should be 0 in the database!");
                    return;
                }
                if (resultSet.getString("pin") == null) {
                    Assertions.fail("The PIN shouldn't be null in the database!");
                    return;
                }
                userData.put(resultSet.getString("number"), resultSet.getString("pin"));
            }

            for (Map.Entry<String, String> entry : correctData.entrySet()) {
                if (!userData.containsKey(entry.getKey())) {
                    Assertions.fail("Your database doesn't save newly created cards.");
                    return;
                } else if (!userData.get(entry.getKey()).equals(entry.getValue())) {
                    Assertions.fail("Correct PIN for card number " + entry.getKey() + " should " +
                            "be " + entry.getValue());
                    return;
                }
            }


        } catch (SQLException e) {
            Assertions.fail("Can't connect the database!");
            return;
        }

        closeConnection();
        Assertions.assertNull(connection);
    }

    @Test
    public void test6_checkLogIn() {

        provideInput("1\n0");
        Main.main(args);
        Matcher cardNumberMatcher = cardNumberPattern.matcher(getOutput());

        if (!cardNumberMatcher.find()) {
            Assertions.fail("You are printing the card number " +
                    "incorrectly. The card number should look like in the example:" +
                    " 400000DDDDDDDDDD, where D is a digit.");
            return;
        }

        Matcher pinMatcher = pinPattern.matcher(getOutput());

        if (!pinMatcher.find()) {
            Assertions.fail("You are printing the card PIN " +
                    "incorrectly. The PIN should look like in the example: DDDD, where D is a digit.");
            return;
        }

        String correctPin = pinMatcher.group().trim();
        String correctCardNumber = cardNumberMatcher.group();

        setUpStreams();
        provideInput("2\n" + correctCardNumber + "\n" + correctPin + "\n0");
        Main.main(args);

        if (!getOutput().toLowerCase().contains("successfully")) {
            Assertions.fail("The user should be signed in after" +
                    " entering the correct card information.");
        }
    }

    @Test
    public void test7_checkBalance() {

        provideInput("1\n0");
        Main.main(args);

        Matcher cardNumberMatcher = cardNumberPattern.matcher(getOutput());
        Matcher pinMatcher = pinPattern.matcher(getOutput());

        if (!cardNumberMatcher.find() || !pinMatcher.find()) {
            Assertions.fail("You should output card number and PIN like in example");
            return;
        }

        String correctPin = pinMatcher.group().trim();
        String correctCardNumber = cardNumberMatcher.group();

        setUpStreams();
        provideInput("2\n" + correctCardNumber + "\n" + correctPin + "\n1\n0");
        Main.main(args);

        if (!getOutput().contains("0")) {
            Assertions.fail("Expected balance: 0");
        }
    }

    private Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFileName);
            } catch (SQLException exception) {
                Assertions.fail("Can't connect to the database! Make sure you close your database" +
                        " connection at the end of the program!");
            }
        }
        return connection;
    }

    private void closeConnection() {
        if (connection == null)
            return;
        try {
            connection.close();
        } catch (SQLException ignored) {
        }
        connection = null;
    }

    private boolean getData(String out) {

        Pattern cardNumberPattern = Pattern.compile("400000\\d{10}");
        Matcher cardNumberMatcher = cardNumberPattern.matcher(out);

        Pattern pinPattern = Pattern.compile("^\\d{4}$", Pattern.MULTILINE);
        Matcher pinMatcher = pinPattern.matcher(out);

        if (!cardNumberMatcher.find() || !pinMatcher.find()) {
            return false;
        }

        String number = cardNumberMatcher.group();
        String PIN = pinMatcher.group();

        if (!checkLuhnAlgorithm(number)) {
            return false;
        }

        correctData.put(number, PIN);

        return true;
    }

    private boolean checkLuhnAlgorithm(String cardNumber) {
        int result = 0;
        for (int i = 0; i < cardNumber.length(); i++) {
            int digit = Character.getNumericValue(cardNumber.charAt(i));
            if (i % 2 == 0) {
                int doubleDigit = digit * 2 > 9 ? digit * 2 - 9 : digit * 2;
                result += doubleDigit;
                continue;
            }
            result += digit;
        }
        return result % 10 == 0;
    }

    private void deleteAllRows() {
        try {
            getConnection().createStatement().execute("DELETE FROM card");
            closeConnection();
        } catch (SQLException exception) {
            Assertions.fail("Can't execute a query in your database! Make sure that your database isn't broken and you close your connection at the end of the program!");
        }
    }

}
