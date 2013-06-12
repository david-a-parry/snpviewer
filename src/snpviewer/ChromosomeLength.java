/*
 * Snp Viewer - a program for viewing SNP data and identifying regions of homozygosity
 * Copyright (C) 2013 David A. Parry
 * d.a.parry@leeds.ac.uk
 * https://sourceforge.net/projects/snpviewer/
 * 
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Simply provides a hash map for different genome builds.
 * This hash map provides a hash map of chromosome names
 * to chromosome lengths in bp.
 */
package snpviewer;

import java.util.HashMap;

/**
 *
 * @author david
 */
public class ChromosomeLength  {
    public static final HashMap<String, HashMap> chromosomeLengths = new HashMap<>();
    public String build; 
    static{
        initializeBuilds();
    }
    public  ChromosomeLength(){
        build = "hg19";
    }
    
    public  ChromosomeLength(String b) throws ChromosomeLengthException{
        if (chromosomeLengths.containsKey(b)){
            build = b;
        }else{
            throw new ChromosomeLengthException("Build " + build + " not found.");
        }
    }
    
    
    public void setBuild (String b) throws ChromosomeLengthException{
        if (chromosomeLengths.containsKey(b)){
            build = b;
        }else{
            throw new ChromosomeLengthException("Build " + build + " not found.");
        }
    }
    //get chrom size by providing build b and chromosome c
    public Integer getlength(String b, String c) throws ChromosomeLengthException{
        if (chromosomeLengths.containsKey(b)){
            if (chromosomeLengths.get(b).containsKey(c)){
                return (Integer) chromosomeLengths.get(b).get(c);
             }else{
                throw new ChromosomeLengthException("Chromosome " + c + " not found for build " + b);
            }
        }else{
            throw new ChromosomeLengthException("Build " + b + " not found.");
        }
    }
    
    /*get chrom size by providing chromosome c, assuming build has been set or
     * user wants default (hg19)
     */
    public Integer getLength(String c) throws ChromosomeLengthException{
        if (chromosomeLengths.get(build).containsKey(c)){
            return (Integer) chromosomeLengths.get(build).get(c);
        }else{
            throw new ChromosomeLengthException("Chromosome " + c + " not found for build " + build);
        }
    }
    
    private static void initializeBuilds(){
        HashMap<String, Integer> hg19 = new HashMap<>();
        HashMap<String, Integer> hg18 = new HashMap<>();

        hg19.put("1", 249250621); 
        hg19.put("2", 243199373); 
        hg19.put("3", 198022430);
        hg19.put("4", 191154276); 
        hg19.put("5", 180915260);
        hg19.put("6", 171115067); 
        hg19.put("7", 159138663); 
        hg19.put("8", 146364022); 
        hg19.put("9", 141213431); 
        hg19.put("10", 135534747);
        hg19.put("11", 135006516); 
        hg19.put("12", 133851895); 
        hg19.put("13", 115169878); 
        hg19.put("14", 107349540); 
        hg19.put("15", 102531392);
        hg19.put("16", 90354753); 
        hg19.put("17", 81195210); 
        hg19.put("18", 78077248); 
        hg19.put("19", 59128983); 
        hg19.put("20", 63025520);
        hg19.put("21", 48129895); 
        hg19.put("22", 51304566); 
        hg19.put("X", 155270560); 
        hg19.put("Y", 59373566); 
        hg19.put("MT", 16571 );
        
        hg18.put("1", 247249719); 
        hg18.put("2", 242951149); 
        hg18.put("3", 199501827); 
        hg18.put("4", 191273063); 
        hg18.put("5", 180857866);             
        hg18.put("6", 170899992);
        hg18.put("7", 158821424); 
        hg18.put("8", 146274826); 
        hg18.put("9", 140273252); 
        hg18.put("10", 135374737);
        hg18.put("11", 134452384); 
        hg18.put("12", 132349534); 
        hg18.put("13", 114142980); 
        hg18.put("14", 106368585); 
        hg18.put("15", 100338915);
        hg18.put("16", 88827254); 
        hg18.put("17", 78774742); 
        hg18.put("18", 76117153); 
        hg18.put("19", 63811651); 
        hg18.put("20", 62435964);
        hg18.put("21", 46944323); 
        hg18.put("22", 49691432); 
        hg18.put("X", 154913754); 
        hg18.put("Y", 57772954); 
        hg18.put("MT", 16571);
        
        chromosomeLengths.put("hg19", hg19);
        chromosomeLengths.put("hg18", hg18);
        
        chromosomeLengths.put("37", hg19);
        chromosomeLengths.put("36", hg18);
    }
    
    public class ChromosomeLengthException extends Exception{
            public ChromosomeLengthException() { super(); }
            public ChromosomeLengthException(String message) { super(message); }
            public ChromosomeLengthException(String message, Throwable cause) { super(message, cause); }
            public ChromosomeLengthException(Throwable cause) { super(cause); }
        }
    
}
