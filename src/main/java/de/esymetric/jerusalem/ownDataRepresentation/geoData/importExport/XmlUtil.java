package de.esymetric.jerusalem.ownDataRepresentation.geoData.importExport;

import net.n3.nanoxml.XMLElement;

public class XmlUtil {
    public static XMLElement getChildByPath(XMLElement element, String path) {
        while (path.startsWith("/"))
            path = path.substring(1);
        if (path.length() == 0)
            return element;
        int p = path.indexOf('/');
        if (p < 0)
            return getChild(element, path);

        String head = path.substring(0, p);
        String tail = path.substring(p + 1);
        XMLElement n = getChild(element, head);
        if (n == null)
            return null;
        else
            return getChildByPath(n, tail);
    }

    public static XMLElement getChild(XMLElement element, String name) {
        for (int i = 0; i < element.getChildren().size(); i++) {
            XMLElement n = (XMLElement) element.getChildren().get(i);
            if (name.equals(n.getName()))
                return n;
        }
        return null;
    }

}
