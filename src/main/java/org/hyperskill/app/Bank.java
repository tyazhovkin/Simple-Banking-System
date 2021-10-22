package org.hyperskill.app;

import java.util.Random;
import java.util.Scanner;

public class Bank {
    private int currentIdCard;
    private State state = State.MAIN;
    private final Scanner scanner = new Scanner(System.in);
    private final CardService cardService;

    public Bank(String fileName) {
        this.cardService = new CardService(fileName);
    }

    public void ready() {
        boolean exit = false;
        String input;
        while (!exit) {
            displayMenu();
            input = scanner.nextLine();
            if (state == State.MAIN) {
                switch (input) {
                    case "1":
                        createAccount();
                        break;
                    case "2":
                        logIntoAccount();
                        break;
                    case "0":
                        exit = true;
                        break;
                }
            } else if (state == State.AUTHORIZED) {
                switch (input) {
                    case "1":
                        displayBalance();
                        break;
                    case "2":
                        addIncome();
                        break;
                    case "3":
                        doTransfer();
                        break;
                    case "4":
                        closeAccount();
                        break;
                    case "5":
                        logOut();
                        break;
                    case "0":
                        exit = true;
                        break;
                }
            }
        }
        System.out.println("\nBy!");
    }

    private void displayMenu() {
        System.out.println();
        if (state == State.MAIN) {
            System.out.println("1. Create an account");
            System.out.println("2. Log into account");
            System.out.println("0. Exit");
        } else if (state == State.AUTHORIZED) {
            System.out.println("1. Balance");
            System.out.println("2. Add income");
            System.out.println("3. Do transfer");
            System.out.println("4. Close account");
            System.out.println("5. Log out");
            System.out.println("0. Exit");
        }
    }

    private void createAccount() {
        Random random = new Random();

        StringBuilder cardNumber = new StringBuilder();
        cardNumber.append("400000");
        for (int i = 0; i < 9; i++) {
            cardNumber.append(random.nextInt(10));
        }
        cardNumber.append("0");

        StringBuilder pin = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            pin.append(random.nextInt(10));
        }

        int checksum = getChecksum(cardNumber.toString());
        cardNumber.deleteCharAt(cardNumber.length() - 1).append(checksum);
        Card card = new Card(0, cardNumber.toString(), pin.toString(), 0);
        cardService.add(card);
        System.out.println();
        System.out.println("Your card has been created");
        System.out.println("Your card number:");
        System.out.println(card.getNumber());
        System.out.println("Your card PIN:");
        System.out.println(pin.toString());
    }

    private int getChecksum(String input) {
        int count = 1;
        int sum = 0;
        for (int i = 0; i < input.length(); i++) {
            int x = Integer.parseInt(String.valueOf(input.charAt(i)));
            if (i != input.length() - 1) {
                if (count % 2 != 0) {
                    x = x * 2;
                }
                if (x > 9) {
                    x -= 9;
                }
                sum += x;
            }
            count++;
        }

        int copySum = sum;
        while (copySum % 10 != 0) {
            copySum++;
        }
        return copySum - sum;
    }

    private void logIntoAccount() {
        System.out.println();
        System.out.println("Enter your card number:");
        String cardNumber = scanner.nextLine();
        System.out.println("Enter your PIN:");
        String pin = scanner.nextLine();
        System.out.println();

        Card currentCard = cardService.findByNumber(cardNumber);
        if (currentCard == null || !checkCorrectPin(currentCard.getPin(), pin)) {
            System.out.println("Wrong card number or PIN!");
        } else {
            state = State.AUTHORIZED;
            currentIdCard = currentCard.getId();
            System.out.println("You have successfully logged in!");
        }

    }

    private boolean checkCorrectPin(String currentPin, String pin) {
        boolean result = true;
        if (!currentPin.equals(pin)) {
            result = false;
        }
        return result;
    }

    private void displayBalance() {
        System.out.println();
        Card currentCard = cardService.findById(currentIdCard);
        System.out.printf("Balance: %d\n", currentCard.getBalance());
    }

    private void addIncome() {
        System.out.println();
        System.out.println("Enter income:");
        String input = scanner.nextLine();
        if (input.matches("\\d+")) {
            Card currentCard = cardService.findById(currentIdCard);
            currentCard.topUpBalance(Integer.parseInt(input));
            cardService.save(currentCard);
            System.out.println("Income was added!");
        }
    }

    private void doTransfer() {
        Card currentCard = cardService.findById(currentIdCard);
        System.out.println();
        System.out.println("Transfer");
        System.out.println("Enter card number:");
        String recipientCardNumber = scanner.nextLine();
        int index = recipientCardNumber.length() - 1;
        if (getChecksum(recipientCardNumber) !=
                Integer.parseInt(recipientCardNumber.substring(index, index + 1))) {
            System.out.println("Probably you made a mistake in the card number. Please try again!");
            return;
        }
        Card recipientCard = cardService.findByNumber(recipientCardNumber);
        if (recipientCard == null) {
            System.out.println("Such a card does not exist.");
            return;
        }
        if (currentCard.equals(recipientCard)) {
            System.out.println("You can't transfer money to the same account!");
            return;
        }
        System.out.println("Enter how much money you want to transfer:");
        String input = scanner.nextLine();
        if (input.matches("\\d+")) {
            int amount = Integer.parseInt(input);
            if (currentCard.getBalance() < amount) {
                System.out.println("Not enough money!");
                return;
            }
            cardService.doTransfer(currentCard, recipientCard, amount);
            System.out.println("Success!");
        }
    }

    private void closeAccount() {
        cardService.closeCard(currentIdCard);
        state = State.MAIN;
    }

    private void logOut() {
        System.out.println();
        state = State.MAIN;
        System.out.println("You have successfully logged out!");
    }
}