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


package snpviewer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 *
 * @author david
 */
public class SnpFile extends Service<SnpFile> implements Serializable {
    private String sampleName;
    File inputFile;
    File outputDirectory;
    HashMap<String, File> chromFiles = new HashMap<>();
    File headerFile;
    String header;
    File commentFile;
    HashMap<String, Integer> chromLengths = new HashMap<>();
    String buildVersion; //check in comment lines
    ArrayList<String> requiredFields;
    HashMap<String, Integer> columns = new HashMap<>();
    Double qualityFilter;
    boolean hasQualityField = false;
    Double meanQuality;
    Double percentNoCall;

    SnpFile(File file, File outDir){
        inputFile = file;
        outputDirectory = outDir;
        requiredFields = new ArrayList<>(Arrays.asList(
            "chromosome", "chromosomal position", "call codes", "dbsnp rs id"));
        //readAndSortInput(file, snpFileDir);
    }
    public void setOutputDirectory(File directory){
        outputDirectory = directory;
    }
    public File getOutputDirectory(){
        return outputDirectory;
    }
    public void setSampleName(String name){
        sampleName = name;
    }
    public String getSampleName(){
        return sampleName;
    }
    public String getInputFileName(){
        return inputFile.getName();
    }
    public File getInputFile(){
        return inputFile;
    }
    public String getOutputDirectoryName(){
        return outputDirectory.getAbsolutePath();
    }
    
    public void setQualityFilter(Double filter){
        qualityFilter = filter;
    }
    public Double getQualityFilter(){
        return qualityFilter;
    }
    public boolean hasQualityField(){
        return hasQualityField;
    }
    
    public Double getPercentNoCall(){
        return percentNoCall;
    }
    public Double getMeanQuality(){
        return meanQuality;
    }
    
    public void setBuildVersion(String build){
        buildVersion = build;
    }

    public String getBuildVersion(){
        return buildVersion;
    }
    


    @Override
    protected  Task<SnpFile> createTask() {
        final File outputDir = outputDirectory;
        final File file = inputFile;
        return new Task() {
            @Override
            protected Object call() throws Exception {

                int totalLines;
                try{
                    totalLines = LineCount.count(file);
                }catch (Exception ex) {
                    //ex.printStackTrace();
                    throw new SnpFileException();
                }
                BufferedReader reader = null;
                try{
                    FileReader fileReader = new FileReader(file);
                    reader = new BufferedReader(fileReader);
                    String line;

                    //TO DO
                    //change code below to implment an array of SnpLines instead

                    ArrayList<SnpLine> sortBuffer = new ArrayList<>();
                    List<String> comments = new ArrayList<>();
                    boolean foundHeader = false;
                    int lineNumber = 0;
                    int totalCalls = 0;
                    int totalNoCalls = 0;
                    Double sumConfidence = new Double(0);
                    updateMessage("Step 1 of 3: Reading input...");
                    while ((line = reader.readLine()) != null){
                        if (isCancelled()){
                            updateMessage("Cancelled");
                            return null;
                        }
                        lineNumber++;
                        updateProgress(lineNumber, totalLines);

                        if (line.startsWith("#")){
                            comments.add(line);
                            if (buildVersion == null){
                                /*check build is valid (and accounted for 
                                 * in our chromSize class) then set build
                                 */
                                Pattern buildPattern = Pattern.compile("genome-version-\\w+=(\\w+)");
                                Matcher buildMatch = buildPattern.matcher(line);
                                buildMatch.find();
                                try{
                                    String headerBuild = buildMatch.group(1);
                                    ChromosomeLength chromLength = new ChromosomeLength();
                                    try{
                                        chromLength.setBuild(headerBuild);
                                        buildVersion = headerBuild;
                                    }catch (Exception ex){
                                        Logger.getLogger(SnpFile.class.getName()).log(Level.SEVERE, null, ex);
                                        throw new SnpFileException("Build version " + headerBuild + " not recognised");
                                    }
                                }catch(Exception ex){
                                    //not a genome version comment - not a problem
                                }
                            }

                            continue;
                        }
                        if (! foundHeader){
                            boolean headerOk = (setHeader(line));
                            if (! headerOk){
                                System.out.println("Header no good!");
                                throw new SnpFileException();
                            }else{
                                foundHeader = true;
                                /*if (buildVersion == null){
                                    buildVersion = "hg19"; //DEFAULT GENOME BUILD
                                }*/
                                continue;
                            }
                        }
                        
                        String splitLine[] = line.split("\t");
                        if (splitLine[columns.get("chromosome")].equals("---")){
                            continue;
                        }
                        
                        try{
                            SnpLine snpLine = new SnpLine(line);
                            if (snpLine.getCall().equalsIgnoreCase("nocall")){
                                totalNoCalls++;
                            }
                            if (snpLine.getConfidence() != null && 
                                    !snpLine.getCall().equalsIgnoreCase("nocall")){
                                sumConfidence += snpLine.getConfidence();
                                totalCalls++;
                            }
                            sortBuffer.add(snpLine);
                        }catch (Exception ex){
                            Logger.getLogger(SnpFile.class.getName()).log(Level.SEVERE, null, ex);
                            throw new SnpFileException();
                        }
                    }
                    if (totalNoCalls > 0){
                        percentNoCall =  100 * (double) totalNoCalls/sortBuffer.size();
                    }
                    if (sumConfidence > 0 && !sortBuffer.isEmpty()){
                        meanQuality = sumConfidence/sortBuffer.size();
                    }
                    reader.close();
                    reader = null;
                    try {
                        /*external sort prob not necessary given current filesizes 
                         */

                        updateMessage("Step 2 of 3: Sorting SNP calls...");
                        updateProgress(-1, -1);
                        CoordinateCompare coordinateCompare = new CoordinateCompare();
                        java.util.Collections.sort(sortBuffer, coordinateCompare);
                        if (isCancelled()){
                            updateMessage("Cancelled");
                            return null;
                        }
                    }catch (Exception ex){
                        //display error here?
                        Logger.getLogger(SnpFile.class.getName()).log(Level.SEVERE, null, ex);
                        throw new SnpFileException();
                    }
                    /*write sorted lines from sortBuffer to one file
                     * per chromsome plus one file for the header
                     */
                    updateMessage("Step 3 of 3: writing output...");
                    updateProgress(0, totalLines - comments.size() -1);
                    String prevChromosome = sortBuffer.get(0).getChromosome();
                    int prevIndex = 0;//index for start index of chromosome
                    int index;
                    for (index = 0; index < sortBuffer.size(); index++){
                        if (isCancelled()){
                            updateMessage("Cancelled");
                            return null;
                        }
                        updateProgress(index, (totalLines - comments.size()) -1);
                        String chromosome = sortBuffer.get(index).getChromosome();
                        if (! chromosome.equals(prevChromosome)){
                            //serialise an arraylist for each chrom
                            File chromFile = new File(outputDir.getPath() + "/chr" + prevChromosome + ".snpview");
                            ObjectOutputStream out = null;
                            try{
                                FileOutputStream fos = new FileOutputStream(chromFile);
                                out = new ObjectOutputStream(new BufferedOutputStream(fos));
                                ArrayList<SnpLine> subList = new ArrayList<>(sortBuffer.subList(prevIndex, index - 1));
                                removeDuplicateCoordinates(subList);//remove duplicate coordinates
                                out.writeObject(subList);
                                chromFiles.put(prevChromosome, chromFile);
                                /* index will always be more than 0 as we've set 
                                 * prevChromosome to the first chromosome before 
                                 * starting this loop
                                 */
                            }catch (IOException ex){
                                ex.printStackTrace();
                                throw new SnpFileException();
                            }finally{
                                if (out != null){
                                    out.close();
                                }
                            }

                            prevChromosome = chromosome;
                            prevIndex = index;
                        }
                    }
                    File chromFile = new File(outputDir.getPath() + "/chr" + prevChromosome + ".snpview");
                    ObjectOutputStream out = null;
                    try{
                        FileOutputStream fos = new FileOutputStream(chromFile);
                        out = new ObjectOutputStream(new BufferedOutputStream(fos));
                        ArrayList<SnpLine> subList = new ArrayList<SnpLine>(sortBuffer.subList(prevIndex, index - 1));
                        removeDuplicateCoordinates(subList);
                        out.writeObject(subList);
                        chromFiles.put(prevChromosome, chromFile);
                        /* index will always be more than 0 as we've set 
                         * prevChromosome to the first chromosome before 
                         * starting this loop
                         */
                    }catch (IOException ex){
                        ex.printStackTrace();
                        throw new SnpFileException();
                    }finally{
                        if (out != null){
                            out.close();
                        }

                    }
                    headerFile = new File(outputDir.getPath() + "/header.txt");
                    BufferedWriter writer = new BufferedWriter(new FileWriter(headerFile));
                    writer.write(header);
                    writer.newLine();
                    writer.close();
                    addHeaderFile(headerFile);
                    commentFile = new File(outputDir.getPath() + "/comment.txt");
                    writer = new BufferedWriter(new FileWriter(commentFile));
                    for (String comment : comments){
                        writer.write(comment);
                        writer.newLine();
                    }
                    writer.close();
                    addCommentFile(commentFile);

                }catch(IOException | SnpFileException ex){
                    //display error here?
//                    ex.printStackTrace();
                    throw new SnpFileException();
                }finally{
                    if (reader != null){
                        reader.close();
                    }
                }
                return true;
            }
        };
    }
    /*check for snps with duplicate coordinates and remove duplicate with
     * the worst (highest) confidence value (if available), else arbitrarily remove
     * the last
     */
    public void removeDuplicateCoordinates(ArrayList<SnpLine> snps){
        Integer prevCoordinate = null;//check for duplicate coordinates
        Double prevConfidence = null;//in case of duplicate coords pick the most confident call
        ArrayList<Integer> indicesToRemove = new ArrayList<>();
        for (int i = 0; i < snps.size(); i++){
            if (prevCoordinate == null){
                prevCoordinate = snps.get(i).getPosition();
                prevConfidence = snps.get(i).getConfidence();
                continue;
            }
            if (prevCoordinate == snps.get(i).getPosition()){
                if (snps.get(i).getConfidence() != null && prevConfidence != null){
                    if (snps.get(i).getConfidence() > prevConfidence){
                        indicesToRemove.add(i);
                    }else{
                        indicesToRemove.add(i-1);
                    }
                }else{
                    indicesToRemove.add(i);
                }
            }
            prevCoordinate = snps.get(i).getPosition();
            prevConfidence = snps.get(i).getConfidence();
        }
        if (!indicesToRemove.isEmpty()){
            Collections.sort(indicesToRemove, Collections.reverseOrder());
            for (int i: indicesToRemove){
                snps.remove(i);
            }
        }
    }

    public void addChromFile(String chrom, File chromFile){
        chromFiles.put(chrom, chromFile);
    }
    public void clearChromFiles(){
        chromFiles.clear();
    }

    public ArrayList<SnpLine> readChromFile(String chrom){
        if (! chromFiles.containsKey(chrom)){
            return null;
        }
        File objectFile = chromFiles.get(chrom);
        return readChromFile (objectFile);
    }

    public ArrayList<SnpLine> readChromFile(File chromFile){
        ArrayList<SnpLine> snpLines = new ArrayList<>();
        try{
            ObjectInputStream is = new ObjectInputStream(new BufferedInputStream
                    (new FileInputStream(chromFile)));
            try {
                snpLines = (ArrayList<SnpLine>) is.readObject();
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(SnpFile.class.getName()).log(Level.SEVERE, null, ex);
            }
            is.close();
        }catch (IOException ex) {
            Logger.getLogger(SnpFile.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (qualityFilter != null){
            filterOnQuality(snpLines);
        }
        return snpLines;
    }
    /*whenever we read SNPs from a chromFile we check if qualityFilter is set
     * and if so we use this method to remove SNPs that have a worse (higher) 
     * quality score, where available
     */
    public void filterOnQuality(ArrayList<SnpLine> snps){
        if (qualityFilter == null || qualityFilter < 0){
            return;
        }
        
        ArrayList<Integer> indicesToRemove = new ArrayList<>();
        for (int i = 0; i < snps.size(); i++){
            if (snps.get(i).getConfidence() != null && 
                    snps.get(i).getConfidence() > qualityFilter){
                indicesToRemove.add(i);
            }
        }
        if (!indicesToRemove.isEmpty()){
            Collections.sort(indicesToRemove, Collections.reverseOrder());
            for (int index: indicesToRemove){
                snps.remove(index);
            }
        }
    }

    public void addHeaderFile(File hFile){
        headerFile = hFile;
    }
    public File getHeaderFile(){
        return headerFile;
    }
    public void addCommentFile(File cFile){
        commentFile = cFile;
    }
    public File getCommentFile(){
        return commentFile;
    }
    public HashMap<String, File> getChromFiles(){
        return chromFiles;
    }

    /* setHeader takes care of this
    public boolean setColumns(HashMap<String, Integer> c){
        //returns true if all required fields are present and
         // greater than 0

        columns = c;
        for (String f: requiredFields){
            if (! columns.containsKey(f)){
                return false;
            }else{
                if (columns.get(f) < 0){
                    return false;
                }
            }
        }
        return true;
    }
    */
    public class SnpLine implements Serializable {
        private String chromosome;
        private int position;
        private String id;
        private String call;
        private Double confidence;
        /* constructor takes tab-delimited line from birdseed file
         * to produce a snp HashMap containing required fields
         */
        SnpLine (String line) throws SnpFileException {
            confidence = null;
            String[] splitLine = line.split("\t");
            if (columns.isEmpty()){
                //columns is only empty if setHeader was unsuccessful
                throw new SnpFileException("Uninitialized header when reading SnpLine");
            }
            if (splitLine[columns.get("chromosome")].equals("---") || 
                    ! splitLine[columns.get("chromosome")].matches("[\\w]+")){
                throw new SnpFileException("No associated chromosome for SnpLine");
            }
            for (String f: requiredFields){
                if (splitLine.length <= columns.get(f)){
                    throw new SnpFileException("Snp line does not contain enough fields");
                }
                if (! columns.containsKey(f)){
                    throw new SnpFileException("SnpFile header does not contain field " + f);
                }
                if (f.equalsIgnoreCase("chromosome")){
                    chromosome = splitLine[columns.get(f)];
                }else if (f.equalsIgnoreCase("chromosomal position")){
                    position = Integer.valueOf(splitLine[columns.get(f)]);
                }else if (f.equalsIgnoreCase("call codes")){
                    call = splitLine[columns.get(f)];
                }else if (f.equalsIgnoreCase("dbsnp rs id")){
                    id = splitLine[columns.get(f)];
                }
            }
            if (columns.containsKey("confidence")){
                hasQualityField = true;
                confidence = Double.valueOf(splitLine[columns.get("confidence")]);
            }
            
        }
        /* alternative constructor takes a hash map to 
         * create snp HashMap - must have all required fields
         */
        SnpLine(HashMap<String, String> c) throws SnpFileException {
            confidence = null;
            for (String f: requiredFields){
                if (! c.containsKey(f)){
                    throw new SnpFileException("Required field " + f + 
                            " not found in HashMap passed to SnpLine constructor");
                }else{
                    if (f.equalsIgnoreCase("chromosome")){
                        chromosome = c.get(f);
                    }else if (f.equalsIgnoreCase("chromosomal position")){
                        position = Integer.valueOf(c.get(f));
                    }else if (f.equalsIgnoreCase("call codes")){
                        call = c.get(f);
                    }else if (f.equalsIgnoreCase("dbsnp rs id")){
                        id = c.get(f);
                    }
                }
            }
            if (c.containsKey("confidence")){
                hasQualityField = true;
                confidence = Double.valueOf(c.get("confidence"));
            }
        }//end SnpLine constructors

        public String getChromosome(){
                return chromosome;
        }
        public int getPosition(){
            return position;
        }
        public String getId(){
            return id;
        }
        public String getCall(){
            return call;
        }
        public Double getConfidence(){
            return confidence;
        }
        public void setChromosome(String c){
            chromosome = c;
        }
        public void setPosition(int p){
            position = p;
        }
        public void setId(String i){
            id = i;
        }
        public void setCall(String c){
            call = c;
        }
        public void setConfidence(Double d){
            confidence = d;
        }
    }


    class CoordinateCompare implements Comparator<SnpLine>{
            @Override
            public int compare(SnpLine a, SnpLine b){
                int c = a.getChromosome().compareTo(b.getChromosome());
                if (c == 0){
                    return  a.getPosition() - b.getPosition();
                }
                return c;
            }
    }

    public class SnpFileException extends Exception{
        public SnpFileException() { super(); }
        public SnpFileException(String message) { super(message); }
        public SnpFileException(String message, Throwable cause) { super(message, cause); }
        public SnpFileException(Throwable cause) { super(cause); }
    }


    
    public boolean setHeader(String line){
        //return true if header is ok, false otherwise
        //header and columns variables only set if header is ok
        String replace = line.toLowerCase().replaceFirst("result_call", "call codes");//both these replaces
        String replace2 = replace.replaceFirst("physical position", //are to maintain 
                "chromosomal position");//compatibility with autoSnpa xls files

        List<String> headerFields = Arrays.asList(replace2.split("\t"));
        HashMap<String, Integer> colCheck = new HashMap<>();
        for (String field: requiredFields){
            int index = headerFields.indexOf(field);
            if (index < 0){
                return false;
            }else{
                colCheck.put(field, index);
            }
        }
        columns = colCheck;
        int index = headerFields.indexOf("confidence");
        if (index >= 0){
            columns.put("confidence", index);
        }
        header = line;
        return true;
    }
    
    public List<SnpLine> getSnpsInRegion(String chrom, int start, int end){
        if (!chromFiles.containsKey(chrom)){
            return null;
        }
        ArrayList<SnpLine> snpLines = readChromFile(chrom);
        int s = binSearchNearestCoordinate(start, snpLines, true);
        int e = binSearchNearestCoordinate(end, snpLines, false);
        return snpLines.subList(s, e + 1);
        
    }
    
    public List<SnpLine> getSnpsInRegion(String chrom, int start, int end, int flanks){
        if (!chromFiles.containsKey(chrom)){
            return null;
        }
        ArrayList<SnpLine> snpLines = readChromFile(chrom);
        int s = binSearchNearestCoordinate(start, snpLines, true);
        int e = binSearchNearestCoordinate(end, snpLines, false);
        s = Math.max(0, s - flanks);
        e = Math.min(e + flanks, snpLines.size() -1);
        return snpLines.subList(s, e + 1);
        
    }
    
    public List<SnpLine> getFlankingSnps(String chrom, int start, int end){
        if (!chromFiles.containsKey(chrom)){
            return null;
        }
        ArrayList<SnpLine> snpLines = readChromFile(chrom);
        int s = binSearchNearestCoordinate(start, snpLines, true);
        int e = binSearchNearestCoordinate(end, snpLines, false);
        SnpLine startSnp = snpLines.get(s);
        SnpLine endSnp = snpLines.get(e);
        ArrayList<SnpLine> snpsToReturn = new ArrayList<>();
        snpsToReturn.add(startSnp);
        snpsToReturn.add(endSnp);
        return snpsToReturn;
        
    }
    
    public SnpLine findClosestSnp(String chrom, int coordinate, final boolean refineBackwards){
        if (!chromFiles.containsKey(chrom)){
            return null;
        }
        ArrayList<SnpLine> snpLines = readChromFile(chrom);
        int i = binSearchNearestCoordinate(coordinate, snpLines, refineBackwards);
        SnpLine snpFound = snpLines.get(i);
        return snpFound;
    }

    public int binSearchNearestCoordinate (int c, ArrayList<SnpLine> snps, final boolean lookBack){
        /* look for coordinate (c) in snps and return index if found.
         * If not found look either backward or forward in snps (depending 
         * on boolean lookBack) and return the first value less than (if 
         * lookBack is true) or more than (if lookBack is false) coordinate
         */
        int l = 0;
        int u = snps.size() - 1;
        int i = 0;
        while (l <= u){
            i = (u + l) / 2;
            int x = snps.get(i).getPosition();
            if (x > c){
                u = i - 1;
            }else if (x < c){
                l = i + 1;
            }else{
                return i;//found
            }
        }
        //not found - return closest snp in whichever direction
        if (lookBack){
            for (int j = i; j > 0; j--){
                if (snps.get(j).getPosition() < c){
                    return j;
                }
            }
            return 0;
        }else{
            for (int j = i; j < snps.size(); j++){
                if (snps.get(j).getPosition() > c){
                    return j;
                }
            }
            return snps.size() - 1;
        }
    }
}