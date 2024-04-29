import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.sql.*;

public class Arcade {
	private static final String ORACLE_URL = "jdbc:oracle:thin:@aloe.cs.arizona.edu:1521:oracle";
	private static final String ARCADE_GAME_TABLE_NAME = "juliusramirez.ArcadeGame";
	private static final String BASE_TRANSACTION_TABLE_NAME = "juliusramirez.BaseTransaction";
	private static final String COUPON_TABLE_NAME = "juliusramirez.Coupon";
	private static final String COUPON_DETAIL_TABLE_NAME = "juliusramirez.CouponDetail";
	private static final String COUPON_TRANSACTION_TABLE_NAME = "juliusramirez.CouponTransaction";
	private static final String GAME_STAT_NAME = "juliusramirez.GameStats";
	private static final String GAME_TRANSACTION_NAME = "juliusramirez.GameTransaction";
	private static final String MEMBER_TABLE_NAME = "juliusramirez.Member";
	private static final String PRIZE_TABLE_NAME = "juliusramirez.Prize";
	private static final String PRIZE_TRANSACTION_TABLE_NAME = "juliusramirez.PrizeTransaction";
	private static final String TOKEN_PURCHASE_TIER_TABLE_NAME = "juliusramirez.TokenPurchaseTier";
	private static final String TOKEN_TRANSACTION_TABLE_NAME = "juliusramirez.TokenTransaction";

    public static void addPrize(Connection dbConn, String[] params) throws SQLException {
        String prizeId = params[0];
        String prizeName = params[1];
        String ticketAmt = params[2];

        String query = "INSERT INTO " + PRIZE_TABLE_NAME + """
                (
                    prizeID, name, baseprice
                ) VALUES ( """ +
                "'" + prizeId.replaceAll("'", "''") + "'," +
                "'" + prizeName.replaceAll("'", "''") + "'," +
                ticketAmt.replaceAll("'", "''") +
                " )";

        Statement stmt = dbConn.createStatement();
        stmt.executeUpdate(query);
        stmt.close();
        System.out.println("Successfully added prize " + prizeId);
    }

    public static String[] gatherPrizeInfo(Connection dbConn) throws SQLException {
        Scanner scanner = new Scanner(System.in);
        String[] information = new String[3];
        int nextPrizeId;

        ResultSet answer = null;
        String findIdsQuery = "SELECT prizeID FROM " + MEMBER_TABLE_NAME + " ORDER BY prizeID DESC";
        Statement stmt = dbConn.createStatement();
        answer = stmt.executeQuery(findIdsQuery);
        if (answer != null && answer.next()) {
            int lastMemId = answer.getInt(1);
            answer.close();
            nextPrizeId = lastMemId + 1;
        } else {
            nextPrizeId = 1;
        }

        if (stmt != null) {
            stmt.close();
        }
        
        information[1] = String.valueOf(nextPrizeId);
        System.out.println("Enter the name of the prize.");
        while (true) {
            information[1] = scanner.nextLine();
            if (information[1].matches("[a-zA-Z ]+")) {
                break;
            } else {
                System.out.println("The prize name should contain only letters. Please try again below.");
            }
        }
        System.out.println("Enter the number of tickets necessary to get this prize.");
        while (true) {
            information[2] = scanner.nextLine();
            if (information[2].matches("\\d+")) {
                break;
            } else {
                System.out.println("The cost in tickets should contain only numbers. Please try again below.");
            }
        }
        scanner.close();
        return information;
    }

	public static void delPrize(Connection dbConn, String[] command) throws SQLException {
		String prizeId = command[2];
		String query = "DELETE FROM " + PRIZE_TABLE_NAME + """
				WHERE prizeID = """ + "'" + prizeId.replaceAll("'", "''") + "'";

        Statement stmt = dbConn.createStatement();
        stmt.executeUpdate(query);
        stmt.close();
        System.out.println("Successfully deleted prize " + prizeId);
    }

	public static void searchPrize(Connection dbConn, String[] command) throws SQLException {
		String prizeName = command[2];
		ResultSet answer = null;

        String query = "SELECT * FROM " + PRIZE_TABLE_NAME + """
                WHERE CONTAINS(name, """ +
                "'" + prizeName.replaceAll("'", "''") + "')";
        Statement stmt = dbConn.createStatement();
        answer = stmt.executeQuery(query);

		if (answer != null) {
			// Get the data about the query result to learn
			// the attribute names and use them as column headers
			ResultSetMetaData answermetadata = answer.getMetaData();
			System.out.println(String.format("%-20s", answermetadata.getColumnName(1)) + " | "
					+ String.format("%-20s", answermetadata.getColumnName(2)) + " | "
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
        } else {
            System.out.println("Unable to find a prize with the name " + prizeName);
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
		String query = "INSERT INTO " + MEMBER_TABLE_NAME
				+ " (memberID, name, phoneNumber, address, ticketBalance, tier, tokenBalance) "
				+ "VALUES (?, ?, ?, ?, ?, ?, ?)";
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
	private static String[] gatherMemberInfo(Connection dbConn, Scanner scanner) throws SQLException {
		//Scanner scanner = new Scanner(System.in);
		String[] information = new String[7];
		int newMemId = generateMemberId(dbConn);
		if (newMemId > 0) { // generateMemberId made a new ID successfully
			information[0] = Integer.toString(newMemId);
		} else {
			System.out.println("Generating a new member ID in gatherMemberInfo() failed.");
		}
		System.out.println("Enter your first and last name separated by a space: ");
		while (true) {
			information[1] = scanner.nextLine();
			if (information[1].matches("[a-zA-Z ]+")) {
				break;
			} else {
				System.out.println("Name should contain only letters. Please enter again: ");
			}
		}
		System.out.println("Enter phone number, including area code (e.g. 5550115155): ");
		while (true) {
			information[2] = scanner.nextLine();
			if (information[2].matches("\\d{10}")) {
				break;
			} else {
				System.out.println("Phone number should be exactly ten digits long. Please enter again: ");
			}
		}
		System.out.println("Enter your address: ");
		information[3] = scanner.nextLine();
		information[4] = "0";
		information[5] = "none";
		information[6] = "0";
		//scanner.close();
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
		String query = "SELECT COUNT(*) FROM " + MEMBER_TABLE_NAME + " WHERE memberID = " + memberID;
		;
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
	private static void updateMember(Connection dbConn, Scanner scanner) throws SQLException {
		// Scanner scanner = new Scanner(System.in);
		System.out.println("Enter your member ID: ");
		String memberID = scanner.nextLine();
		if (isMember(memberID, dbConn)) {
			System.out.println(
					"\nWhat would you like to update? " + "\n a) Name" + "\n b) Phone Number" + "\n c) Address\n");
			String request = scanner.nextLine();
			if (request.equalsIgnoreCase("A")) {
				System.out.println("Enter name update: ");
				String newName = scanner.nextLine();
				if (!newName.matches("[a-zA-Z ]+")) {
					System.out.println("Name should contain only letters.");
					return;
				}
				updateMemberField(memberID, "name", newName, dbConn);
			} else if (request.equalsIgnoreCase("B")) {
				System.out.println("Enter phone update: ");
				String newPhone = scanner.nextLine();
				if (!newPhone.matches("\\d{10}")) {
					System.out
							.println("Phone number should be ten digits long in the " + "following format: 5550115155");
					return;
				}
				updateMemberField(memberID, "phoneNumber", newPhone, dbConn);
			} else if (request.equalsIgnoreCase("C")) {
				System.out.println("Enter address update: ");
				String newAddress = scanner.nextLine();
				updateMemberField(memberID, "address", newAddress, dbConn);
			} else {
				System.out.println("Invalid entry, please choose from" + "options a to c listed above.");
			}

			System.out.println("\n Membership successfully updated.");
		} else {
			System.out.println("Not a valid member, check member ID and try again.");
			return;
		}
		//scanner.close();
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
	private static void updateMemberField(String memberID, String field, String updateContent, Connection dbConn)
			throws SQLException {
		ResultSet answer = null;
		String query = "UPDATE " + MEMBER_TABLE_NAME + " SET " + field + " = '" + updateContent + "' WHERE memberID = '"
				+ memberID + "'";
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
	private static void delMember(Connection dbConn, Scanner scanner) throws SQLException {
		// Scanner scanner = new Scanner(System.in);
		System.out.println("Enter your member ID: ");
		String memberID = scanner.nextLine();
		if (isMember(memberID, dbConn)) {
			int tokens;
			if ((tokens = getTokens(memberID, dbConn)) > 0) {
				// Ask to exchange for prize/discount
				System.out.println("You have " + tokens + "tokens, please"
						+ " exchange them for a prize or discount before closing " + " your membership.");
				String[] commands = { "", "", memberID };
				queryThree(dbConn, commands);
			} else {
				// Delete the member
				deleteQuery(memberID, dbConn);
				System.out.println("Member account deleted successfully.");
			}
		} else {
			System.out.println("Not a valid member, check member ID and try again.");
		}
		//scanner.close();
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

	/*------------------------------------------------------------------*
	| Function queryOne()
	|
	| Purpose: This function lists all games in the arcade and the names of
	|		   the members who have the top ten current high scores.
	| 
	| Parameters:
	|  Map<String, Integer> games List of available arcade games.
	|  Connection dbConn    Connection to the database
	| 
	| Returns: None
	*-------------------------------------------------------------------*/
	private static void queryOne(Map<String, Integer> games, Connection dbConn) throws SQLException {
		System.out.println("--- Query One ---");
		System.out.println("-----------------");
		for (Map.Entry<String, Integer> entry : games.entrySet()) {
			String name = entry.getKey();
			int gameID = entry.getValue();
			Statement stmt = dbConn.createStatement();
			String query = "SELECT " + GAME_STAT_NAME + ".score, " + MEMBER_TABLE_NAME + ".name FROM " + GAME_STAT_NAME
					+ " JOIN " + MEMBER_TABLE_NAME + " ON " + GAME_STAT_NAME + ".memberID = " + MEMBER_TABLE_NAME
					+ ".memberID" + " WHERE " + GAME_STAT_NAME + ".gameID = " + gameID + " ORDER BY " + GAME_STAT_NAME
					+ ".score DESC";
			ResultSet answer = stmt.executeQuery(query);
			System.out.println("Top Ten Scores for " + name + "\n");
			int loop = 1;
			while (answer.next() && loop < 11) {
				int score = answer.getInt("score");
				String pName = answer.getString("name");
				System.out.println(loop + ". " + pName + " - Score: " + score);
				loop++;
			}
			if (loop < 11) {
				for (int i = loop; i < 11; i++) {
					System.out.println(i + ". NULL");
				}
			}
			System.out.println();
		}
	}

	/*------------------------------------------------------------------*
	| Function getArcadeGames()
	|
	| Purpose: This function gets all the current arcade games.
	| 
	| Parameters:
	|  Connection dbConn    Connection to the database
	| 
	| Returns: A hashmap of all the arcade games and their IDs. 
	*-------------------------------------------------------------------*/
	private static Map<String, Integer> getArcadeGames(Connection dbConn) throws SQLException {
		Map<String, Integer> games = new HashMap<>();
		Statement stmt = dbConn.createStatement();
		String query = "SELECT name, gameID FROM " + ARCADE_GAME_TABLE_NAME;
		ResultSet answer = stmt.executeQuery(query);
		System.out.println("\nArcade Games: ");
		int i = 0;
		while (answer != null && answer.next()) {
			i++;
			String name = answer.getString("name");
			int gameID = answer.getInt("gameID");
			System.out.println(i + ". " + name);
			games.put(name, gameID);
		}
		System.out.println();
		return games;
	}

	/*------------------------------------------------------------------*
	| Function queryThree()
	|
	| Purpose: This function prints the prizes that the member is able to purchase based on
	| the amount of tickets they have.
	| 
	| Parameters:
	|  Connection dbConn    Connection to the database
	|  String command       String array of the command
	| 
	| Returns: None
	*-------------------------------------------------------------------*/
	public static void queryThree(Connection dbConn, String[] command) throws SQLException {
		Statement stmt = dbConn.createStatement();

		String memberID = command[2];
		try {
			Integer.parseInt(memberID);
		} catch (NumberFormatException e) {
			System.out.println("Invalid member ID passed. Please enter a valid member ID.");
			return;
		}

		String memberQuery = "SELECT * FROM " + MEMBER_TABLE_NAME + " WHERE memberID = " + memberID;
		ResultSet memberData = null;
		memberData = stmt.executeQuery(memberQuery);
		if (!memberData.next()) {
			System.out.println("Member not found!");
			return;
		}

		int ticketBalance = memberData.getInt("ticketBalance");
		String tier = memberData.getString("tier");
		System.out.println("Member " + memberID + " has " + ticketBalance + " tickets and has tier: " + tier);

		ResultSet prizeData = null;
		String prizeQuery = "SELECT * FROM " + PRIZE_TABLE_NAME;
		prizeData = stmt.executeQuery(prizeQuery);

		if (tier.equals("gold")) { // 10% discount
			printPrizes(prizeData, ticketBalance, 0.9);
		} else if (tier.equals("diamond")) { // 20% discount
			printPrizes(prizeData, ticketBalance, 0.8);
		} else { // no discount
			printPrizes(prizeData, ticketBalance, 1.0);
		}

	}

	/*------------------------------------------------------------------*
	| Function queryThree()
	|
	| Purpose: Helper function to print the prizes that the member is able to purchase.
	| It is called by validPrizePurchases. Note that the discount applied rounds
	| up to the nearest whole number.
	| Parameters:
	|  ResultSet prizeData  ResultSet of all prizes
	|  int ticketBalance    int ticket balance of the member
	|  double discount      double representing the discount the member has
	| 
	| Returns: None
	*-------------------------------------------------------------------*/
	public static void printPrizes(ResultSet prizeData, int ticketBalance, double discount) throws SQLException {
		System.out.println("Prizes available for purchase:");
		System.out.println("Prize ID | Prize Name | Base Price | Discount Price");
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

	public static void processQuery(String[] command, Connection dbConn, Scanner scanner) throws SQLException {
		if (command[0].equals("ADD")) {
			if (command[1].equalsIgnoreCase("PRIZE")) {
                String[] prizeInfo = gatherPrizeInfo(dbConn);
                addPrize(dbConn, prizeInfo);
			} else if (command[1].equalsIgnoreCase("GAME")) {
				// TODO: addGame
			} else if (command[1].equalsIgnoreCase("MEMBER")) {
				String[] userInput = gatherMemberInfo(dbConn, scanner);
				addMember(userInput, dbConn);
			} else {
                System.out.println("Invalid command. You can add a prize, game or member with the command:");
                System.out.println("ADD <PRIZE | GAME | MEMBER>");
            }
		} else if (command[0].equals("DELETE")) {
			if (command[1].equalsIgnoreCase("PRIZE")) {
				delPrize(dbConn, command);
			} else if (command[1].equalsIgnoreCase("GAME")) {
				// TODO: delGame
			} else if (command[1].equalsIgnoreCase("MEMBER")) {
				delMember(dbConn, scanner);
			} else {
                System.out.println("Invalid command. You can delete a prize, game or member with the command:");
                System.out.println("DELETE <PRIZE | GAME | MEMBER>");
            }
		} else if (command[0].equals("SEARCH")) {
            if (command[1].equalsIgnoreCase("PRIZE")) {
                searchPrize(dbConn, command);
            } else {
                System.out.println("Invalid command. You can search prize, member and game names with the command:");
                System.out.println("SEARCH <PRIZE | GAME | MEMBER>");
            }
        } else if (command[0].equals("UPDATE")) {
            if (command[1].equalsIgnoreCase("PRIZE")) {
                searchPrize(dbConn, command);
            } else if (command[1].equalsIgnoreCase("GAME")) {
                // TODO: searchGame
            } else if (command[1].equalsIgnoreCase("MEMBER")) {
                updateMember(dbConn, scanner);
            } else {
                System.out.println("Invalid command. You can update a prize, member or game with the command:");
                System.out.println("UPDATE <PRIZE | GAME | MEMBER>");
            }
        } else if (command[0].equals("PLAY")) {
			// TODO: Implement function that stores a player's score from a game.
		} else if (command[0].equals("QUERY")) {
			if (command[1].equalsIgnoreCase("ONE")) {
				Map<String, Integer> games = getArcadeGames(dbConn);
				queryOne(games, dbConn);
			} else if (command[1].equalsIgnoreCase("TWO")) {
				// TODO query 2
			} else if (command[1].equalsIgnoreCase("THREE")) {
				queryThree(dbConn, command);
			} else if (command[1].equalsIgnoreCase("FOUR")) {
				// TODO custom query
			}
		}
	}

	public static void main(String[] args) throws SQLException {
		String username = args[0];
		String password = args[1];

		try {
			Class.forName("oracle.jdbc.OracleDriver");
		} catch (ClassNotFoundException e) {
			System.err.println("*** ClassNotFoundException:  " + "Error loading Oracle JDBC driver.  \n"
					+ "\tPerhaps the driver is not on the Classpath?");
			System.exit(-1);
		}

		Connection dbconn = null;
		try {
			dbconn = DriverManager.getConnection(ORACLE_URL, username, password);

		} catch (SQLException e) {
			System.err.println("*** SQLException:  " + "Could not open JDBC connection.");
			System.err.println("\tMessage:   " + e.getMessage());
			System.err.println("\tSQLState:  " + e.getSQLState());
			System.err.println("\tErrorCode: " + e.getErrorCode());
			System.exit(-1);
		}

		// run loop that prompts for user query.
		Scanner in = new Scanner(System.in);
		System.out.println("Welcome!\n");

		while (true) {
		    try {
		        System.out.println("\nPlease enter your query.");
		        System.out.println("To exit, enter 'e' or 'E'.");
		        if (!in.hasNextLine()) {
		            System.out.println("No more input available. Exiting...");
		            break; 
		        }
		        String s = in.nextLine().toUpperCase(); 

		        if (s.equalsIgnoreCase("e")) {
		            System.out.println("\nExiting...");
		            break;
		        } else {
		            System.out.println("\nYou entered " + s);
		            try {
		                processQuery(s.split(" "), dbconn, in);  
		            } catch (SQLException e) {
		                System.err.println("*** SQLException:  "
		                        + "Failed to execute one or more queries.");
		                System.err.println("\tMessage:   " + e.getMessage());
		                System.err.println("\tSQLState:  " + e.getSQLState());
		                System.err.println("\tErrorCode: " + e.getErrorCode());
		                System.exit(-1);  
		            }
		        }
		    } catch (NoSuchElementException e) {
		        System.out.println("Input stream closed unexpectedly.");
		        break; 
		    }
		}
		in.close();
	}
}