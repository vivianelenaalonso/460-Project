import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

import javax.naming.spi.DirStateFactory.Result;

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


	private static void addGameStat(Connection dbConn, Scanner scanner) throws SQLException {
		System.out.println("Enter member ID");
		String memberID = scanner.nextLine();
		ResultSet answer = null;

		while (!memberID.matches("\\d+") || !isMember(memberID, dbConn)) {
			if (memberID.toUpperCase().equals("C")) {
				System.out.println("Returning to query menu...");
				return;
			}
			System.out.println("Invalid member ID. Please try again, or press c  to cancel.");
			memberID = scanner.nextLine();
		}

		System.out.println("Enter game ID");
		String gameID = scanner.nextLine();
		while (!gameID.matches("\\d+") || !isGame(gameID, dbConn)) {
			if (gameID.toUpperCase().equals("C")) {
				System.out.println("Returning to query menu...");
				return;
			}
			System.out.println("Invalid game ID. Please try again, or press c to cancel.");
			gameID = scanner.nextLine();
		}

		int cur_balance = -1;
		int req_balance = -1;
		float tps = -1;
		int score = (int) Math.round(Math.random() * 10000);

		String getTokenBal = "SELECT tokenbalance FROM " + MEMBER_TABLE_NAME + " WHERE memberID = " + memberID;
        Statement stmt = dbConn.createStatement();
        answer = stmt.executeQuery(getTokenBal);
		
		if (answer.next()) {
			cur_balance = answer.getInt("tokenbalance");
		}
		answer.close();
		stmt.close();

		String getGameInfo = "SELECT cost, ticketsperscore FROM " + ARCADE_GAME_TABLE_NAME + " WHERE gameID = '" + gameID + "'";
		stmt = dbConn.createStatement();
        answer = stmt.executeQuery(getGameInfo);

		if (answer.next()) {
			req_balance = answer.getInt("cost");
			tps = answer.getFloat("ticketsperscore");
		}

		if (req_balance == -1 || cur_balance == -1 || req_balance > cur_balance) {
			System.out.println("Sorry, you do not have enough tokens to play this game.");
			return;
		} else {
			System.out.println("You scored " + score + ". Congratulations!");

			String updateMemQuery = "UPDATE " + MEMBER_TABLE_NAME + " SET tokenbalance = " + (cur_balance - req_balance) +
			" WHERE memberID = '" + memberID + "'";
			stmt = dbConn.createStatement();
			answer = stmt.executeQuery(updateMemQuery);
			answer.close();
			stmt.close();

			String transID = getNewTransactionID(dbConn);
			String statID = getNewStatID(dbConn);

			String insertBTQuery = "INSERT INTO " + BASE_TRANSACTION_TABLE_NAME + 
			"(transactionid, memberid, transactiondate, type) VALUES " +
			"('" + transID + "', '" + memberID +"', sysdate, 'game')";
			stmt = dbConn.createStatement();
			answer = stmt.executeQuery(insertBTQuery);
			answer.close();
			stmt.close();

			String insertPTQuery = "INSERT INTO " + GAME_TRANSACTION_NAME + 
			"(transactionid, tokensspent, ticketsearned) VALUES " +
			"('" + transID + "', " + req_balance +", " + Math.round(score * tps) + ")";
			stmt = dbConn.createStatement();
			answer = stmt.executeQuery(insertPTQuery);
			answer.close();
			stmt.close();

			String delPrizeQuery = "INSERT INTO " + GAME_STAT_NAME + " " + """
				(statID, memberID, gameID, score) VALUES """ + 
				"('" + statID + "', '" + memberID + "', '" + gameID + "', " + score + ")";

			stmt = dbConn.createStatement();
			answer = stmt.executeQuery(delPrizeQuery);
			answer.close();
			stmt.close();

			System.out.println("You have been awarded " + Math.round(score * tps) + " tickets!");
		}
	}

	private static void redeemPrize(Connection dbConn, Scanner scanner) throws SQLException {
		System.out.println("Enter member ID");
		String memberID = scanner.nextLine();
		ResultSet answer = null;

		while (!memberID.matches("\\d+") || !isMember(memberID, dbConn)) {
			if (memberID.toUpperCase().equals("C")) {
				System.out.println("Returning to query menu...");
				return;
			}
			System.out.println("Invalid member ID. Please try again, or press c  to cancel.");
			memberID = scanner.nextLine();
		}

		System.out.println("Enter prize ID");
		String prizeID = scanner.nextLine();
		while (!prizeID.matches("\\d+") || !isPrizeId(dbConn, prizeID)) {
			if (prizeID.toUpperCase().equals("C")) {
				System.out.println("Returning to query menu...");
				return;
			}
			System.out.println("Invalid prize ID. Please try again, or press c  to cancel.");
			prizeID = scanner.nextLine();
		}

		int cur_balance = -1;
		int req_balance = -1;
		String prize_name = null;

		String getTicketBalQuery = "SELECT ticketbalance FROM " + MEMBER_TABLE_NAME + " WHERE memberID = " + memberID;
        Statement stmt = dbConn.createStatement();
        answer = stmt.executeQuery(getTicketBalQuery);
		
		if (answer.next()) {
			cur_balance = answer.getInt("ticketbalance");
		}
		answer.close();
		stmt.close();

		String getPrizeCostQuery = "SELECT name, baseprice FROM " + PRIZE_TABLE_NAME + " WHERE prizeID = '" + prizeID + "'";
		stmt = dbConn.createStatement();
        answer = stmt.executeQuery(getPrizeCostQuery);

		if (answer.next()) {
			req_balance = answer.getInt("baseprice");
			prize_name = answer.getString("name");
		}

		if (req_balance == -1 || cur_balance == -1 || req_balance > cur_balance) {
			System.out.println("Sorry, you do not have enough tickets to claim this prize.");
			return;
		} else {
			String updateMemQuery = "UPDATE " + MEMBER_TABLE_NAME + " SET ticketbalance = " + (cur_balance - req_balance) +
			" WHERE memberID = '" + memberID + "'";
			stmt = dbConn.createStatement();
			answer = stmt.executeQuery(updateMemQuery);
			answer.close();
			stmt.close();

			String transID = getNewTransactionID(dbConn);

			String insertBTQuery = "INSERT INTO " + BASE_TRANSACTION_TABLE_NAME + 
			"(transactionid, memberid, transactiondate, type) VALUES " +
			"('" + transID + "', '" + memberID +"', sysdate, 'prize')";
			stmt = dbConn.createStatement();
			answer = stmt.executeQuery(insertBTQuery);
			answer.close();
			stmt.close();

			String insertPTQuery = "INSERT INTO " + PRIZE_TRANSACTION_TABLE_NAME + 
			"(transactionid, prizename, ticketsspent) VALUES " +
			"('" + transID + "', '" + prize_name +"', " + req_balance + ")";
			stmt = dbConn.createStatement();
			answer = stmt.executeQuery(insertPTQuery);
			answer.close();
			stmt.close();

			String delPrizeQuery = "DELETE FROM " + PRIZE_TABLE_NAME + " " + """
				WHERE prizeID = """ + "'" + prizeID.replaceAll("'", "''") + "'";
			stmt = dbConn.createStatement();
			answer = stmt.executeQuery(delPrizeQuery);
			answer.close();
			stmt.close();

			System.out.println("Successfully redeemed prize " + prizeID + "for member " + memberID);
		}
	}


	private static String getNewTransactionID(Connection dbConn) throws SQLException {
		int nextTransactionId;

        ResultSet answer = null;
        String findIdsQuery = "SELECT transactionID FROM " + BASE_TRANSACTION_TABLE_NAME + " ORDER BY transactionID DESC";
        Statement stmt = dbConn.createStatement();
        answer = stmt.executeQuery(findIdsQuery);
        if (answer != null && answer.next()) {
            int lastTid = answer.getInt(1);
            answer.close();
            nextTransactionId = lastTid + 1;
        } else {
            nextTransactionId = 1;
        }
		return String.valueOf(nextTransactionId);
	}

	private static String getNewStatID(Connection dbConn) throws SQLException {
		int nextStatId;

        ResultSet answer = null;
        String findIdsQuery = "SELECT statID FROM " + GAME_STAT_NAME + " ORDER BY statID DESC";
        Statement stmt = dbConn.createStatement();
        answer = stmt.executeQuery(findIdsQuery);
        if (answer != null && answer.next()) {
            int lastSid = answer.getInt(1);
            answer.close();
            nextStatId = lastSid + 1;
        } else {
            nextStatId = 1;
        }
		return String.valueOf(nextStatId);
	}

    private static void addPrize(Connection dbConn, String[] params) throws SQLException {
        String prizeId = params[0];
        String prizeName = params[1];
        String ticketAmt = params[2];

		System.out.println(prizeId + " " + prizeName + " " + ticketAmt);

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


    private static String[] gatherPrizeInfo(Connection dbConn, Scanner scanner) throws SQLException {
        String[] information = new String[3];
        int nextPrizeId;

        ResultSet answer = null;
        String findIdsQuery = "SELECT prizeID FROM " + PRIZE_TABLE_NAME + " ORDER BY prizeID DESC";
        Statement stmt = dbConn.createStatement();
        answer = stmt.executeQuery(findIdsQuery);
        if (answer != null && answer.next()) {
            int lastPid = answer.getInt(1);
            answer.close();
            nextPrizeId = lastPid + 1;
        } else {
            nextPrizeId = 1;
        }

        if (stmt != null) {
            stmt.close();
        }
        
        information[0] = String.valueOf(nextPrizeId);
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
        return information;
    }


	private static boolean isPrizeId(Connection dbConn, String prizeId) throws SQLException {
		ResultSet answer = null;

        String query = "SELECT * FROM " + PRIZE_TABLE_NAME + " " + """
                WHERE prizeID = """ +
                "'" + prizeId.replaceAll("'", "''") + "'";

        Statement stmt = dbConn.createStatement();
        answer = stmt.executeQuery(query);

		if (answer != null && answer.next()) {
			answer.close();
			return true;
		}
		answer.close();
		return false;
	}


	private static void delPrize(Connection dbConn, Scanner scanner) throws SQLException {
		System.out.println("Enter prize ID to delete:");
		String prizeId = scanner.nextLine();

		// Check that prize id valid and exists in the db.
		while (!prizeId.matches("\\d+") || !isPrizeId(dbConn, prizeId)) {
			if (prizeId.toUpperCase().equals("C")) {
				System.out.println("Returning to query menu...");
				return;
			}
			System.out.println("Invalid value for prize ID. " +
			"You can run SEARCH PRIZE <PRIZE NAME> to find prize IDs" +
			"\nPlease try another prize ID, or enter C to return to cancel.");
			prizeId = scanner.nextLine();
		}

		String query = "DELETE FROM " + PRIZE_TABLE_NAME + " " + """
				WHERE prizeID = """ + "'" + prizeId.replaceAll("'", "''") + "'";

        Statement stmt = dbConn.createStatement();
        stmt.executeUpdate(query);
        stmt.close();
        System.out.println("Successfully deleted prize " + prizeId);
    }

	private static void searchPrize(Connection dbConn, String[] command) throws SQLException {
		if (command.length < 3) {
			System.out.println("Invalid command format. Should be 'SEARCH PRIZE <SEARCH_TERM>'");
			return;
		}
		String prizeName = command[2];
		ResultSet answer = null;

        String query = "SELECT * FROM " + PRIZE_TABLE_NAME + " " + """
                WHERE regexp_like(name, """ +
                "'.*" + prizeName.replaceAll("'", "''") + ".*', 'i') ORDER BY prizeID";
        Statement stmt = dbConn.createStatement();
        answer = stmt.executeQuery(query);

		if (answer != null) {
			// Get the data about the query result to learn
			// the attribute names and use them as column headers
			ResultSetMetaData answermetadata = answer.getMetaData();
			System.out.println(String.format("%-10s", answermetadata.getColumnName(1)) + " | "
					+ String.format("%-35s", answermetadata.getColumnName(2)) + " | "
					+ String.format("%-15s", answermetadata.getColumnName(3)));
			for (int i = 1; i <= 60; i++) {
				System.out.print("-");
			}
			System.out.println();

            // Use next() to advance cursor through the result
            // tuples and print their attribute values
            while (answer.next()) {
                System.out.println(
                        String.format("%-10s", answer.getString("prizeID"))
                                + " | "
                                + String.format("%-35s", answer.getString("name"))
                                + " | "
                                + String.format("%-15s", answer.getString("baseprice")));
            }
        } else {
            System.out.println("Unable to find a prize with the name " + prizeName);
        }
    }

	private static void updatePrize(Connection dbConn, Scanner scanner) throws SQLException {
		System.out.println("Enter the ID of the prize you want to update: ");
		String prizeID = scanner.nextLine();
		ResultSet answer = null;
		

		if (isPrizeId(dbConn, prizeID)) {
			System.out.println(
					"\nWhat would you like to update? " + "\n a) Name" + "\n b) Cost (in tickets)\n");
			String request = scanner.nextLine();

			if (request.equalsIgnoreCase("A")) {
				System.out.println("Enter name update: ");
				String newName = scanner.nextLine();
				if (!newName.matches("[a-zA-Z ]+")) {
					System.out.println("Name should contain only letters.");
					return;
				}
				String query = "UPDATE " + PRIZE_TABLE_NAME + " SET name = '" + newName + "' WHERE prizeID = '"
				+ prizeID + "'";
				Statement stmt = dbConn.createStatement();
				answer = stmt.executeQuery(query);
				answer.close();
				stmt.close();
			} else if (request.equalsIgnoreCase("B")) {
				System.out.println("Enter cost update: ");
				String newCost = scanner.nextLine();
				if (!newCost.matches("\\d")) {
					System.out.println("Cost should contain only digits.");
					return;
				}
				String query = "UPDATE " + PRIZE_TABLE_NAME + " SET name = '" + newCost + "' WHERE prizeID = '"
				+ prizeID + "'";
				Statement stmt = dbConn.createStatement();
				answer = stmt.executeQuery(query);
				answer.close();
				stmt.close();
			} else {
				System.out.println("Invalid entry, please choose from" + "options a to c listed above.");
			}

			System.out.println("\n Prize" + prizeID + " was successfully updated.");
		} else {
			System.out.println("Not a valid prize ID, please check and try again.");
			return;
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

    /*------------------------------------------------------------------*
	|  Function isGame()
	|
	|  Purpose: Returns a boolean if a game ID within the game table.
	|
	|  Parameters:
	|	String game ID - The ID to search for and possibly locate.
	|	Connection dbConn - Connection string for SQL query execution.
	|
	|  Returns:  Boolean reflecting if an ID is in the arcade game table.
	*-------------------------------------------------------------------*/
    private static boolean isGame(String gameID, Connection dbConn) throws SQLException {
        ResultSet answer = null;
        String query = "SELECT COUNT(*) FROM " + ARCADE_GAME_TABLE_NAME + " WHERE gameID = " + gameID;
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
    |  Function updateGame()
    |
    |  Purpose: Updates a game's name, cost, tickets per score.
    |           in the database.
    |
    |  Parameters:
    |	String game ID - The ID to update the contents of.
    |	Connection dbConn - Connection string for SQL query execution.
    |
    |  Returns:  None.
    *-------------------------------------------------------------------*/
    private static void updateGame(Connection dbConn, Scanner scanner) throws SQLException {
        // Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your game ID: ");
        String gameID = scanner.nextLine();

        if (isGame(gameID, dbConn)) {
            System.out.println("\nWhat would you like to update? " + "\n a) Name" + "\n b) Ticket Cost" + "\n c) Tickets Per Score\n");
            String request = scanner.nextLine();

            if (request.equalsIgnoreCase("A")) {
                System.out.println("Enter new name: ");
                String newName = scanner.nextLine();

                if (!newName.matches("[a-zA-Z ]+")) {
                    System.out.println("Name should contain only letters.");
                    return;
                }

                updateGameField(gameID, "name", newName, dbConn);

            } else if (request.equalsIgnoreCase("B")) {

                System.out.println("Enter ticket cost: ");
                String newTicketCost = scanner.nextLine();
                if (!newTicketCost.matches("[0-9]+")) {
                    System.out.println("Ticket cost must be a number.");
                    return;
                }
                updateGameField(gameID, "cost", newTicketCost, dbConn);
            }
            else if (request.equalsIgnoreCase("C")) {
                System.out.println("Enter tickets per score: ");
                String newTicketsPerScore = scanner.nextLine();
                updateGameField(gameID, "ticketsperscore", newTicketsPerScore, dbConn);
            }
            else {
                System.out.println("Invalid entry, please choose from" + "options a to c listed above.");
            }

            System.out.println("\n Game successfully updated.");
        }
        else {
            System.out.println("Not a valid game, check game ID and try again.");
            return;
        }
        //scanner.close();
    }

    /*------------------------------------------------------------------*
    |  Function updateGameField()
    |
    |  Purpose: Updates a specific field requested by the user in
    |			the database.
    |
    |  Parameters:
    |	String game ID - The ID to search for and possibly locate.
    |	String field - The field to update in the DB
    |   String updateContent - The new information to update with.
    |	Connection dbConn - Connection string for SQL query execution.
    |
    |  Returns:  None.
    *-------------------------------------------------------------------*/
    private static void updateGameField(String gameID, String field, String updateContent, Connection dbConn) throws SQLException {
        ResultSet answer = null;
        String query = "UPDATE " + ARCADE_GAME_TABLE_NAME + " SET " + field + " = '" + updateContent + "' WHERE gameID = '"
                + gameID + "'";
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
    |  Function delGame()
    |
    |  Purpose: Deletes a game from the database.
    |
    |  Parameters:
    |	Connection dbConn - Connection string for SQL query execution.
    |
    |  Returns:  None.
    *-------------------------------------------------------------------*/
    private static void delGame(Connection dbConn, Scanner scanner) throws SQLException {
        System.out.println("Enter your game ID: ");
        String gameID = scanner.nextLine();
        if (isGame(gameID, dbConn)) {
            String query = "DELETE FROM " + ARCADE_GAME_TABLE_NAME + " WHERE gameID = " + "'" + gameID + "'";
            Statement stmt = dbConn.createStatement();
            stmt.executeUpdate(query);
            stmt.close();

            String query2 = "DELETE FROM " + GAME_STAT_NAME + " WHERE gameID = " + "'" + gameID + "'";
            Statement stmt2 = dbConn.createStatement();
            stmt2.executeUpdate(query2);
            stmt2.close();

        }
        else {
            System.out.println("Not a valid game, check game ID and try again.");
        }
        System.out.println("Deletion successful.");
    }

    /*------------------------------------------------------------------*
    |  Function generateGameID()
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
    private static int generateGameID(Connection dbConn) throws SQLException {
        ResultSet answer = null;
        String query = "SELECT gameID FROM " + ARCADE_GAME_TABLE_NAME + " ORDER BY gameID DESC";
        Statement stmt = dbConn.createStatement();
        answer = stmt.executeQuery(query);
        if (answer != null && answer.next()) {
            int lastGameId = answer.getInt(1);
            answer.close();
            return (lastGameId + 1);
        }
        if (stmt != null) {
            stmt.close();
        }
        return -1;
    }


    /*------------------------------------------------------------------*
    |  Function addGame()
    |
    |  Purpose: Adds a new game into the arcade game relation in the DB.
    |
    |  Pre-Condition: The new game's information has been successfully
    |		 		  gathered and a new game ID has been constructed
    |				  for insertion.
    |
    |  Parameters:
    |	String[] info - Information for the new game in the same game
    |				    relation order gameID, cost, name, ticketsperscore
    |	Connection dbConn - Connection string for SQL query execution.
    |
    |  Returns:  None.
    *-------------------------------------------------------------------*/
    private static void addGame(String[] info, Connection dbConn) throws SQLException {
        String query = "INSERT INTO " + ARCADE_GAME_TABLE_NAME
                + " (gameID, cost, name, ticketsperscore) "
                + "VALUES (?, ?, ?, ?)";
        PreparedStatement pstmt = dbConn.prepareStatement(query);
        pstmt.setString(1, info[0]);
        pstmt.setInt(2, Integer.parseInt(info[1]));
        pstmt.setString(3, info[2]);
        pstmt.setFloat(4, Float.parseFloat(info[3]));
        int rowsInserted = pstmt.executeUpdate();
        if (rowsInserted == 1) {
            System.out.println("Game successfully created. Your game ID is " + info[0]);
            pstmt.close();
        }
    }

    /*------------------------------------------------------------------*
    |  Function gatherGameInfo()
    |
    |  Purpose: Gathers required information for a new game to be
    |			created in the arcade game relation. Enforces some table field
    |			restriction to ensure proper DB insertion.
    |
    |  Parameters:
    |	Connection dbConn - Connection string for SQL query execution.
    |
    |  Returns:  String array full of member information for addGame()
    |			 function in the proper order.
    *-------------------------------------------------------------------*/
    private static String[] gatherGameInfo(Connection dbConn, Scanner scanner) throws SQLException {
        String[] information = new String[4];
        int newGameId = generateGameID(dbConn);
        if (newGameId > 0) { // generateGameID made a new ID successfully
            information[0] = Integer.toString(newGameId);
        } else {
            System.out.println("Generating a new game ID in generateGameID() failed.");
        }

        System.out.println("Enter game cost: ");
        while (true) {
            information[1] = scanner.nextLine();
            if (information[1].matches("[0-9]+")) {
                break;
            } else {
                System.out.println("Name should contain only numberss. Please enter again: ");
            }
        }
        System.out.println("Enter game name: ");
        information[2] = scanner.nextLine();

        System.out.println("Enter game tickets per score: ");
        information[3] = scanner.nextLine();
        return information;
    }

    /*------------------------------------------------------------------*
    | Function queryTwo()
    |
    | Purpose: Helper function to print out the members' names and tiers for those
    |           who have spent over $100 in the last most.
    |
    | Precondition: None
    |
    | Post-condition: No crash.
    |
    | Returns: None
    *-------------------------------------------------------------------*/
    private static void queryTwo(Connection dbConn) throws SQLException {
        Statement stmt = dbConn.createStatement();
        String query = "SELECT d.name, c.memberID, c.total, d.tier FROM (SELECT a.memberID, SUM(b.cost) total FROM (SELECT * from juliusramirez.BaseTransaction WHERE type='token') a JOIN (SELECT * FROM juliusramirez.TokenTransaction) b ON a.transactionID=b.transactionID WHERE transactiondate >= add_months(trunc(sysdate, 'month'), -1) AND transactiondate < trunc(sysdate, 'month') GROUP BY a.memberID) c JOIN juliusramirez.Member d ON c.memberID=d.memberID WHERE TOTAL >= 100";
        ResultSet answer = stmt.executeQuery(query);
        System.out.println("\nMembers who've spent over $100 in the last month.: ");
        int i = 0;
        while (answer != null && answer.next()) {
            i++;
            System.out.println("Name: " + answer.getString("name") + " Tier: " + answer.getString("tier"));
        }
        System.out.println();
    }

	private static void queryFour(Connection dbConn, String[] command, Scanner scanner) throws SQLException {
		System.out.println("Enter a member ID to view recent transaction summaries for:");
		String memberID = scanner.nextLine();

		while (!memberID.matches("\\d+") || !isMember(memberID, dbConn)) {
			if (memberID.toUpperCase().equals("C")) {
				System.out.println("Returning to query menu...");
				return;
			}
			System.out.println("Invalid member ID. Please enter another or press C to cancel.");
			memberID = scanner.nextLine();
		}

		System.out.println("Summary of transactions for member " + memberID);
		Statement stmt = dbConn.createStatement();
		ResultSet answer = null;

		// Token purchase stats for last month.
		String tokenPurchaseQuery = "SELECT sum(tokenamount) totaltok, sum(cost) totalcost from " + BASE_TRANSACTION_TABLE_NAME + ", " + TOKEN_TRANSACTION_TABLE_NAME +
		" WHERE " + BASE_TRANSACTION_TABLE_NAME + ".transactionid = " + TOKEN_TRANSACTION_TABLE_NAME + ".transactionid " +
		"AND memberid = " + memberID + " " +
		"AND transactiondate >= add_months(trunc(sysdate, 'month'), -1) AND transactiondate < trunc(sysdate, 'month')";

		//System.out.println(tokenPurchaseQuery);
		answer = stmt.executeQuery(tokenPurchaseQuery);

		if (answer.next()) {
			System.out.println("\nYou spent $" + answer.getFloat("totalcost")
			+ " on " + answer.getInt("totaltok") + 
			" tokens last month.");
		} else {
			System.out.println("You bought no video game tokens last month.");
		}
		
		// Gameplay stats for last month.
		String gameTokensQuery = "SELECT sum(ticketsearned) totalearned, sum(tokensspent) totalspent from " + BASE_TRANSACTION_TABLE_NAME + ", " + GAME_TRANSACTION_NAME +
		" WHERE " + BASE_TRANSACTION_TABLE_NAME + ".transactionid = " + GAME_TRANSACTION_NAME + ".transactionid " +
		"AND memberid = " + memberID + " " +
		"AND transactiondate >= add_months(trunc(sysdate, 'month'), -1) AND transactiondate < trunc(sysdate, 'month')";

		//System.out.println(gameTokensQuery);
		answer = stmt.executeQuery(gameTokensQuery);

		if (answer.next()) {
			System.out.println("\nYou spent " + answer.getInt("totalspent")
			+ " token(s) on games last month, and earned "
			+ answer.getInt("totalearned") + " tickets.");
		} else {
			System.out.println("You played no games last month.");
		}


		// Prize redemption stats for the last month.
		String prizesQuery = "SELECT count(prizename) numprizes, sum(ticketsspent) totaltix from " +
		BASE_TRANSACTION_TABLE_NAME + ", " + PRIZE_TRANSACTION_TABLE_NAME +
		" WHERE " + BASE_TRANSACTION_TABLE_NAME + ".transactionid = " + PRIZE_TRANSACTION_TABLE_NAME + ".transactionid " +
		"AND memberid = " + memberID + " " +
		" AND transactiondate >= add_months(trunc(sysdate, 'month'), -1) AND transactiondate < trunc(sysdate, 'month')"; 
		//System.out.println(prizesQuery);
		answer = stmt.executeQuery(prizesQuery);
		
		if (answer.next()) {
			System.out.println("\nYou redeemed " + answer.getInt("numprizes")
			+ " prize(s) last month, spending a total of "
			+ answer.getInt("totaltix") + " tickets.");
		} else {
			System.out.println("You did not redeem any tickets last month.");
		}

		// Coupon redemption stats for the last month.
		String couponQuery = "SELECT count(detailid) numredeems from " +
		BASE_TRANSACTION_TABLE_NAME + ", " + COUPON_TRANSACTION_TABLE_NAME +
		" WHERE " + BASE_TRANSACTION_TABLE_NAME + ".transactionid = " + COUPON_TRANSACTION_TABLE_NAME + ".transactionid " +
		"AND memberid = " + memberID + " " +
		" AND transactiondate >= add_months(trunc(sysdate, 'month'), -1) AND transactiondate < trunc(sysdate, 'month')"; 
		//System.out.println(couponQuery);
		answer = stmt.executeQuery(couponQuery);
		
		if (answer.next()) {
			System.out.println("\nYou redeemed " + answer.getInt("numredeems")
			+ " coupon(s) last month.");
		} else {
			System.out.println("You did not redeem any coupons last month.");
		}

		answer.close();
		stmt.close();
	}


	public static void processQuery(String[] command, Connection dbConn, Scanner scanner) throws SQLException {
		if (command[0].equals("ADD")) {
			if (command[1].equalsIgnoreCase("PRIZE")) {
                String[] prizeInfo = gatherPrizeInfo(dbConn, scanner);
                addPrize(dbConn, prizeInfo);
			} else if (command[1].equalsIgnoreCase("GAME")) {
				String[] userInput = gatherGameInfo(dbConn, scanner);
				addGame(userInput, dbConn);
			} else if (command[1].equalsIgnoreCase("MEMBER")) {
				String[] userInput = gatherMemberInfo(dbConn, scanner);
				addMember(userInput, dbConn);
			} else {
                System.out.println("Invalid command. You can add a prize, game or member with the command:");
                System.out.println("ADD <PRIZE | GAME | MEMBER>");
            }
		} else if (command[0].equals("DELETE")) {
			if (command[1].equalsIgnoreCase("PRIZE")) {
				delPrize(dbConn, scanner);
			} else if (command[1].equalsIgnoreCase("GAME")) {
				delGame(dbConn, scanner);
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
                System.out.println("SEARCH <PRIZE>");
            }
        } else if (command[0].equals("UPDATE")) {
            if (command[1].equalsIgnoreCase("PRIZE")) {
                updatePrize(dbConn, scanner);
            } else if (command[1].equalsIgnoreCase("GAME")) {
                updateGame(dbConn, scanner);
            } else if (command[1].equalsIgnoreCase("MEMBER")) {
                updateMember(dbConn, scanner);
            } else {
                System.out.println("Invalid command. You can update a prize, member or game with the command:");
                System.out.println("UPDATE <PRIZE | GAME | MEMBER>");
            }
        } else if (command[0].equals("PLAY")) {
			addGameStat(dbConn, scanner);
		} else if (command[0].equals("REDEEM")) {
			redeemPrize(dbConn, scanner);
		} else if (command[0].equals("QUERY")) {
			if (command[1].equalsIgnoreCase("ONE")) {
				Map<String, Integer> games = getArcadeGames(dbConn);
				queryOne(games, dbConn);
			} else if (command[1].equalsIgnoreCase("TWO")) {
				queryTwo(dbConn);
			} else if (command[1].equalsIgnoreCase("THREE")) {
				queryThree(dbConn, command);
			} else if (command[1].equalsIgnoreCase("FOUR")) {
				queryFour(dbConn, command, scanner);
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
