package org.hyperskill.app;

import java.sql.*;

public class CardService {
    private final String url;

    public CardService(String fileName) {
        url = "jdbc:sqlite:" + fileName;
        createTableCard();
    }

    public void save(Card card) {
        try (Connection connection = DriverManager.getConnection(url)) {
            if (connection != null) {
                String query = "UPDATE card SET balance = ? WHERE id = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    preparedStatement.setInt(1, card.getBalance());
                    preparedStatement.setInt(2, card.getId());
                    preparedStatement.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void doTransfer(Card card, Card recipientCard, int amount) {
        try (Connection connection = DriverManager.getConnection(url)) {
            if (connection != null) {
                connection.setAutoCommit(false);
                String query = "UPDATE card SET balance = ? WHERE id = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    card.withdrawBalance(amount);
                    preparedStatement.setInt(1, card.getBalance());
                    preparedStatement.setInt(2, card.getId());
                    preparedStatement.executeUpdate();
                    recipientCard.topUpBalance(amount);
                    preparedStatement.setInt(1, recipientCard.getBalance());
                    preparedStatement.setInt(2, recipientCard.getId());
                    preparedStatement.executeUpdate();
                    connection.commit();
                } catch (SQLException ex) {
                    connection.rollback();
                    ex.printStackTrace();
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();

        }
    }

    public Card findByNumber(String cardNumber) {
        Card card = null;
        try (Connection connection = DriverManager.getConnection(url)) {
            if (connection != null) {
                String query = "SELECT id, number, pin, balance FROM card WHERE number = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    preparedStatement.setString(1, cardNumber);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        card = new Card(resultSet.getInt("id"),
                                resultSet.getString("number"),
                                resultSet.getString("pin"),
                                resultSet.getInt("balance"));

                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return card;
    }

    public Card findById(int id) {
        Card card = null;
        try (Connection connection = DriverManager.getConnection(url)) {
            if (connection != null) {
                String query = "SELECT id, number, pin, balance FROM card WHERE id = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    preparedStatement.setInt(1, id);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        card = new Card(resultSet.getInt("id"),
                                resultSet.getString("number"),
                                resultSet.getString("pin"),
                                resultSet.getInt("balance"));

                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return card;
    }

    public void add(Card card) {
        try (Connection connection = DriverManager.getConnection(url)) {
            if (connection != null) {
                String query = "INSERT INTO card (number, pin, balance) VALUES (?, ?, ?);";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query,
                        Statement.RETURN_GENERATED_KEYS)) {
                    preparedStatement.setString(1, card.getNumber());
                    preparedStatement.setString(2, card.getPin());
                    preparedStatement.setInt(3, card.getBalance());
                    int affectedRows = preparedStatement.executeUpdate();
                    if (affectedRows == 0) {
                        throw new SQLException("Creating user failed, no rows affected.");
                    }
                    try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            card.setId(generatedKeys.getInt(1));
                        } else {
                            throw new SQLException("Creating user failed, no ID obtained.");
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void closeCard(int id) {
        try (Connection connection = DriverManager.getConnection(url)) {
            if (connection != null) {
                String query = "DELETE FROM card WHERE id = ?;";
                try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                    preparedStatement.setInt(1, id);
                    preparedStatement.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void createTableCard() {
        try (Connection connection = DriverManager.getConnection(url)) {
            if (connection != null) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS card (" +
                            "id INTEGER PRIMARY KEY," +
                            "number TEXT NOT NULL," +
                            "pin TEXT NOT NULL," +
                            "balance INTEGER DEFAULT 0" +
                            ");");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}