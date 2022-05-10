package rebuilding;

import de.esymetric.jerusalem.osmDataRepresentation.OSMDataReader;
import de.esymetric.jerusalem.rebuilding.Rebuilder;
import de.esymetric.jerusalem.routing.Router;
import de.esymetric.jerusalem.routing.algorithms.TomsAStarStarRouting;
import de.esymetric.jerusalem.routing.heuristics.TomsRoutingHeuristics;
import de.esymetric.jerusalem.utils.BufferedRandomAccessFile;
import org.apache.tools.bzip2.CBZip2InputStream;
import org.junit.Test;
import routing.RouterTest;

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;

public class RebuilderTest {

    RouterTest routerTest = new RouterTest();

    @Test
    public void testRebuildAndRouting() throws Exception {
        String dataDirectoryPath = "testData";
        new File(dataDirectoryPath).mkdirs();
        String tempDirectoryPath = "tempData";
        new File(tempDirectoryPath).mkdirs();

        de.esymetric.jerusalem.rebuilding.Rebuilder rebuilder = new de.esymetric.jerusalem.rebuilding.Rebuilder(dataDirectoryPath, tempDirectoryPath,
                new TomsRoutingHeuristics(), false, false, false);
        FileInputStream fis = new FileInputStream("osmData/hadern2.osm.bz2");
        CBZip2InputStream bzis = new CBZip2InputStream(fis);
        OSMDataReader reader = new OSMDataReader(bzis, rebuilder, false);
        reader.read(new Date());
        bzis.close();
        fis.close();
        rebuilder.finishProcessingAndClose();
        System.out.println("BRAF-INFO: " +
                BufferedRandomAccessFile.getShortInfoAndResetCounters());


        // create quadtree index

        rebuilder = new de.esymetric.jerusalem.rebuilding.Rebuilder(dataDirectoryPath, tempDirectoryPath,
                new TomsRoutingHeuristics(), true, false, true);
        rebuilder.makeQuadtreeIndex();
        rebuilder.finishProcessingAndClose();

        // test

        Router router = new Router(dataDirectoryPath,
                new TomsAStarStarRouting(), new TomsRoutingHeuristics(), 120);
        Router.setDebugMode(true);
        routerTest.makeRoute(router, 48.107891, 11.461865, 48.099986, 11.511051,
                "durch-waldfriedhof", dataDirectoryPath);

        routerTest.makeRoute(router, 48.107608, 11.461648, 48.108656, 11.477371,
                "grosshadern-fussweg", dataDirectoryPath);

        routerTest.makeRoute(router, 48.116892, 11.487076,    48.117909, 11.472429,
                "eichenstrasse", dataDirectoryPath);
    }

    @Test
    public void testRebuildOnlyWays() throws Exception {
        String dataDirectoryPath = "testData";
        String tempDirectoryPath = "tempData";

        // rebuild full

        Rebuilder rebuilder = new Rebuilder(dataDirectoryPath,tempDirectoryPath,
                new TomsRoutingHeuristics(), false, false, false);
        FileInputStream fis = new FileInputStream("osmData/hadern2.osm.bz2");
        CBZip2InputStream bzis = new CBZip2InputStream(fis);
        OSMDataReader reader = new OSMDataReader(bzis, rebuilder, false);
        reader.read(new Date());
        bzis.close();
        fis.close();
        rebuilder.finishProcessingAndClose();

        // create quadtree index

        rebuilder = new Rebuilder(dataDirectoryPath, tempDirectoryPath,
                new TomsRoutingHeuristics(), true, false, true);
        rebuilder.makeQuadtreeIndex();
        rebuilder.finishProcessingAndClose();

        // rebuild only ways

        rebuilder = new Rebuilder(dataDirectoryPath, tempDirectoryPath,
                new TomsRoutingHeuristics(), true, true, false);
        fis = new FileInputStream("osmData/hadern2.osm.bz2");
        bzis = new CBZip2InputStream(fis);
        reader = new OSMDataReader(bzis, rebuilder, true);
        reader.read(new Date());
        bzis.close();
        fis.close();
        rebuilder.finishProcessingAndClose();


        // test

        Router router = new Router(dataDirectoryPath,
                new TomsAStarStarRouting(), new TomsRoutingHeuristics(), 120);
        Router.setDebugMode(true);
        routerTest.makeRoute(router, 48.107891, 11.461865, 48.099986, 11.511051,
                "durch-waldfriedhof", dataDirectoryPath);

        routerTest.makeRoute(router, 48.107608, 11.461648, 48.108656, 11.477371,
                "grosshadern-fussweg", dataDirectoryPath);

        routerTest.makeRoute(router, 48.107608, 11.461648, 48.108656, 11.477371,
                "grosshadern-fussweg", dataDirectoryPath);

        routerTest.makeRoute(router, 48.116892, 11.487076,    48.117909, 11.472429,
                "eichenstrasse", dataDirectoryPath);

    }

}
