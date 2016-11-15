/**
 * @author Flah-Uddin Ahmad
 * @author Andreas Loeffler
 * @version 1.0
 */

package dbservice;

import static spark.Spark.*;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mysql.jdbc.PreparedStatement;


/**
 * StadtRadDBService manages get and post requests.
 * Functionality of this Class is to store/get data to/from DB
 */
public class StadtRadDBservice {

	private static Connection connection;

	private static Connection connectToDB() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			connection = (Connection) DriverManager.getConnection("jdbc:mysql://localhost/mi", "mi", "miws16");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			MailNotification.sendMail(e);
		}

		return connection;
	}

	private static class DataObject {

		private String name;
		private double latitude;
		private double longitude;

		public DataObject(String string, double int1, double int2) {
			name = string;
			latitude = int1;
			longitude = int2;
		}

	}

	public static void main(String[] args) {

		connectToDB();

		// Instanz des Jsonparsers erstellen fuer alle Routen nutzbar
		Gson gson = new Gson();

		get("/allStations", (request, response) -> {

			String query = "SELECT DISTINCT `crawledData`.station_name,`crawledData`.latitude,`crawledData`.longitude FROM `crawledData`;";

			java.sql.Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery(query);

			ArrayList<DataObject> objList = new ArrayList<DataObject>();

			while (rs.next()) {
				objList.add(new DataObject(rs.getString("station_name"), rs.getDouble("latitude"),
						rs.getDouble("longitude")));
			}

			st.close();

			return new Gson().toJson(objList);
		});

		get("/freeBikesOfStation", (request, response) -> {

			String name = request.queryParams("station_name");
			String query = "SELECT `crawledData`.free_bikes FROM `crawledData` WHERE `crawledData`.station_name = \""
					+ name + "\" ORDER BY `crawledData`.information_timestamp DESC LIMIT 1;";

			java.sql.Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery(query);

			int free_bikes = -1;
			while (rs.next()) {

				free_bikes = rs.getInt("free_bikes");

			}

			st.close();
			return free_bikes;
		});

		get("/freeBikesofStationAtSpecTime", (request, response) -> {

			String name = request.queryParams("station_name");
			String timestamp = request.queryParams("information_timestamp");
			String timestampmin = new Long(Long.parseLong(timestamp) - 150000).toString();
			String timestampmax = new Long(Long.parseLong(timestamp) + 150000).toString();

			String query = "SELECT `crawledData`.free_bikes FROM `crawledData` WHERE `crawledData`.station_name = \""
					+ name + "\" AND `crawledData`.information_timestamp BETWEEN " + timestampmin + " AND " + timestampmax;
			
			java.sql.Statement st = connection.createStatement();
			ResultSet rs = st.executeQuery(query);
			
			int free_bikes = -1;
			while (rs.next()) {

				free_bikes = rs.getInt("free_bikes");

			}

			st.close();
			return free_bikes;
		});

		get("/nextXStationsofLatLong", (request, response) -> {
			return "NOT IMPLEMENTED YET";
		});

		post("/newData", (request, response) -> {
			// Speichern des Jsons aus dem Requestbody
			String json = request.body();

			// Format fuer das umwandeln jsons in ein Javaobjekt festelegen
			Type type = new TypeToken<List<Map<String, String>>>() {
			}.getType();

			// Aus dem Json ein Javaobjekt erstellen
			List<Map<String, String>> itemsList = gson.fromJson(json, type);

			System.out.println("List Size: " + itemsList.size());
			System.out.println("First Item: " + itemsList.get(0));

			String query = "insert into crawledData (station_id, station_name, free_bikes, information_timestamp, latitude, longitude) "
					+ "values (?, ?, ?, ?, ?, ?)";

			for (Map<String, String> pair : itemsList) {
				PreparedStatement preparedStmt = (PreparedStatement) connection.prepareStatement(query);
				preparedStmt.setString(1, pair.get("id"));
				preparedStmt.setString(2, pair.get("name"));
				preparedStmt.setString(3, pair.get("free_bikes"));
				preparedStmt.setString(4, pair.get("timestamp"));
				preparedStmt.setString(5, pair.get("latitude"));
				preparedStmt.setString(6, pair.get("longitude"));

				// execute the preparedstatement
				preparedStmt.execute();
			}

			return "";
		});
	}
}