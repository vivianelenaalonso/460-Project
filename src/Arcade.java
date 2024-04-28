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
    
    /*------------------------------------------------------------------*
    |  Function addMember()
    |
    |  Purpose: Adds a new member into the member relation in the DB.
    |
    |  Pre-Condition: The new member's information has been successfully
    |		 		  gathered and a new member ID has been constructed
    |				  for insertion.
	|
	|  Parameters:
	|	String[] info - Information for the new member in the same member
	|				    relation order memberID, name, phoneNumber, address, 
	|					ticketBalance, tier, tokenBalance
	|	Connection dbConn - Connection string for SQL query execution.
	|
	|  Returns:  None.
    *-------------------------------------------------------------------*/
    private static void addMember(String[] info, Connection dbConn) throws SQLException {
        String query = "INSERT INTO " + MEMBER_TABLE_NAME +
                       " (memberID, name, phoneNumber, address, ticketBalance, tier, tokenBalance) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement pstmt = dbConn.prepareStatement(query);
        pstmt.setString(1, info[0]); 
        pstmt.setString(2, info[1]); 
        pstmt.setString(3, info[2]);
        pstmt.setString(4, info[3]); 
        pstmt.setInt(5, Integer.parseInt(info[4]));
        pstmt.setString(6, info[5]);
        pstmt.setInt(7, Integer.parseInt(info[6]));
        int rowsInserted = pstmt.executeUpdate();
        if (rowsInserted == 1) {
        	System.out.println("Membership successfully created. Your member ID is " + info[0]);
        	pstmt.close();
        }   		
    }
    
    /*------------------------------------------------------------------*
    |  Function generateMemberId()
    |
    |  Purpose: Generates a new member ID for inserting a new member into
    |			the member table.
	|
	|  Parameters:
	|	Connection dbConn - Connection string for SQL query execution.
	|
	|  Returns:  Integer memberId that is one more than the last in the
	|			 memberId column. Does not reuse memberIds for members
	|		     who have deleted their accounts and had subsequent members
	| 			 added since.
    *-------------------------------------------------------------------*/
    private static int generateMemberId(Connection dbConn) throws SQLException {
    	ResultSet answer = null;
        String query = "SELECT memberId FROM " + MEMBER_TABLE_NAME + " ORDER BY memberID DESC";
        Statement stmt = dbConn.createStatement();
        answer = stmt.executeQuery(query);
        if (answer != null && answer.next()) {
        	int lastMemId = answer.getInt(1);
        	answer.close();
        	return (lastMemId + 1);
        }
        if (stmt != null) {
        	stmt.close();
        }
    	return -1;       
    }
    
    /*------------------------------------------------------------------*
    |  Function gatherMemberInfo()
    |
    |  Purpose: Gathers required information for a new membership to be 
    |			created in the member relation. Enforces some table field
    |			restriction to ensure proper DB insertion.
	|
	|  Parameters:
	|	Connection dbConn - Connection string for SQL query execution.
	|
	|  Returns:  String array full of member information for addMember() 
	|			 function in the proper order.
    *-------------------------------------------------------------------*/
    private static String[] gatherMemberInfo(Connection dbConn) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        String[] information = new String[7];
        int newMemId = generateMemberId(dbConn);
        if (newMemId > 0) { // generateMemberId made a new ID successfully
        	information[0] = Integer.toString(newMemId);
        }    
        else {
        	System.out.println("Generating a new member ID in gatherMemberInfo() failed.");
        }
        System.out.println("Enter your first and last name separated by a space: ");
        while (true) {
            information[1] = scanner.nextLine();
            if (information[1].matches("[a-zA-Z ]+")) {
                break;
            } 
            else {
                System.out.println("Name should contain only letters. Please enter again: ");
            }
        }  
        System.out.println("Enter phone number, including area code (e.g. 5550115155): ");
        while (true) {
            information[2] = scanner.nextLine();
            if (information[2].matches("\\d{10}")) {
                break;
            } 
            else {
                System.out.println("Phone number should be exactly ten digits long. Please enter again: ");
            }
        }
        System.out.println("Enter your address: ");
        information[3] = scanner.nextLine();
        information[4] = "0";
        information[5] = "none";
        information[6] = "0";
        scanner.close(); 
        return information;
    }
    
    /*------------------------------------------------------------------*
    |  Function isMember()
    |
    |  Purpose: Returns a boolean if a member ID within the member table.
	|
	|  Parameters:
	|	String member ID - The ID to search for and possibly locate.
	|	Connection dbConn - Connection string for SQL query execution.
	|
	|  Returns:  Boolean reflecting if an ID is in the member table.
    *-------------------------------------------------------------------*/
    private static boolean isMember(String memberID, Connection dbConn) throws SQLException {
    	ResultSet answer = null;
        String query = "SELECT COUNT(*) FROM " + MEMBER_TABLE_NAME + " WHERE memberID = " + memberID;;
        Statement stmt = dbConn.createStatement();
        answer = stmt.executeQuery(query);
        if (answer != null) {
        	if (answer.next()) {
                int count = answer.getInt(1);
                return (count > 0);
            }
        }
        if (answer != null) {
            answer.close();
        }
        if (stmt != null) {
        	stmt.close();
        }
    	return false;
    }
    
    /*------------------------------------------------------------------*
    |  Function updateMember()
    |
    |  Purpose: Updates a member's name, phone number or address
    |           in the database.
	|
	|  Parameters:
	|	String member ID - The ID to update the contents of.
	|	Connection dbConn - Connection string for SQL query execution.
	|
	|  Returns:  None.
    *-------------------------------------------------------------------*/
    private static void updateMember(Connection dbConn) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your member ID: ");
        String memberID = scanner.nextLine();
    	if (isMember(memberID, dbConn)) {
        	System.out.println("\nWhat would you like to update? "
        			+ "\n a) Name"
        			+ "\n b) Phone Number"
        			+ "\n c) Address\n");
        	String request = scanner.nextLine();
        	if (request.equalsIgnoreCase("A")) {
        		System.out.println("Enter name update: ");
        		String newName = scanner.nextLine();
                if (!newName.matches("[a-zA-Z ]+")) {
                    System.out.println("Name should contain only letters.");
                    return;
                }
                updateMemberField(memberID, "name", newName, dbConn);
        	}
        	else if (request.equalsIgnoreCase("B")) {
        		System.out.println("Enter phone update: ");
        		String newPhone = scanner.nextLine();
                if (!newPhone.matches("\\d{10}")) {
                    System.out.println("Phone number should be ten digits long in the "
                    		+ "following format: 5550115155");
                    return;
                }
                updateMemberField(memberID, "phoneNumber", newPhone, dbConn);
        	}
        	else if (request.equalsIgnoreCase("C")) {
        		System.out.println("Enter address update: ");
        		String newAddress = scanner.nextLine();
                updateMemberField(memberID, "address", newAddress, dbConn);
        	}
        	else {
        		System.out.println("Invalid entry, please choose from"
        				+ "options a to c listed above.");
        	}
    		
    		System.out.println("\n Membership successfully updated.");
    	}
    	else {
    		System.out.println("Not a valid member, check member ID and try again.");
    		return;
    	}
    }
    
    /*------------------------------------------------------------------*
    |  Function updateMemberField()
    |
    |  Purpose: Updates a specific field requested by the user in 
    |			the database.
	|
	|  Parameters:
	|	String member ID - The ID to search for and possibly locate.
	|	String field - The field to update in the DB
	|   String updateContent - The new information to update with. 
	|	Connection dbConn - Connection string for SQL query execution.
	|
	|  Returns:  None.
    *-------------------------------------------------------------------*/
    private static void updateMemberField(String memberID, String field, String updateContent, Connection dbConn) throws SQLException {
    	ResultSet answer = null;
    	String query = "UPDATE " + MEMBER_TABLE_NAME + " SET " + field 
                + " = '" + updateContent + "' WHERE memberID = '" + memberID + "'";
        Statement stmt = dbConn.createStatement();
        answer = stmt.executeQuery(query);
        if (answer != null) {
            answer.close();
        }
        if (stmt != null) {
        	stmt.close();
        }
    }
    
    /*------------------------------------------------------------------*
    |  Function delMember()
    |
    |  Purpose: Deletes a member from the database.
	|
	|  Parameters:
	|	Connection dbConn - Connection string for SQL query execution.
	|
	|  Returns:  None.
    *-------------------------------------------------------------------*/
    private static void delMember(Connection dbConn) throws SQLException {
    	Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your member ID: ");
        String memberID = scanner.nextLine();
        if (isMember(memberID, dbConn)) {
        	if (getTokens(memberID, dbConn) > 0) {
        		// Ask to exchange for prize/discount
        	}
        	else {
        		// Delete the member
        		deleteQuery(memberID, dbConn);
        		System.out.println("Member account deleted successfully.");
        	}
        }
        else {
            System.out.println("Not a valid member, check member ID and try again.");
        }
    }
    
    /*------------------------------------------------------------------*
    |  Function getTokens()
    |
    |  Purpose: Updates a specific field requested by the user in 
    |			the database.
	|
	|  Parameters:
	|	String member ID - The ID to search for and possibly locate.
	|	String field - The field to update in the DB
	|   String updateContent - The new information to update with. 
	|	Connection dbConn - Connection string for SQL query execution.
	|
	|  Returns:  The number of tokens a member currently has.
    *-------------------------------------------------------------------*/
    private static int getTokens(String memberID, Connection dbConn) throws SQLException {
    	int tokenBalance = -1;
    	Statement stmt = dbConn.createStatement();
    	String query = "SELECT tokenBalance FROM " + MEMBER_TABLE_NAME + " WHERE memberID = " + memberID;      
    	ResultSet answer = stmt.executeQuery(query);
    	if (answer != null && answer.next()) {
            tokenBalance = answer.getInt("tokenBalance");
        }
    	if (answer != null) {
            answer.close();
        }
        if (stmt != null) {
        	stmt.close();
        }
        return tokenBalance;
    }
    
    /*------------------------------------------------------------------*
    |  Function deleteQuery()
    |
    |  Purpose: Deletes the specified member using their ID from the 
    |			member relation.
	|
	|  Parameters:
	|	String member ID - The ID to search for and possibly locate.
	|	Connection dbConn - Connection string for SQL query execution.
	|
	|  Returns:  None.
    *-------------------------------------------------------------------*/
    private static void deleteQuery(String memberID, Connection dbConn) throws SQLException {
    	Statement stmt = dbConn.createStatement();
    	String query = "DELETE FROM " + MEMBER_TABLE_NAME + " WHERE memberID = " + memberID;     
    	ResultSet answer = stmt.executeQuery(query);
    	if (answer != null) {
            answer.close();
        }
        if (stmt != null) {
        	stmt.close();
        }
    }
    
    private static void queryOne() {
    	
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

        if (!prizeData.next()) {
            System.out.println("No prizes available!");
            return;
        }
        prizeData.beforeFirst();

        String memberID = "//TODO figure out command format";
        String memberQuery = "SELECT * FROM " + MEMBER_TABLE_NAME + " WHERE memberID = " + memberID;
        ResultSet memberData = null;
        memberData = stmt.executeQuery(memberQuery);
        if (!memberData.next()) {
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
            } 
            else if (command[1].equalsIgnoreCase("GAME")) {
                // TODO: addGame
            } 
            else if (command[1].equalsIgnoreCase("MEMBER")) {
                String[] userInput = gatherMemberInfo(dbConn);
                addMember(userInput, dbConn);         
            }
        } 
        else if (command[0].equals("DELETE")) {
            if (command[1].equalsIgnoreCase("PRIZE")) {
                delPrize(dbConn, command);
            } 
            else if (command[1].equalsIgnoreCase("GAME")) {
                // TODO: delGame
            } 
            else if (command[1].equalsIgnoreCase("MEMBER")) {
            	delMember(dbConn);
            }
        } else if (command[0].equals("UPDATE")) {
            if (command[1].equalsIgnoreCase("PRIZE")) {
                searchPrize(dbConn, command);
            } 
            else if (command[1].equalsIgnoreCase("GAME")) {
                // TODO: searchGame
            } 
            else if (command[1].equalsIgnoreCase("MEMBER")) {
            	updateMember(dbConn);
            }
        } 
        else if (command[0].equals("PLAY")) {
            // TODO: Implement function that stores a player's score from a game.
        }
    }

    public static void main(String[] args) throws SQLException {
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

        System.out.println("This should be true " + isMember("37", dbconn));
        delMember(dbconn);
        System.out.println("This should be false " + isMember("37", dbconn));
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
