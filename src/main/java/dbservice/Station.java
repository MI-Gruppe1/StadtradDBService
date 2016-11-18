package dbservice;

/**
* Created by FBeck on 08.11.2016.
*/
public class Station {
	
   private String name;
   private Double latitude;
   private Double longitude;
   
   public Station(String name, Double latitude, Double longitude) {
	   this.name = name;
	   this.latitude = latitude;
	   this.longitude = longitude;
   }

   @Override
   public boolean equals(Object o) {
       if (this == o) return true;
       if (o == null || getClass() != o.getClass()) return false;

       Station station = (Station) o;

       if (!getName().equals(station.getName())) return false;
       if (!getLatitude().equals(station.getLatitude())) return false;
       return getLongitude().equals(station.getLongitude());

   }

   @Override
   public int hashCode() {
       int result = getName().hashCode();
       result = 31 * result + getLatitude().hashCode();
       result = 31 * result + getLongitude().hashCode();
       return result;
   }

   public String getName() {
       return name;
   }

   public Double getLatitude() {
       return latitude;
   }

   public Double getLongitude() {
       return longitude;
   }
}