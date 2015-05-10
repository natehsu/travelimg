package at.ac.tuwien.qse.sepm.gui;

import at.ac.tuwien.qse.sepm.entities.Exif;
import at.ac.tuwien.qse.sepm.service.impl.ExifServiceImpl;
import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.object.*;
import javafx.application.Application;
import javafx.scene.Scene;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.DoubleSummaryStatistics;

/**
 * Created by christoph on 08.05.15.
 */
public class GoogleMapsScene implements MapComponentInitializedListener {

    private static final Logger logger = LogManager.getLogger(ExifServiceImpl.class);

    private GoogleMapView mapView;
    private GoogleMap map;
    static final LatLong defaulLocation = new LatLong(40.7033127, -73.979681); // the default Location
    private static LatLong destination; // the Location from the Exif Objekt
    private Exif marker;

    /**
     * Default Constructor
     *
     *
     */
    public GoogleMapsScene(){
        this.mapView = new GoogleMapView();
        this.mapView.addMapInializedListener(this);
        this.marker =null;
    }

    /**
     * Constructor of a new GoogleMapsScene-Objekt
     *
     * @param marker a Exif-Objekt to be displayed as a marker
     */
    public GoogleMapsScene(Exif marker){
        this.mapView = new GoogleMapView();
        destination = calculate(marker.getLongitude(), marker.getLatitude());
        this.mapView.addMapInializedListener(this);
        this.marker =marker;
    }

    /**
     * calculate the decimal coordinates
     * @param longitude the longitude-GPS coordinate from the Exif-file (grad, minutes, sec)
     * @param latitude  the latitude-GPS coordinate from the Exif-file (grad, minutes, sec)
     * @return a LatLong-Object with the decimal coordinates
     */
    private LatLong calculate(String longitude, String latitude){
        String[] longi = longitude.split(" ");
        String[] lat = latitude.split(" ");

        double grad1 = Double.parseDouble(longi[0]);
        double grad2 = Double.parseDouble(lat[0]);
        double min1 = Double.parseDouble(longi[1]);
       double min2 = Double.parseDouble(lat[1]);
        double sec1 = Double.parseDouble(longi[2]);
        double sec2 = Double.parseDouble(lat[2]);
        double erg1 = (((sec1/60)+min1)/60)+grad1;
        double erg2 = (((sec2/60)+min2)/60)+grad2;
       return new LatLong(erg1,erg2);
    }

    /**
     * Initialising GoogleMap
     * default --> show the global view (x and y = Double_MinValue)
     * Consturctor (exif) --> a marker will be placed at the exif-Koordinate
     * Constructor (x,y,exif)--> focus Position x,y ;  a marker will be placed at the exif-Koordinate
     *
     *
     */
    @Override
    public void mapInitialized() {
        //Set the initial properties of the map.
        logger.debug("Initializing Map ");
        MapOptions mapOptions;

        if(destination==null) {
           mapOptions =returnOption(defaulLocation, true, true, true, 2);
        }else{
            mapOptions =returnOption(destination,true,true,true,12);
        }

        map = mapView.createMap(mapOptions);

        if(this.marker!=null){
            map.addMarker(new Marker(new MarkerOptions().position(destination).visible(Boolean.TRUE)));
        }
    }

    /**
     * returns a MapOption Objekt
     * @param destinat the destination on the Map
     * @param overview true --> switch between Map and Satelit
     * @param panControl true --> show Navigation-element from GoogleMaps
     * @param zoomControl true--> show zoom-element from GoogleMaps
     * @param zoomfactor represent the zoomfactor in the map
     * @return a MapOption-Objekt
     *
     */
    private MapOptions returnOption(LatLong destinat,boolean overview,boolean panControl, boolean zoomControl, int zoomfactor){
        MapOptions mapOptions = new MapOptions();

        mapOptions.center(destinat)
                .overviewMapControl(overview)
                .panControl(panControl)
                .rotateControl(false)
                .scaleControl(false)
                .streetViewControl(false)
                .zoomControl(zoomControl)
                .zoom(zoomfactor);
        return mapOptions;
    }
    /**
     *
     * @return GoogleMapView as a Scene
     */
    public Scene getScene(){
        logger.debug("returning Scene");
        return new Scene(this.mapView);

    }


}
