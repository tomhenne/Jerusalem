package de.esymetric.jerusalem.ownDataRepresentation.geoData;
public class Position {
        public double latitude;
        public double longitude;
        public double altitude;
        public int elapsedTimeSec;
        public byte nrOfTransitions;

        public boolean Equals(Object other)
        {
            if (!(other instanceof Position)) return false;
            return ((Position)other).latitude == latitude && ((Position)other).longitude == longitude;
        }

        public int GetHashCode()
        {
            return super.hashCode();
        }

        public boolean IsNull()
        {
            return latitude == 0.0 && longitude == 0.0;
        }

        public double distanceTo(Position p)
        {
            return GPSMath.CalculateDistance(p.latitude, p.longitude, latitude, longitude);
        }
    }
