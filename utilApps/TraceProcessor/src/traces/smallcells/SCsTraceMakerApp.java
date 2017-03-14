package traces.smallcells;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Xenofon Vasilakos xvas@aueb.gr
 */
public class SCsTraceMakerApp {

    public static void main(String[] args) {
        int maxCells = 25;

        int maxRows = 5;
        int maxColumns = 5;

        int x_start = 70, y_start = 70;

        int distBetweenRows = 140;
        int distBetweenColmns = 140;

        String note = "#\tEach line in the trace file complies with the following colon "
                + "separated format:\n"
                + "#\t<int id>;"
                + "\t<int coordinate x>;"
                + "\t<int coordinate y>;"
                + "\t<double maximum data transmission tr_rate=-1 to set according to "
                + "corresponding setup parameter>;"
                + "\t<double backhaul data rate bk_rate=-1 to set according to corresponding "
                + "setup parameter>"
                + "\t<NONE> to denote no neighborhood specified (see code for details);"
                + "\\n";

        String userHome = System.getProperty("user.home");
        try (FileWriter fwr = new FileWriter(
                new File(userHome + "/Dropbox/2014-2015-EPC+POP/trunk/files/sim/core/"
                        + "properties/scs-trace.txt")
        )) {

            fwr.write(note + "\n");

            for (int clmn = 0; clmn < maxColumns && maxCells > 0; clmn++) {
                for (int row = 0; row < maxRows && maxCells > 0; row++) {
                    int x = x_start + row * distBetweenRows;
                    int y = y_start + clmn * distBetweenColmns;
                    fwr.write((maxCells--) + ";");
                    fwr.write(x + ";");
                    fwr.write(y + ";");
                    fwr.write("-1;");//-1 for radius
                    fwr.write("-1;");//-1 for maximum data transmission
                    fwr.write("NONE");//no neighbors with probs

                    fwr.write('\n');
                }
            }
        } catch (IOException iox) {
            Logger.getLogger(
                    SCsTraceMakerApp.class.getCanonicalName()).
                    log(Level.SEVERE,
                            iox.getMessage() + "\n\t {0}",
                            iox.getStackTrace());
        }
    }

}
