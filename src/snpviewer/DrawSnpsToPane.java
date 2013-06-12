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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.layout.Pane;

/**
 *
 * @author david
 */
public class DrawSnpsToPane extends Service{
    Pane pane;
    SnpFile snpFile;
    String chrom;
    HashMap<String, Integer> colorMap = new HashMap<>();
    Double startPos;
    Double endPos;
    DrawSnpsToPane(Pane p, SnpFile s, String c){
        this(p, s, c, 0, 1, 2, null, null);
    }
    DrawSnpsToPane(Pane p, SnpFile s, String c, double start, double end){
        this(p, s, c, 0, 1, 2, start, end);
    }
    DrawSnpsToPane(Pane p, SnpFile s, String c, int aa, int bb, int ab){
        this(p, s, c, aa, bb, ab, null, null);
    }
    DrawSnpsToPane(Pane p, SnpFile s, String c, int aa, int bb, int ab, Double start, Double end){
        
        pane = p;
        snpFile = s;
        chrom = c;
        colorMap.put("aa", aa);
        colorMap.put("bb", bb);
        colorMap.put("ab", ab);
        startPos = start;
        endPos = end;
    }
    
    
    
    @Override
    protected  Task<ArrayList<HashMap<String, Double>>> createTask() {
        return new Task<ArrayList<HashMap<String, Double>>>() {
            @Override
            protected ArrayList<HashMap<String, Double>> call() throws Exception {
                ArrayList<HashMap<String, Double>> lines = new ArrayList<>();
                double width = pane.getMinWidth();
                double height = pane.getMinHeight();
                if (! snpFile.chromFiles.containsKey(chrom)){
                    return null;
                }
                List<SnpFile.SnpLine> snpLines = new ArrayList<>();                
                ChromosomeLength chromLength = new ChromosomeLength();
                double factor;
                if (startPos == null || endPos == null){
                    snpLines = snpFile.readChromFile(chrom);
                                               
                    try {
                        factor = width/chromLength.getlength(snpFile.buildVersion, chrom);
                    }catch (Exception ex){
                        //use last coordinate for scaling if chrom length unavailable
                        SnpFile.SnpLine lastLine = snpLines.get(snpLines.size()-1);
                        double lastPos = (double) lastLine.getPosition();
                        //System.out.println("Last position is " + lastPos);
                        factor = width/lastPos;
                    }
                }else{
                    snpLines = snpFile.getSnpsInRegion(chrom, startPos.intValue(), 
                            endPos.intValue());
                    factor = width / (endPos - startPos);
                }
                if(snpLines == null){
                    return null;
                }
                int lineNumber = 0;
                int totalLines = snpLines.size();
                updateMessage("Drawing genotypes");
                for (SnpFile.SnpLine callLine: snpLines){
                    if (isCancelled()){
                        updateMessage("Cancelled");
                        lines.clear();
                    }
                    updateProgress(++lineNumber, totalLines);
                    HashMap<String, Double> lineToDraw = new HashMap<>();
                    double coordinate = (double) callLine.getPosition();
                    if (startPos != null){
                        coordinate -= startPos;
                        if (coordinate < 0){
                            continue;
                        }
                    }
                    lineToDraw.put("x", factor * coordinate);
                    lineToDraw.put("y", height);
                    String call = callLine.getCall();
                    if (colorMap.containsKey(call.toLowerCase())){
                        lineToDraw.put("color", colorMap.get(call.toLowerCase()).doubleValue());
                        lines.add(lineToDraw);
                    }else if (call.equalsIgnoreCase("NoCall")){
                        //lineToDraw.setStroke(Color.GRAY);
                        continue;
                    }else{//ERROR! - should throw an exception really
                        System.out.println("Error for call code " + call);
                        //lineToDraw.setStroke(Color.YELLOW);
                        continue;
                    }
                }

                return lines;
        
            }//end of call
        };//end of task
    }//end of create task


}
