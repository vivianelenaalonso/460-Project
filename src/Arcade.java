import java.util.Scanner;
import java.sql.*;

public class Arcade {
    private static final String PRIZE_TABLE_NAME = "prize_table_name";
    private static final String ORACLE_URL = 
                        "jdbc:oracle:thin:@aloe.cs.arizona.edu:1521:oracle";

    public static void addPrize(Connection dbConn, String[] command) throws SQLException {
        String prizeId = command[2];
        String prizeName = command[3];
        String ticketAmt = command[4];

        String query = "INSERT INTO " + PRIZE_TABLE_NAME + """
        ( 
            prizeID, name, amount
        ) VALUES ( """ + 
            "'" + prizeId.replaceAll("'", "''") + "'," +
            "'" + prizeName.replaceAll("'", "''") + "'," +
            ticketAmt.replaceAll("'", "''") +
        " )";

        Statement stmt = dbConn.createStatement();
        stmt.executeUpdate(query);
        stmt.close();
    }



    public static void delPrize(Connection dbConn, String[] command) throws SQLException {
        String prizeId = command[2];
        String query = "DELETE FROM " + PRIZE_TABLE_NAME + """
        WHERE prizeID = """ + 
            "'" + prizeId.replaceAll("'", "''") + "'";

        Statement stmt = dbConn.createStatement();
        stmt.executeUpdate(query);
        stmt.close();
    }

    public static void searchPrize(Connection dbConn, String[] command) throws SQLException {
        String prizeName = command[2];
        ResultSet answer = null;

        String query = "SELECT * FROM " + PRIZE_TABLE_NAME + """
                WHERE name = """ + 
                "'" + prizeName.replaceAll("'", "''") + "'";
        Statement stmt = dbConn.createStatement();
        answer = stmt.executeQuery(query);

        if (answer != null) {
            // Get the data about the query result to learn
            // the attribute names and use them as column headers
            ResultSetMetaData answermetadata = answer.getMetaData();
            System.out.println(
                String.format("%-20s", answermetadata.getColumnName(1))
                + " | "
                + String.format("%-20s", answermetadata.getColumnName(2))
                + " | "
                + String.format("%-10s", answermetadata.getColumnName(3))
            );
            for (int i = 1; i <= 50; i++) {
                System.out.print("-");
            }
            System.out.println();

            // Use next() to advance cursor through the result
            // tuples and print their attribute values
            while (answer.next()) {
                System.out.println(
                    String.format("%-20s", answer.getString("prizeID"))
                    + " | "
                    + String.format("%-20s", answer.getString("name"))
                    + " | "
                    + String.format("%-10s", answer.getString("amount"))
                    );
            }
        }
    }

    public static void processQuery(String[] command, Connection dbConn) throws SQLException {
        if (command[0].equals("ADD")) {
            if (command[1].equalsIgnoreCase("PRIZE")) {
                addPrize(dbConn, command);
            } else if (command[1].equalsIgnoreCase("GAME")) {
                // TODO: addGame
            } else if (command[1].equalsIgnoreCase("MEMBER")) { 
                // TODO: addMember
            }
        } else if (command[0].equals("DELETE")) {
            if (command[1].equalsIgnoreCase("PRIZE")) {
                delPrize(dbConn, command);
            } else if (command[1].equalsIgnoreCase("GAME")) {
                // TODO: delGame
            } else if (command[1].equalsIgnoreCase("MEMBER")) { 
                // TODO: delMember
            }
        } else if (command[0].equals("SEARCH")) {
            if (command[1].equalsIgnoreCase("PRIZE")) {
                searchPrize(dbConn, command);
            } else if (command[1].equalsIgnoreCase("GAME")) {
                // TODO: searchGame
            } else if (command[1].equalsIgnoreCase("MEMBER")) { 
                // TODO: searchMember
            }
        } else if (command[0].equals("PLAY")) {
            // TODO: Implement function that stores a player's score from a game.
        }
    }

    public static void main (String[] args) {
        String username = args[0];
        String password = args[1];

        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            System.err.println("*** ClassNotFoundException:  "
                + "Error loading Oracle JDBC driver.  \n"
                + "\tPerhaps the driver is not on the Classpath?");
            System.exit(-1);
        }

        Connection dbconn = null;
        try {
            dbconn = DriverManager.getConnection(ORACLE_URL,username,password);

        } catch (SQLException e) {
            System.err.println("*** SQLException:  "
                + "Could not open JDBC connection.");
            System.err.println("\tMessage:   " + e.getMessage());
            System.err.println("\tSQLState:  " + e.getSQLState());
            System.err.println("\tErrorCode: " + e.getErrorCode());
            System.exit(-1);
        }

        // run loop that prompts for user query.
        Scanner in = new Scanner(System.in);
        System.out.println("Welcome!\n");

        while (true) {
            System.out.println("\nPlease enter your query.");
            System.out.println("To exit, enter 'e' or 'E'.");

            String s = in.nextLine();

            // exit if given 'e' or 'E' as input
            if (s.equalsIgnoreCase("e")) {
                System.out.println("\nExiting...");
                break;
            } else {
                System.out.println("\nYou entered " + s);
                try {
                    processQuery(s.split(" "), dbconn);
                } catch (SQLException e) {
                    System.err.println("*** SQLException:  "
                    + "Failed to execute one or more queries.");
                    System.err.println("\tMessage:   " + e.getMessage());
                    System.err.println("\tSQLState:  " + e.getSQLState());
                    System.err.println("\tErrorCode: " + e.getErrorCode());
                    System.exit(-1);
                }
            }
        }
        in.close();
    }
}
