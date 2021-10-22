package org.hyperskill.app;

public class Main {
    public static void main(String[] args) {
        String fileName = args.length == 0 ? "database.db" : args[1];
        Bank bank = new Bank(fileName);
        bank.ready();
    }
}