package pt.ist.standards.geographic;

import java.util.stream.Stream;

import pt.ist.standards.geographic.Country;
import pt.ist.standards.geographic.Place;
import pt.ist.standards.geographic.Planet;

public class TreasuryGeographicInfoLoader {

    public static final String PRT = "PRT";
    private static TreasuryGeographicInfoLoader geographicInfoLoader;
    private final Planet earth;

    private TreasuryGeographicInfoLoader() {
        earth = Planet.getEarth();
    }

    public Stream<Country> findAllCountries() {
        return earth.getPlaces().stream();
    }

    synchronized public static TreasuryGeographicInfoLoader getInstance() {
        if (geographicInfoLoader == null) {
            geographicInfoLoader = new TreasuryGeographicInfoLoader();
        }
        return geographicInfoLoader;
    }

    public static boolean isDefaultCountry(Country country) {
        return country.alpha3.equals(PRT);
    }

    public Place importPlaceFromString(final String placeString) {
        if (placeString == null || placeString.length() == 0) {
            return null;
        }
        return earth.importFrom(placeString);
    }

    public static String externalizePlace(Place place) {
        return place.exportAsString();
    }

    public static <T extends Place> T internalizePlace(String placeString) {
    	TreasuryGeographicInfoLoader geographicInfoLoader = TreasuryGeographicInfoLoader.getInstance();
        return (T) geographicInfoLoader.importPlaceFromString(placeString);
    }
}
