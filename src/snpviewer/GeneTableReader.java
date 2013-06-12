/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package snpviewer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 *
 * @author david
 */
public class GeneTableReader {
    File geneTable = new File ("refGenehg19.txt");
    GeneTableReader(){
        try{
            FileReader fileReader = new FileReader(geneTable);
            BufferedReader reader = new BufferedReader(fileReader);
            String line;
            while (( line = reader.readLine()) != null){
                String splitLine[] = line.split("\t");
            }
        }catch (Exception ex){
            //TO DO - show Error?
        }
    }
}
