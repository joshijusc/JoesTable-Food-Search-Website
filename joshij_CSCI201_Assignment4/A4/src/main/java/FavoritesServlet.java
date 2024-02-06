import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

//import com.google.gson.Gson;
//import com.google.gson.JsonArray;
//import com.google.gson.JsonObject;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/FavoritesServlet")
public class FavoritesServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String ID_USER = "idUser";
	private static final String BUSINESSESRAW = "businessesraw";
	private static final String BUSINESSES = "businesses";
	private static final String BUSINESSESJSON = "businessesJson";

	private static final Object cachedAllowHeaderValueLock = null;

	public FavoritesServlet() {
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

		if (Boolean.parseBoolean(request.getParameter("checkDoesFavoriteExist"))) {
			Connection conn = connectToDatabase();
			sql = "SELECT count(idfavorite) FROM reservation.favorites WHERE iduser = ? and idrestaurantyelp = ?";

			try (PreparedStatement statement = conn.prepareStatement(sql)) {
				statement.setInt(1, idUser);
				statement.setString(2, request.getParameter("idrestaurantyelp"));

				try (ResultSet resultSet = statement.executeQuery()) {
					while (resultSet.next()) {
						int doesFavoriteExist = resultSet.getInt(1);
						System.out.println("doesFavoriteExist > " + doesFavoriteExist);
						response.getWriter().write(String.valueOf(doesFavoriteExist));
					}
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
		} else {

			// Add a parameter for sorting in the servlet
			String sortBy = request.getParameter("sortBy");
			// Default sort order
			if (sortBy == null) {
				sortBy = "recent";
			}

			Connection conn = connectToDatabase();

			sql = "SELECT f.iduser as f_iduser, f.idrestaurantyelp as f_idrestaurantyelp, name, address, url, urlimage, rawyelpdata "
					+ " " + "FROM favorites f " + " " + "JOIN restaurants r ON f.idrestaurantyelp = r.idrestaurantyelp "
					+ " " + "WHERE f.iduser = ? " + " " + "ORDER BY " + getSortByClause(sortBy) + "; ";

			try (PreparedStatement statement = conn.prepareStatement(sql)) {
				statement.setInt(1, idUser);

				try (ResultSet resultSet = statement.executeQuery()) {
					JSONArray favoritesArray = new JSONArray();
					while (resultSet.next()) {
						// Create a JSON object for each favorite restaurant
						JSONObject favorite = new JSONObject();
						favorite.put("iduser", (Integer) resultSet.getInt("f_iduser"));
						favorite.put("idrestaurantyelp", resultSet.getString("f_idrestaurantyelp"));
						favorite.put("name", resultSet.getString("name"));
						favorite.put("address", resultSet.getString("address"));
						favorite.put("url", resultSet.getString("url"));
						favorite.put("urlimage", resultSet.getString("urlimage"));
						favorite.put("business", resultSet.getString("rawyelpdata"));

						// Add the favorite restaurant to the array
						favoritesArray.add(favorite);
					}
					// Convert the array to a JSON string
					String favoritesJson = favoritesArray.toJSONString();

					// Set the response type to JSON
					response.setContentType("application/json;charset=UTF-8");

					// Write the JSON string to the response
					response.getWriter().write(favoritesJson);
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

	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Read form fields
		String addOrRemoveFavoritesButton = request.getParameter("addOrRemoveFavoritesButton");
		String idrestaurantyelp = request.getParameter("idrestaurantyelp");
		String idrestaurantyelp2removeFavorite = request.getParameter("idrestaurantyelp2removeFavorite");
		
		Integer idUser = 0;
		// Create or retrieve the user session
		HttpSession session = request.getSession(true);
		idUser = (Integer) session.getAttribute(ID_USER);
		if (idUser == null) {
			return;
		}
		System.out.println("149 Idrestaurantyelp: " + idrestaurantyelp);
		if ("Add to Favorites".equals(addOrRemoveFavoritesButton)) {
			String businessJson = request.getParameter("business");
			if (businessJson != null) {
				try {
					JSONParser parser = new JSONParser();
					try {
						JSONObject business = (JSONObject) parser.parse(businessJson);
						addFavorite2Database(idUser.intValue(), idrestaurantyelp, business);
					} catch (ParseException e) {
						e.printStackTrace(); // Handle the parse exception appropriately
					}

				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("businessJson is null");
			}
		} else if ("Remove from Favorites".equals(addOrRemoveFavoritesButton)) {
			try {
				removeFavoriteFromDatabase(idUser.intValue(), idrestaurantyelp2removeFavorite);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		response.sendRedirect("favorites.html");
	}

	protected Boolean addFavorite2Database(int idUser, String idrestaurantyelp, JSONObject business)
			throws JSONException {
		Boolean favoriteAdded = false;
		String sql = "";

		// Open the database
		Connection conn = connectToDatabase();

		// Check if user name exists
		sql = "INSERT IGNORE INTO `reservation`.`favorites` (`iduser`, `idrestaurantyelp`) VALUES (?, ?)";
		// If all checks pass, proceed with the insertion
		try (PreparedStatement statement = conn.prepareStatement(sql)) {
			statement.setInt(1, idUser);
			statement.setString(2, idrestaurantyelp);
			int rowsInserted = statement.executeUpdate();

			if (rowsInserted > 0) {
				favoriteAdded = true;
			} else {
				favoriteAdded = false;
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
				favoriteAdded = true;
				System.out.println("Restaurant inserted");
			} else {
				favoriteAdded = false;
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

		return favoriteAdded;
	}

	protected Boolean removeFavoriteFromDatabase(int idUser, String idrestaurantyelp)
			throws JSONException {
		Boolean favoriteRemoved = false;
		String sql = "";

		// Open the database
		Connection conn = connectToDatabase();

		sql = "DELETE FROM reservation.favorites WHERE iduser = ? AND idrestaurantyelp = ?";
		System.out.println("Idrestaurantyelp: " + idrestaurantyelp);
		
		try (PreparedStatement statement = conn.prepareStatement(sql)) {
			statement.setInt(1, idUser);
			statement.setString(2, idrestaurantyelp);

			int rowsDeleted = statement.executeUpdate();

			if (rowsDeleted > 0) {
				favoriteRemoved = true;
				System.out.println("Favorite removed");
			} else {
				favoriteRemoved = false;
				System.out.println("Favorite not removed.");
			}

		} catch (SQLException e) {
			e.printStackTrace();
			System.out.println("Error during database operation: " + e.getMessage());
		}


		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		return favoriteRemoved;
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
			}
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
		return conn;
	}

	private String getSortByClause(String sortBy) {
		switch (sortBy) {
		case "nameAZ":
			return "r.name ASC"; // Use alias "r" for the restaurants table
		case "nameZA":
			return "r.name DESC";
		case "ratingHigh":
			return "r.rating DESC"; // Use alias "r" for the restaurants table
		case "ratingLow":
			return "r.rating ASC";
		case "recent":
			return "f.idfavorite DESC";
		case "leastRecent":
			return "f.idfavorite ASC";
		default:
			return "f.idfavorite DESC";
		}
	}

	void tests() {
		/*
		 * 
		 * authenticatedUser and nonAuthenticatedUser
		 * 
		 * Fav Restaurant Reservation insert insert TODO duplicate duplicate TODO
		 * 
		 * 
		 */
	}

}
