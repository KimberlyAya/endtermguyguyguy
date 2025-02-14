import java.sql.*;
import java.util.Scanner;

interface DatabaseManager {
    void executeUpdate(String query, Object... params) throws SQLException;
    ResultSet executeQuery(String query, Object... params) throws SQLException;
}

class SQLiteManager implements DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:contacts.db";
    private static SQLiteManager instance;

    private SQLiteManager() {
        createTables();
    }

    public static SQLiteManager getInstance() {
        if (instance == null) {
            instance = new SQLiteManager();
        }
        return instance;
    }

    private void createTables() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS contacts (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, phone TEXT NOT NULL, email TEXT NOT NULL)");
        } catch (SQLException e) {
            System.out.println("database error: " + e.getMessage());
        }
    }

    @Override
    public void executeUpdate(String query, Object... params) throws SQLException {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            pstmt.executeUpdate();
        }
    }

    @Override
    public ResultSet executeQuery(String query, Object... params) throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        PreparedStatement pstmt = conn.prepareStatement(query);
        for (int i = 0; i < params.length; i++) {
            pstmt.setObject(i + 1, params[i]);
        }
        return pstmt.executeQuery();
    }
}

//single resp
abstract class Record {
    protected final DatabaseManager dbManager;

    public Record(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public abstract void save();
}

//inheritance
class Contact extends Record {
    private final String name;
    private final String phone;
    private final String email;

    public Contact(DatabaseManager dbManager, String name, String phone, String email) {
        super(dbManager);
        this.name = name;
        this.phone = phone;
        this.email = email;
    }

    @Override
    public void save() {
        try {
            dbManager.executeUpdate("INSERT INTO contacts (name, phone, email) VALUES (?, ?, ?)", name, phone, email);
            System.out.println("Contact saved successfully!");
        } catch (SQLException e) {
            System.out.println("Error saving contact: " + e.getMessage());
        }
    }
}

public class Main {
    private static final Scanner scanner = new Scanner(System.in);
    private static final DatabaseManager dbManager = SQLiteManager.getInstance(); //singleton pattern

    public static void main(String[] args) {
        while (true) {
            System.out.println("\nContact Book");
            System.out.println("1. Add Contact");
            System.out.println("2. View Contacts");
            System.out.println("3. Search Contact");
            System.out.println("4. Delete Contact");
            System.out.println("5. Exit");
            System.out.print("Choose: ");

            if (!scanner.hasNextInt()) {
                System.out.println("Invalid");
                scanner.next();
                continue;
            }
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    addContact();
                    break;
                case 2:
                    viewContacts();
                    break;
                case 3:
                    searchContact();
                    break;
                case 4:
                    deleteContact();
                    break;
                case 5:
                    System.out.println("Bye!");
                    return;
                default:
                    System.out.println("Invalid");
            }
        }
    }

    private static void addContact() {
        System.out.print("Enter Name: ");
        String name = scanner.nextLine();
        System.out.print("Enter Phone: ");
        String phone = scanner.nextLine();
        System.out.print("Enter Email: ");
        String email = scanner.nextLine();
        new Contact(dbManager, name, phone, email).save();
    }

    private static void viewContacts() {
        try (ResultSet rs = dbManager.executeQuery("SELECT name, phone, email FROM contacts")) {
            boolean found = false;
            while (rs.next()) {
                System.out.println("Name: " + rs.getString("name") +
                        ", Phone: " + rs.getString("phone") +
                        ", Email: " + rs.getString("email"));
                found = true;
            }
            if (!found) {
                System.out.println("No contacts found.");
            }
        } catch (SQLException e) {
            System.out.println("Error getting contacts: " + e.getMessage());
        }
    }

    private static void searchContact() {
        System.out.print("Enter Name to Search: ");
        String name = scanner.nextLine();
        try (ResultSet rs = dbManager.executeQuery("SELECT name, phone, email FROM contacts WHERE name LIKE ?", "%" + name + "%")) {
            boolean found = false;
            while (rs.next()) {
                System.out.println("Name: " + rs.getString("name") +
                        ", Phone: " + rs.getString("phone") +
                        ", Email: " + rs.getString("email"));
                found = true;
            }
            if (!found) {
                System.out.println("No contacts found for: " + name);
            }
        } catch (SQLException e) {
            System.out.println("Error searching contacts: " + e.getMessage());
        }
    }

    private static void deleteContact() {
        System.out.print("Enter Name to Delete: ");
        String name = scanner.nextLine();
        try {
            dbManager.executeUpdate("DELETE FROM contacts WHERE name = ?", name);
            System.out.println("Contact deleted (if it existed).");
        } catch (SQLException e) {
            System.out.println("Error deleting contact: " + e.getMessage());
        }
    }
}
