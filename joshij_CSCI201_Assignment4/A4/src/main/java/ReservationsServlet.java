import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@WebServlet("/ReservationsServlet")
public class ReservationsServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String ID_USER = "idUser";

	public ReservationsServlet() {
		super();
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Integer idUser = 0;

		// Create or retrieve the user session
		HttpSession session = request.getSession(true);
		session.setMaxInactiveInterval(30 * 60); // Set session timeout to 30 minutes
		idUser = (Integer) session.getAttribute(ID_USER);

		String sql = "";

		// Add a parameter for sorting in the servlet
		String sortBy = request.getParameter("sortBy");
		// Default sort order
		if (sortBy == null) {
			sortBy = "nameAZ";
		}

		// Open the database
		Connection conn = connectToDatabase();

		sql = "SELECT f.iduser as f_iduser, f.idrestaurantyelp as f_idrestaurantyelp, name, address, url, urlimage, rawyelpdata, date, time "
				+ " " + "FROM reservations f " + " " + " JOIN restaurants r ON f.idrestaurantyelp = r.idrestaurantyelp "
				+ " " + "WHERE f.iduser = ? " + " " + " ORDER BY " + getSortByClause(sortBy) + "; ";

		try (PreparedStatement statement = conn.prepareStatement(sql)) {
			statement.setInt(1, idUser);

			// Use executeQuery for SELECT statements
			try (ResultSet resultSet = statement.executeQuery()) {
				JSONArray reservationsArray = new JSONArray();
				while (resultSet.next()) {
					// Create a JSON object for each reservation restaurant
					JSONObject reservation = new JSONObject();
					reservation.put("iduser", (Integer) resultSet.getInt("f_iduser"));
					reservation.put("idrestaurantyelp", resultSet.getString("f_idrestaurantyelp"));
					reservation.put("name", resultSet.getString("name"));
					reservation.put("address", resultSet.getString("address"));
					reservation.put("url", resultSet.getString("url"));
					reservation.put("urlimage", resultSet.getString("urlimage"));
					reservation.put("business", resultSet.getString("rawyelpdata"));
					reservation.put("date", resultSet.getString("date"));
					reservation.put("time", resultSet.getString("time"));

					// Add the reservation restaurant to the array
					reservationsArray.add(reservation);
				}
				// Convert the array to a JSON string
				String reservationsJson = reservationsArray.toJSONString();

				// Set the response type to JSON
				response.setContentType("application/json;charset=UTF-8");

				// Write the JSON string to the response
				response.getWriter().write(reservationsJson);
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String idrestaurantyelp = request.getParameter("idrestaurantyelp2reservation");
		Integer idUser = 0;
		Boolean reservationSuccess = false;

		// Create or retrieve the user session
		HttpSession session = request.getSession(true);
		idUser = (Integer) session.getAttribute(ID_USER);

		// read form fields
		String reservationDate = request.getParameter("reservationDate");
		String reservationTime = request.getParameter("reservationTime");
		String reservationNotes = request.getParameter("reservationNotes");

		if (idUser != null) {
			// Extract business JSON from request
			String businessJson = request.getParameter("business2reservation");

			if (businessJson != null) {
				try {
					JSONParser parser = new JSONParser();
					try {
						JSONObject business = (JSONObject) parser.parse(businessJson);
						addReservation2Database(idUser.intValue(), idrestaurantyelp, business, reservationDate,
								reservationTime, reservationNotes);
					} catch (ParseException e) {
						e.printStackTrace(); // Handle the parse exception appropriately
					}

				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("businessJson is null");
			}
		} else {
			System.out.println("idUser not found in the session");
		}
		
		response.sendRedirect("reservations.html");

	}

	protected Boolean addReservation2Database(int idUser, String idrestaurantyelp, JSONObject business,
			String reservationDate, String reservationTime, String reservationNotes) throws JSONException {
		Boolean reservationAdded = false;
		String sql = "";

		// Open the database
		Connection conn = connectToDatabase();
		if (reservationDate == null || reservationDate == "") {
			return false;
		}
		
		sql = "INSERT IGNORE INTO `reservation`.`reservations` (`iduser`, `idrestaurantyelp`, `date`, `time`, `notes`) VALUES (?, ?, ?, ?, ?)";
		try (PreparedStatement statement = conn.prepareStatement(sql)) {
			statement.setInt(1, idUser);
			statement.setString(2, idrestaurantyelp);
			statement.setString(3, reservationDate);
			statement.setString(4, reservationTime);
			statement.setString(5, reservationNotes);

			int rowsInserted = statement.executeUpdate();

			if (rowsInserted > 0) {
				reservationAdded = true;
				System.out.println("Restaurant inserted");
			} else {
				reservationAdded = false;
				System.out.println("Restaurant not inserted. Perhaps duplicate?");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		sql = "INSERT IGNORE INTO `reservation`.`restaurants` (`idrestaurantyelp`, `name`, `address`, `phone`, `price`, `rating`, `url`, `urlimage`, `rawyelpdata`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
		try (PreparedStatement statement = conn.prepareStatement(sql)) {
			statement.setString(1, idrestaurantyelp);
			statement.setString(2, (String) business.get("name"));
			statement.setString(4, (String) business.get("phone"));
			statement.setString(5, (String) business.get("price"));
			statement.setString(6, String.valueOf(business.get("rating")));
			statement.setString(7, (String) business.get("url"));
			statement.setString(8, (String) business.get("image_url"));

			String jsonString = JSONValue.toJSONString(business);
			statement.setString(9, jsonString);

			JSONObject location = (JSONObject) business.get("location");
			if (location != null) {
				Object displayAddressObject = location.get("display_address");

				if (displayAddressObject != null && displayAddressObject instanceof JSONArray) {
					JSONArray displayAddress = (JSONArray) displayAddressObject;

					if (!displayAddress.isEmpty()) {
						statement.setString(3, String.join(", ", displayAddress));
					} else {
						System.out.println("Display address is empty");
					}
				} else {
					System.out.println("Unexpected or null value for display_address: " + displayAddressObject);
				}
			} else {
				System.out.println("Location information is missing");
			}

			int rowsInserted = statement.executeUpdate();

			// TODO need below?
			if (rowsInserted > 0) {
				reservationAdded = true;
				System.out.println("Restaurant inserted");
			} else {
				reservationAdded = false;
				System.out.println("Restaurant not inserted. Perhaps duplicate?");
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}

		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return reservationAdded;
	}

	public Connection connectToDatabase() {
		Connection conn = null;
		try {
			// Explicitly load the JDBC driver
			Class.forName("com.mysql.cj.jdbc.Driver");

			// Establish the database connection
			String DB_URL = "jdbc:mysql://localhost:3306/reservation";
			String DB_USER = "root";
			String DB_PASSWORD = "root";

			conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
			if (conn != null) {
				System.out.println("Connected to the database.");
			}
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
		return conn;
	}

	private String getSortByClause(String sortBy) {
		switch (sortBy) {
		case "recent":
			return "date ASC, time ASC";
		case "leastRecent":
			return "date DESC, time DESC";
		default:
			return "date DESC, time DESC"; // Default sorting
		}
	}

}
