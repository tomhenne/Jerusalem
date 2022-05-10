package de.esymetric.jerusalem.ownDataRepresentation.geoData;


public class GPSMath {
    public final static double EARTH_RADIUS = 6371007.0;

    public final static double MAX_LAT = 90.0;
    public final static double MIN_LAT = -90.0;
    public final static double MAX_LON = 180.0;
    public final static double MIN_LON = -180.0;

    /****************************************************************
    Distances
    *****************************************************************/


    //Haversine
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2)
    {
        lat1 = Deg2Rad(lat1);
        lon1 = Deg2Rad(lon1);
        lat2 = Deg2Rad(lat2);
        lon2 = Deg2Rad(lon2);

        double dLat = lat2 - lat1;
        double dLong = lon2 - lon1;
        if (dLat == 0 && dLong == 0) return 0;

        double slat = Math.sin(dLat / 2);
        double slong = Math.sin(dLong / 2);
        double a = slat * slat + Math.cos(lat1) * Math.cos(lat2) * slong * slong;
        //double c = 2 * Math.Atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double c = 2.0 * Math.asin(Math.min(1, Math.sqrt(a)));
        return EARTH_RADIUS * c;
    }

    public static double CalculateDistance2D(double x1, double y1, double x2, double y2)
    {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // compute distance of point Pc to a line segment defined by
    // P1 - P2
    // uses Haversine > about 25 km

    final static double DIST_FROM_LINE_CALC_MAX_NORMAL_DIST = 25000; // m

    public static double CalculateDistanceToLine2DWithHaversineAlternative(
        double x1, double y1, double x2, double y2, double xc, double yc,
        Position pc, Position p1, Position p2)
    {
        double d = CalculateDistanceToLine2D(x1, y1, x2, y2, xc, yc);
        if (d > DIST_FROM_LINE_CALC_MAX_NORMAL_DIST)
        {
            Position pm = new Position();
            pm.latitude = (p1.latitude + p2.latitude) / 2.0;
            pm.latitude = (p1.longitude + p2.longitude) / 2.0;
            d = calculateDistance(pc.latitude, pc.longitude, pm.latitude, pm.longitude);
        }
        return d;
    }

    // same as above but also compute intersection point
    public static double CalculateDistanceToLine2DWithHaversineAlternative(
        double x1, double y1, double x2, double y2, double xc, double yc,
        Position pc, Position p1, Position p2, Coord2 intersection2D)
    {
        Coord2 intersection2DNew = CalculateIntersectionPointWithLine2D(x1, y1, x2, y2, xc, yc);
        intersection2D.x = intersection2DNew.x;
        intersection2D.y = intersection2DNew.y;
        double d = CalculateDistance2D(intersection2D.x, intersection2D.y, xc, yc);
        if (d > DIST_FROM_LINE_CALC_MAX_NORMAL_DIST)
        {
            Position pm = new Position();
            pm.latitude = (p1.latitude + p2.latitude) / 2.0;
            pm.latitude = (p1.longitude + p2.longitude) / 2.0;
            d = calculateDistance(pc.latitude, pc.longitude, pm.latitude, pm.longitude);
        }
        return d;
    }


    // compute distance of point Pc to a line segment defined by
    // P1 - P2
    // uses Phythagoras, not Haversine

    public static double CalculateDistanceToLine2D(double x1, double y1, 
        double x2, double y2, double xc, double yc)
    {
        // always have the line from left to right

        if (x1 > x2)
        {
            double xt = x2;
            double yt = y2;
            x2 = x1;
            y2 = y1;
            x1 = xt;
            y1 = yt;
        }

        double dx = x2 - x1;
        double dy = y2 - y1;

        // special cases

        if (dx == 0.0)
        {
            // have the line from bottom to top
            if (y1 > y2)
            {
                double xt = x2;
                double yt = y2;
                x2 = x1;
                y2 = y1;
                x1 = xt;
                y1 = yt;
            }

            if (yc < y1) return CalculateDistance2D(xc, yc, x1, y1);
            if (yc > y2) return CalculateDistance2D(xc, yc, x2, y2);

            return Math.abs(xc - x1);
        }

        if (dy == 0.0)
        {
            if (xc < x1) return CalculateDistance2D(xc, yc, x1, y1);
            if (xc > x2) return CalculateDistance2D(xc, yc, x2, y2);

            return Math.abs(yc - y1);
        }

        // line equation

        double m = dy / dx;
        double t = y1 - m * x1;

        // normal equation

        double tn = yc + xc / m; // normal

        // intersection

        double xs = (tn - t) / (m + 1 / m); // intersection point
        double ys = m * xs + t;

        // treat special cases

        if (xs < x1) return CalculateDistance2D(xc, yc, x1, y1);
        if (xs > x2) return CalculateDistance2D(xc, yc, x2, y2);

        return CalculateDistance2D(xc, yc, xs, ys);
    }


    public static Coord2 CalculateIntersectionPointWithLine2D(double x1, double y1,
        double x2, double y2, double xc, double yc)
    {
        // always have the line from left to right

        if (x1 > x2)
        {
            double xt = x2;
            double yt = y2;
            x2 = x1;
            y2 = y1;
            x1 = xt;
            y1 = yt;
        }

        double dx = x2 - x1;
        double dy = y2 - y1;

        // special cases

        if (dx == 0.0)
        {
            // have the line from bottom to top
            if (y1 > y2)
            {
                double xt = x2;
                double yt = y2;
                x2 = x1;
                y2 = y1;
                x1 = xt;
                y1 = yt;
            }

            if (yc < y1) return new Coord2(x1, y1);
            if (yc > y2) return new Coord2(x2, y2);

            return new Coord2(x1, yc);
        }

        if (dy == 0.0)
        {
            if (xc < x1) return new Coord2(x1, y1);
            if (xc > x2) return new Coord2(x2, y2);

            return new Coord2(xc, y1);
        }

        // line equation

        double m = dy / dx;
        double t = y1 - m * x1;

        // normal equation

        double tn = yc + xc / m; // normal

        // intersection

        double xs = (tn - t) / (m + 1 / m); // intersection point
        double ys = m * xs + t;

        // treat special cases

        if (xs < x1) return new Coord2(x1, y1);
        if (xs > x2) return new Coord2(x2, y2);

        return new Coord2(xs, ys);
    }

    /****************************************************************
    Angles
    *****************************************************************/

    // compute ABSOLUTE delta of 2 angles

    public static double DeltaAngle(double angle1, double angle2)
    {
        double delta = Math.abs(angle1 - angle2);
        if (delta > 180.0) delta = 360 - delta;
        return delta;
    }

    public static double Deg2Rad(double deg)
    {
        return deg / 180.0 * Math.PI;
    }

    public static double Rad2Deg(double rad)
    {
        return rad * 180.0 / Math.PI;
    }

    // this method retrieves the angle in degrees 
    //     y|p/
    //      |/
    //------------- x
    //      |
    //      |
    // p (phi) is the angle between y axis and the vector

    public static double GetPhi(Coord2 vector)
    {
        double heading = Math.atan2(vector.x, vector.y);
        heading = heading / Math.PI * 180.0;
        if (heading < 0.0) heading += 360.0;
        return heading;
    }

    // get a vector from an angle with a specified length
    // phi must be in deg

    public static Coord2 GetVector(double phi, double length)
    {
        double phiRad = phi / 360F * 2F * (double)Math.PI;
        Coord2 vector = new Coord2();
        vector.x = (double)Math.sin((double)phiRad) * length;
        vector.y = (double)Math.cos((double)phiRad) * length;
        return vector;
    }

    // get a point from an angle with a specified distance from a given point
    // phi must be in deg

    public static Coord2 GetVector(double phi, double length, Coord2 origin)
    {
        double phiRad = phi / 360F * 2F * (double)Math.PI;
        Coord2 vector = new Coord2();
        vector.x = (double)Math.sin((double)phiRad) * length + origin.x;
        vector.y = (double)Math.cos((double)phiRad) * length + origin.y;
        return vector;
    }

    // compute heading when going from currentPosition to nextTrackPosition

    public static double ComputeHeadingAngle(Position currentPosition, Position nextTrackPosition)
    {
        Coord2 p = ProjectEarth2Sky(nextTrackPosition.longitude, nextTrackPosition.latitude,
            currentPosition.longitude, currentPosition.latitude, EARTH_RADIUS);
        return GetPhi(p);
    }

    // compute opening angle between 3 positions
    // p1 --- p (middle) --- p2
    // returns: angle > -180� < 180�

    public static int ComputeOpeningAngle(Position p1, Position p, Position p2)
    {
        Coord2 vectorP1P = GPSMath.ProjectEarth2Sky(p1.longitude, p1.latitude,
            p.longitude, p.latitude, GPSMath.EARTH_RADIUS);
        double headingP1P = GPSMath.GetPhi(vectorP1P);

        Coord2 vectorPP2 = GPSMath.ProjectEarth2Sky(p.longitude, p.latitude,
            p2.longitude, p2.latitude, GPSMath.EARTH_RADIUS);
        double headingPP2 = GPSMath.GetPhi(vectorPP2);

        int deltaHeading = (int)headingPP2 - (int)headingP1P;
        if (deltaHeading > 180) deltaHeading = deltaHeading - 360;
        if (deltaHeading < -180) deltaHeading = deltaHeading + 360;
        return deltaHeading;
    }


    /****************************************************************
    Transformations
    *****************************************************************/

    // make 3d cartesian coordinates from longitude and latitude

    public static Coord3 ConvertToCoord3(double longitude, double latitude)
    {
        double phi = Deg2Rad(latitude);
        double lambda = Deg2Rad(longitude);
        Coord3 point = new Coord3();
        point.x = Math.cos(phi) * Math.sin(lambda);
        point.y = Math.sin(phi);
        point.z = Math.cos(phi) * Math.cos(lambda);
        return point;
    }

    // make latitude/longitude coordinates from 3d cartesian coordinates
    // precondition: 3d point must be on unit sphere (r = 1)

    public static Position ConvertToPosition(Coord3 coord3)
    {
        Position pos = new Position();
        pos.latitude = Rad2Deg(Math.asin(coord3.y));
        pos.longitude = Rad2Deg(Math.atan2(coord3.x, coord3.z));
        while (pos.latitude > MAX_LAT) pos.latitude -= (MAX_LAT - MIN_LAT);
        while (pos.latitude < MIN_LAT) pos.latitude += (MAX_LAT - MIN_LAT);
        while (pos.longitude > MAX_LON) pos.longitude -= (MAX_LON - MIN_LON);
        while (pos.longitude < MIN_LON) pos.longitude += (MAX_LON - MIN_LON);
        return pos;
    }

    /****************************************************************
    Rotations
    *****************************************************************/

    // rotate by Y axis (changing longitude)

    public static Coord3 RotateY(Coord3 p, double angle)
    {
        double phi = Deg2Rad(angle);
        Coord3 pn = new Coord3();
        pn.x = p.x * Math.cos(phi) + p.z * Math.sin(phi);
        pn.y = p.y;
        pn.z = p.z * Math.cos(phi) - p.x * Math.sin(phi);
        return pn;
    }

    // rotate by X axis (changing latitude)

    public static Coord3 RotateX(Coord3 p, double angle)
    {
        double phi = Deg2Rad(angle);
        Coord3 pn = new Coord3();
        pn.x = p.x;
        pn.y = p.y * Math.cos(phi) + p.z * Math.sin(phi);
        pn.z = p.z * Math.cos(phi) - p.y * Math.sin(phi);
        return pn;
    }

    /****************************************************************
    Projections
    *****************************************************************/

    // project 3d coordinates to a 2d surface defined by the normal
    // given by longitudeRef and latitudeRef

    public static Coord2 ProjectEarth2Sky(double longitudeOrig, double latitudeOrig,
        double longitudeRef, double latitudeRef, double radius)
    {
        Coord3 p1 = ConvertToCoord3(longitudeOrig, latitudeOrig);
        Coord3 p2 = RotateY(p1, -longitudeRef);
        Coord3 p3 = RotateX(p2, -latitudeRef);
        Coord2 pn = new Coord2();
        // p3.z should be almost 1.0
        pn.x = p3.x * radius / p3.z;
        pn.y = p3.y * radius / p3.z;
        return pn;
    }

    // project 2d surface (sky) coordinates back to earth coordinates with reference
    // given by longitudeRef and latitudeRef

    public static Position ProjectSky2Earth(Coord2 coord2d,
        double longitudeRef, double latitudeRef, double radius)
    {
        Coord3 p1 = new Coord3();
        p1.x = coord2d.x / radius;
        p1.y = coord2d.y / radius;
        p1.z = 1.0;
        Coord3 p2 = RotateX(p1, latitudeRef);
        Coord3 p3 = RotateY(p2, longitudeRef);
        Position pos = ConvertToPosition(p3);
        return pos;
    }

    /****************************************************************
    Testing
    *****************************************************************/

    // this method is for testing purposes only!

    public static Coord3 TestRotation(double longitude, double latitude)
    {
        Coord3 p1 = ConvertToCoord3(longitude, latitude);
        Coord3 p2 = RotateY(p1, -longitude);
        Coord3 p3 = RotateX(p2, -latitude);
        return p3;
    }

    // this method is for testing purposes only!

    /* code for testing
     * 
     *  double lat = 80.0;
        double lon = 100.0;
        Position dings = GPSMath.TestProjection(lon, lat);

        lat = -80.0;
        lon = 100.0;
        dings = GPSMath.TestProjection(lon, lat);

        lat = 80.0;
        lon = -100.0;
        dings = GPSMath.TestProjection(lon, lat);

        lat = -80.0;
        lon = -170.0;
        dings = GPSMath.TestProjection(lon, lat);
     */

    public static Position TestProjection(double longitude, double latitude)
    {
        Coord3 p1 = ConvertToCoord3(longitude, latitude);
        Position p2 = ConvertToPosition(p1);
        return p2;
    }

    public static double GetDegreesFromAngular(double dbl)
    {
        dbl /= 100;

        int i = (int)dbl;
        double frac = (dbl - (double)i) * (100.0 / 60.0);

        return ((double)i + frac);
    }

}


