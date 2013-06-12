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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
*
* @author david
*/
public class RegionSummary implements Comparable<RegionSummary>, Serializable{
    private int startPos;
    private int endPos;
    private int startIndex;
    private int endIndex;
    private Integer length;
    private String startId;
    private String endId;
    private String chromosome = new String();
    
    RegionSummary(){
        this(null, 0, 0, 0, 0, null, null);
    }
    RegionSummary(String chrom, int sp, int ep, int si, int ei){
        this(chrom, sp, ep, si, ei, null, null);
    }
    RegionSummary(int sp, int ep, int si, int ei){
        this(null, sp, ep, si, ei, null, null);
    }
    RegionSummary(int sp, int ep, int si, int ei, String sd, String ed){
        this(null, sp, ep, si, ei, sd, ed);
    }
    RegionSummary(String chrom, int sp, int ep, int si, int ei, String sd, String ed){
        startPos = sp;
        endPos = ep;
        startIndex = si;
        endIndex = ei;
        chromosome = chrom;
        startId = sd;
        endId = ed;
        length = endPos - startPos;
    }
    
    public void setChromosome(String c){
        chromosome = c;
    }
    public void setStartId(String id){
        startId = id;
    }
    public void setEndId(String id){
        endId = id;
    }
    public void setStartPos(int i){
        startPos = i;
    }
    public void setStartIndex(int i){
        startIndex = i;
    }
    public void setEndPos(int i){
        endPos = i;
    }
    public void setEndIndex(int i){
        endIndex = i;
    }
    public String getChromosome(){
        return chromosome;
    }
    public String getStartId(){
        return startId;
    }
    public String getEndId(){
        return endId;
    }
    public int getStartPos(){
        return startPos;
    }
    public int getStartIndex(){
        return startIndex;
    }
    public int getEndPos(){
        return endPos;
    }
    public int getEndIndex(){
        return endIndex;
    }
    public boolean isEmpty(){
        if (startIndex == 0 && endIndex == 0 && startPos == 0 && endPos == 0){
            return true;
        }else{
            return false;
        }
    }
    public int getLength(){
        length = endPos - startPos;
        return length;
    }
    public String getCoordinateString(){
        return chromosome + ":" + startPos + "-" + endPos; 
    }
    public String getBedLine(){
        return chromosome + "\\t" + startPos + "\\t" + endPos;
    }
    public String getIdLine(){
        return startId + ";" + endId;
    }

    @Override
    public int compareTo(RegionSummary r){
        if (chromosome == null && r.getChromosome() != null){
            return 1;
        }else if (chromosome != null && r.getChromosome() == null){
            return -1;
        }else if(chromosome != null && r.getChromosome() != null){
            ChromComparator chromCompare = new ChromComparator();
            int i = chromCompare.compare(chromosome,r.getChromosome());
            //int i =  chromosome.compareToIgnoreCase(r.getChromosome());
            if (i == 0){
                i = startPos - r.getStartPos();
                if (i != 0){
                    return i;
                }else{
                    return endPos - r.getEndPos();
                }
            }else{
                return i;
            }
        }else{
            int i = startPos - r.getStartPos();
            if (i != 0){
                return i;
            }else{
                return endPos - r.getEndPos();
            }
        }
    }
    public void mergeRegionsByPosition(ArrayList<RegionSummary> regions){
        if (regions.size() < 2){
            return;
        }
        Collections.sort(regions);
        Iterator<RegionSummary> rIter = regions.iterator();
        RegionSummary previousRegion = rIter.next();
        ArrayList<RegionSummary> merged = new ArrayList<>();
        while (rIter.hasNext()){
            RegionSummary r = rIter.next();
            if (r.getChromosome() == null && 
                    previousRegion.getChromosome() == null){
                if (previousRegion.getEndPos() >= r.getStartPos()){
                    if (previousRegion.getEndPos() < r.getEndPos()){
                        previousRegion.setEndPos(r.getEndPos());
                    }
                }else{
                    merged.add(previousRegion);
                    previousRegion = r;
                }
            }else if (r.getChromosome() == null || 
                    previousRegion.getChromosome() == null){
                merged.add(previousRegion);
                previousRegion = r;
            }else{//both not null
                if (r.getChromosome().equalsIgnoreCase(previousRegion.getChromosome())
                        && previousRegion.getEndPos() >= r.getStartPos()){
                    if (previousRegion.getEndPos() < r.getEndPos()){
                        previousRegion.setEndPos(r.getEndPos());
                    }
                }else{
                    merged.add(previousRegion);
                    previousRegion = r;
                }
            }
        }
        merged.add(previousRegion);
        regions.clear();
        regions.addAll(merged);
    }
}//end of RegionSummary class
