/**
 * @author Flah-Uddin Ahmad
 * @author Andreas Loeffler
 * @version 1.0
 */

package dbservice;

import static spark.Spark.get;
import static spark.Spark.post;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
	private static final double R = 6372.8; //Erdradius in km
	private static PreparedStatement preparedStmt;
	private static String json;
	private static Type type;
	private static List<Map<String, String>> itemsList;

	private static Connection connectToDB() {
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			connection = (Connection) DriverManager.getConnection("jdbc:mysql://mysqldb/mi", "mi", "miws16");
			// Fuer lokalen Test
			//connection = (Connection) DriverManager.getConnection("jdbc:mysql://localhost/mi", "mi", "miws16");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			MailNotification.sendMail(e);
		}

		return connection;
	}

	public static void main(String[] args) throws SQLException {

		// Port fuer diesen Service setzten
		spark.Spark.port(6000);
		
		connectToDB();

		// Instanz des Jsonparsers erstellen fuer alle Routen nutzbar
		Gson gson = new Gson();

		get("/allStations", (request, response) -> {
			return gson.toJson(getAllStations());
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
			Integer number_of_stations = Integer.parseInt(request.queryParams("number_of_stations"));
			Double latitude = Double.parseDouble(request.queryParams("latitude"));
			Double longitude = Double.parseDouble(request.queryParams("longitude"));
			
			List<Station> allStations = getAllStations();
			
			List<Station> ordered = getNearestStationsOfPoint(latitude, longitude, allStations, number_of_stations);
			
			return gson.toJson(ordered);
		});

		post("/newData", (request, response) -> {
			// Speichern des Jsons aus dem Requestbody
			json = request.body();

			// Format fuer das umwandeln jsons in ein Javaobjekt festelegen
			type = new TypeToken<List<Map<String, String>>>() {
			}.getType();

			// Aus dem Json ein Javaobjekt erstellen
			itemsList = gson.fromJson(json, type);

			String query = "insert into crawledData (station_id, station_name, free_bikes, information_timestamp, latitude, longitude) "
					+ "values (?, ?, ?, ?, ?, ?)";

			for (Map<String, String> pair : itemsList) {
				preparedStmt = (PreparedStatement) connection.prepareStatement(query);
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
	
	public static List<Station> getAllStations() throws SQLException {
		String query = "SELECT DISTINCT `crawledData`.station_name,`crawledData`.latitude,`crawledData`.longitude FROM `crawledData`;";

		java.sql.Statement st = connection.createStatement();
		ResultSet rs = st.executeQuery(query);

		ArrayList<Station> objList = new ArrayList<Station>();

		while (rs.next()) {
			objList.add(new Station(rs.getString("station_name"), rs.getDouble("latitude"),
					rs.getDouble("longitude")));
		}

		st.close();
		
		return objList;
	}
	
	/*
	 * Berechnet fuer einen Punkt (latitude,longitude) die naechstliegenden Stationen
	 */
	public static List<Station> getNearestStationsOfPoint(double latPoint, double lngPoint, List<Station> allStations, int numberOfStations) {
		
		// Pruefen ob die Liste der Stationen leer ist
		if(allStations == null) {
			throw new IllegalArgumentException("Die Liste der Stationen darf nicht leer sein");
		}
		
		// Pruefen ob die Anzahl der gewunschten Stationen kleiner als die Gesamtanzahl der Stationen ist
		if(numberOfStations > allStations.size()) {
			throw new IllegalArgumentException("Die Anzahl der gewunschten Stationen ist groesser als die Anzahlder zur verfuegung stehenden Stationen");
		}
		
		// Das Ergebnis enthaelt die Stationen am naehsten vom Ausgangspunkt
		List<Station> result = new ArrayList<>();
		
		// Speichert fuer jede Station die Entferung vom Ausgangspunkt
		List<List<Object>> distances = new ArrayList<>();
		
		// Fuer alle Stationen die Entfernung zum Ausgangspunkt berechnen und zwischen speichern
		for(Station s : allStations) {
			double distance = haversine(s.getLatitude(), s.getLongitude(), latPoint, lngPoint);
			distances.add(new ArrayList<>(Arrays.asList(s, distance)));
		}
		
		// Ergebnis nach Entfernung sortieren
		Collections.sort(distances, new Comparator<List<Object>>() {    
	        @Override
	        public int compare(List<Object> o1, List<Object> o2) {
	            return ((Double) o1.get(1)).compareTo((Double) o2.get(1));
	        }               
		});
		
		// Nur die Stationen extrahieren und zurueck geben
		for (int i = 0; i < numberOfStations; i++) {
			result.add((Station) distances.get(i).get(0));
		}
		
		return result;
	}
	
	/*
	 * Berechnet die Entfernung zwischen zwei Punkten mit Lat/Long
	 */
	private static double haversine(double latStation, double lngStation, double latWaypoint, double lngWaypoint) {
		double dLat = Math.toRadians(latWaypoint - latStation);
		double dLon = Math.toRadians(lngWaypoint - lngStation);
		latStation = Math.toRadians(latStation);
		latWaypoint = Math.toRadians(latWaypoint);

		double a = Math.pow(Math.sin(dLat / 2), 2)
				+ Math.pow(Math.sin(dLon / 2), 2) * Math.cos(latStation) * Math.cos(latWaypoint);
		double c = 2 * Math.asin(Math.sqrt(a));
		return R * c;
	}
}
