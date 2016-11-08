package dbservice;

import static spark.Spark.*;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mysql.jdbc.PreparedStatement;

public class Main {
	
	private static Connection connection;
	
	private static Connection connectToDB() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			connection = (Connection) DriverManager.getConnection(
				    "jdbc:mysql://mysqldb:3306/mi",
				    "mi",
				    "miws16"
				);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			MailNotification.sendMail(e);
		}
		
		return connection;
	}
	
	public static void main(String[] args) {
		
		connectToDB();
		
		// Instanz des Jsonparsers erstellen fuer alle Routen nutzbar
		Gson gson = new Gson();
		
		get("/allStations", (request, response) -> {
			return "NOT IMPLEMENTED YET";
		});
		
		get("/freeBikesOfStation", (request, response) -> {
			return "NOT IMPLEMENTED YET";
		});
		
		get("/freeBikesofStationAtSpecTime", (request, response) -> {
			return "NOT IMPLEMENTED YET";
		});
		
		get("/nextXStationsofLatLong", (request, response) -> {
			return "NOT IMPLEMENTED YET";
		});
		
		post("/newData", (request, response) -> {
			// Speichern des Jsons aus dem Requestbody
			String json = request.body();
			
			// Format fuer das umwandeln jsons in ein Javaobjekt festelegen
			Type type = new TypeToken<List<Map<String, String>>>(){}.getType();
			
			// Aus dem Json ein Javaobjekt erstellen
			List<Map<String, String>> itemsList = gson.fromJson(json, type);
			
			System.out.println("List Size: " + itemsList.size());
			System.out.println("First Item: " + itemsList.get(0));
			
			String query = "insert into crawledData (station_id, station_name, free_bikes, information_timestamp, latitude, longitude) " +
					       "values (?, ?, ?, ?, ?, ?)";
			
			for(Map<String, String> pair : itemsList) {
				PreparedStatement preparedStmt = (PreparedStatement) connection.prepareStatement(query);
			    preparedStmt.setString(1, pair.get("id"));
			    preparedStmt.setString(2, pair.get("name"));
			    preparedStmt.setString(3, pair.get("free_bikes"));
			    preparedStmt.setString(4, pair.get("timestamp"));
			    preparedStmt.setString(5, pair.get("latitude"));
			    preparedStmt.setString(6, pair.get("longitude")); 
			}
			
		    return "";
		});
	}
}