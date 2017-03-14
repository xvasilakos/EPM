/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package traces.walkers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

/**
 *
 * @author xvas
 */
public class LastLinePostProcessor {

    public static void main(String[] args) throws FileNotFoundException {

        File dir = new File("C:\\Users\\xvas\\Dropbox\\2014-2015-EPC+POP\\trunk\\files\\unsync\\walkers\\sparse\\sparse/");
//        File dir = new File("C:\\Users\\xvas\\Dropbox\\2014-2015-EPC+POP\\trunk\\files\\unsync\\walkers\\medium\\medium/");
//        File dir = new File("C:\\Users\\xvas\\Dropbox\\2014-2015-EPC+POP\\trunk\\files\\unsync\\walkers\\dense\\dense/");
//        File dir = new File("C:\\Users\\xvas\\Dropbox\\2014-2015-EPC+POP\\trunk\\files\\unsync\\walkers\\subway\\subway/");
        for (String name : dir.list()) {
            if (!name.endsWith(".tr.csv")) {
                continue;
            }
            PrintWriter out = new PrintWriter(dir + "/" + name.substring(0, name.indexOf(".tr")) + "_meta.csv");

            File nxtFile = new File(dir, name);
            String lastline = tail(nxtFile);


            out.write(lastline);
            out.close();
        }
    }

    public static String tail(File file) {
        RandomAccessFile fileHandler = null;
        try {
            fileHandler = new RandomAccessFile(file, "r");
            long fileLength = fileHandler.length() - 1;
            StringBuilder sb = new StringBuilder();

            for (long filePointer = fileLength; filePointer != -1; filePointer--) {
                fileHandler.seek(filePointer);
                int readByte = fileHandler.readByte();

                if (readByte == 0xA) {
                    if (filePointer == fileLength) {
                        continue;
                    }
                    break;

                } else if (readByte == 0xD) {
                    if (filePointer == fileLength - 1) {
                        continue;
                    }
                    break;
                }

                sb.append((char) readByte);
            }

            String lastLine = sb.reverse().toString();
            return lastLine;
        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (fileHandler != null) {
                try {
                    fileHandler.close();
                } catch (IOException e) {
                    /* ignore */
                }
            }
        }
    }
}
