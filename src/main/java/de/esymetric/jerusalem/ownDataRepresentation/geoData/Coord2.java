package de.esymetric.jerusalem.ownDataRepresentation.geoData;

public class Coord2
{
    public double x;
    public double y;

    public Coord2() { }

    public Coord2(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    public double distance(Coord2 c)
    {
        double dx = c.x - x;
        double dy = c.y - y;
        return Math.sqrt(dx * dx + dy * dy);
    }
}

