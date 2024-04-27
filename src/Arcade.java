import java.util.Scanner;
import java.sql.*;

public class Arcade {
    private static final String ORACLE_URL = "jdbc:oracle:thin:@aloe.cs.arizona.edu:1521:oracle";
    private static final String ARCADE_GAME_TABLE_NAME = "juliusramirez.ArcadeGame";
    private static final String BASE_TRANSACTION_TABLE_NAME = "juliusramirez.BaseTransaction";
    private static final String COUPON_TABLE_NAME = "juliusramirez.Coupon";
    private static final String COUPON_DETAIL_TABLE_NAME = "juliusramirez.CouponDetail";
    private static final String COUPON_TRANSACTION_TABLE_NAME = "juliusramirez.CouponTransaction";
    private static final String GAME_STAT_NAME = "juliusramirez.GameStat";
    private static final String GAME_TRANSACTION_NAME = "juliusramirez.GameTransaction";
    private static final String MEMBER_TABLE_NAME = "juliusramirez.Member";
    private static final String PRIZE_TABLE_NAME = "juliusramirez.Prize";
    private static final String PRIZE_TRANSACTION_TABLE_NAME = "juliusramirez.PrizeTransaction";
    private static final String TOKEN_PURCHASE_TIER_TABLE_NAME = "juliusramirez.TokenPurchaseTier";
    private static final String TOKEN_TRANSACTION_TABLE_NAME = "juliusramirez.TokenTransaction";

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
                            + String.format("%-10s", answermetadata.getColumnName(3)));
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
                                + String.format("%-10s", answer.getString("amount")));
            }
        }
    }

    /**
     * This function prints the prizes that the member is able to purchase based on
     * the amount of tickets they have.
     * 
     * Query 3
     * 
     * @param dbConn  Connection to the database
     * @param command String array of the command
     * @throws SQLException
     */
    public static void validPrizePurchases(Connection dbConn, String[] command) throws SQLException {
        ResultSet prizeData = null;
        Statement stmt = dbConn.createStatement();
        String prizeQuery = "SELECT * FROM " + PRIZE_TABLE_NAME;
        prizeData = stmt.executeQuery(prizeQuery);

        if (prizeData == null) {
            System.out.println("No prizes available!");
            return;
        }

        String memberID = "//TODO populate here";
        String memberQuery = "SELECT * FROM " + MEMBER_TABLE_NAME + " WHERE memberID = " + memberID;
        ResultSet memberData = null;
        memberData = stmt.executeQuery(memberQuery);
        if (memberData == null) {
            System.out.println("Member not found!");
            return;
        }

        int ticketBalance = memberData.getInt("ticketBalance");
        String tier = memberData.getString("tier");
        if (tier.equals("Gold")) { // 10% discount
            printPrizes(prizeData, ticketBalance, 0.9);
        } else if (tier.equals("Diamond")) { // 20% discount
            printPrizes(prizeData, ticketBalance, 0.8);
        } else { // no discount
            printPrizes(prizeData, ticketBalance, 1.0);
        }

    }

    /**
     * Helper function to print the prizes that the member is able to purchase.
     * It is called by validPrizePurchases. Note that the discount applied rounds
     * up to the nearest whole number.
     * 
     * @param prizeData     ResultSet of all prizes
     * @param ticketBalance int ticket balance of the member
     * @param discount      double representing the discount the member has
     *                      (0.8, 0.9, or 1.0)
     * @throws SQLException
     */
    public static void printPrizes(ResultSet prizeData, int ticketBalance, double discount) throws SQLException {
        System.out.println("Prizes available for purchase:");
        System.out.println("Prize ID | Prize Name | Ticket Price (inlcuding discount)");
        System.out.println("-------------------------------------------------");
        while (prizeData.next()) {
            int ticketAmount = prizeData.getInt("basePrice");
            int finalPrice = (int) Math.ceil((double) ticketAmount * discount);
            if (ticketBalance >= finalPrice) {
                String output = prizeData.getString("prizeID") + " | " + prizeData.getString("name") + " | "
                        + ticketAmount + " | " + finalPrice;
                System.out.println(output);
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

    public static void main(String[] args) {
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
            dbconn = DriverManager.getConnection(ORACLE_URL, username, password);

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
