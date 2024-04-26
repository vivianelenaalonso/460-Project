import java.util.Scanner;
import java.sql.*;

public class Arcade {
    private static final String PRIZE_TABLE_NAME = "prize_table_name";
    private static final String ORACLE_URL = 
                        "jdbc:oracle:thin:@aloe.cs.arizona.edu:1521:oracle";



    public static void addPrize(Connection dbConn, String prizeId, String prizeName, 
                                    String ticketAmt) throws SQLException {
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



    public static void delPrize(Connection dbConn, String prizeId) throws SQLException {
        String query = "DELETE FROM " + PRIZE_TABLE_NAME + """
        WHERE prizeID = """ + 
            "'" + prizeId.replaceAll("'", "''") + "'," +
        " )";

        Statement stmt = dbConn.createStatement();
        stmt.executeUpdate(query);
        stmt.close();
    }

    public static void main (String[] args) {
        
    }
}
