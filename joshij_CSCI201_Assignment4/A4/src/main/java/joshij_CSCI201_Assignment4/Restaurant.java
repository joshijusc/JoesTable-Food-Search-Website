package joshij_CSCI201_Assignment4;

import java.util.List;


public class Restaurant {
//    @SerializedName("name")
    private String name_;
//    @SerializedName("address")
    private String address_;
//    @SerializedName("latitude")    
    private double latitude_;
//    @SerializedName("longitude")
    private double longitude_;
//    @SerializedName("drivers")
    private int drivers_;
//    @SerializedName("menu")
    private List<String> menu_;

    
    public Restaurant(String restaurantName, String restaurantAddress, double latitude, double longitude, int drivers, List<String> menuItems){
    	name_ = restaurantName;
    	address_ = restaurantAddress;
    	latitude_ = latitude;
    	longitude_ = longitude;
    	drivers_ = drivers;
    	menu_ = menuItems;
    }
    
    public String getName() {
    	return name_;
    }
    
    public String getAddress() {
    	return address_;
    }
    
    public double getLatitude() {
    	return latitude_;
    }
    
    public double getLongitude() {
    	return longitude_;
    }

    public int getNumDrivers() {
    	return drivers_;
    }
    
    
//    public void setNumDrivers(int drivers) { // Don't want to modify the number of drivers, instead use another counter of available drivers if needed
//    	drivers_ = drivers;
//    }

    public List<String> getMenu() {
    	return menu_;
    }
    

    
    @Override
    public String toString() {
        return name_ + ", located at " + address_;
    }
}