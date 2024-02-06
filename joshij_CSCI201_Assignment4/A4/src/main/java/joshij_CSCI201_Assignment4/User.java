package joshij_CSCI201_Assignment4;

public class User {
	private String username_;
	private String password_;
	private String email_;

	public String getUsername_() {
		return username_;
	}

	public void setUsername_(String username_) {
		this.username_ = username_;
	}

	public String getPassword_() {
		return password_;
	}

	public void setPassword_(String password_) {
		this.password_ = password_;
	}

	public String getEmail_() {
		return email_;
	}

	public void setEmail_(String email_) {
		this.email_ = email_;
	}

	
	private User(String username, String password, String email) {
		username_ = username;
		password_ = password;
		email_ = email;
	}
	
}
