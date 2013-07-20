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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 *
 * @author davidparry
 */
public class RegionFinder extends Service{
    LinkedHashSet<String> chromosomes = new LinkedHashSet<>();
    ArrayList<SnpFile> aff;//set of affected snp files
    ArrayList<SnpFile> un;//set of unaffected snp files
    double hetCutoff ; //e.g. 0.05 for 5 snps in 100 as cut-off for a hom region
    double disconcordantCutoff ;//allow 1 in 100 dischordant calls(?)
    int windowSize;
    double regionLength;//size in bp for region to be considered significant in an individual
    int minRegionToReport ; //min size after refining region with other samples
    int minRunLengthToReport ; //min no. SNPs after refining region with other samples;
    boolean concordant;
    int refineWindow;//when refining the edges of regions use this window size
    double refineCutoff;//use this fraction of hets in refineWindow to refine regions
    String genome;
    ChromosomeLength chromLengths;
    /*Constructors
     * require chroms (can be null for all chroms) list of affFiles, list of unFiles 
     * default constructor uses window size of 100 and region length of 1 Mb and
     * will check for concordance
     */
    RegionFinder (Set<String> chroms, ArrayList<SnpFile> affFiles, ArrayList<SnpFile> unFiles){
        this(chroms, affFiles, unFiles, 100, 1, true);    
    }
    
    //takes window size as an int, takes region length as Mb double value - converts to bp, 
    RegionFinder (Set<String> chroms, ArrayList<SnpFile> affFiles, ArrayList<SnpFile> unFiles, int w, double r){
        this(chroms, affFiles, unFiles, w, r, true);
    }
    
    //uses con to determine whether to check concordance
    RegionFinder (Set<String> chroms, ArrayList<SnpFile> affFiles, ArrayList<SnpFile> unFiles, boolean con){
        this(chroms, affFiles, unFiles, 100, 1, con);
    }
    
    RegionFinder (Set<String> chroms, ArrayList<SnpFile> affFiles, 
            ArrayList<SnpFile> unFiles, int w, double r, boolean con){
        this(chroms, affFiles, unFiles, w, r, con, w/10, 0.1, 10000, 25, 0.05, 0.01);
    }
    
    RegionFinder (Set<String> chroms, ArrayList<SnpFile> affFiles, ArrayList<SnpFile> 
            unFiles, int w, double r, boolean con, int refWind, double refCut,
            int minReportSize, int minReportRun, double het, double dischord){
        aff = affFiles;
        un = unFiles;
        if (chroms == null){
            for (SnpFile s: aff){
                Set c = s.chromFiles.keySet();
                chromosomes.addAll(c);
                String build = s.buildVersion;
                if (genome.isEmpty()){//SnpViewer class should have already have ensured all are same genome
                    genome = build;
                }
            }
        }else{
            chromosomes.addAll(chroms);
        }
        windowSize = Math.max(w, 10);
        regionLength = r * 1000000;
        concordant = con;
        refineWindow = refWind;
        refineCutoff = refCut;
        minRegionToReport = minReportSize;
        minRunLengthToReport = minReportRun;
        hetCutoff = het;
        disconcordantCutoff = dischord;
        
        try{
            chromLengths = new ChromosomeLength(genome);
        }catch(Exception ex){
            chromLengths = new ChromosomeLength();            
        }
    }
    //end of constructors

    
    @Override
    protected Task<ArrayList<RegionSummary>> createTask() {
        /*for each chromosome iterate through our affected SnpFiles (aff) and 
         * for each file identify runs of homozygosity. Compare and identify any
         * overlapping regions between affecteds. For shared regions check 
         * for absence in unaffected samples. 
         */
        return new Task<ArrayList<RegionSummary>>(){
            @Override
            protected ArrayList<RegionSummary> call(){   
                ArrayList<RegionSummary> regionsToReturn = new ArrayList<>();
                double progress = 0;
                double target = chromosomes.size() * (aff.size() + un.size());
                updateProgress(progress, target);
                updateTitle("Finding Homozygous Regions");
                chroms:
                for (String c: chromosomes){
                    if (isCancelled()){
                        updateMessage("Cancelled");
                        return null;
                    }
                    updateMessage("Processing chromosome " + c);
                    HashMap<SnpFile, ArrayList<SnpFile.SnpLine>> affFileLines = new HashMap<>();
                    HashMap<SnpFile, ArrayList<RegionSummary>> affFileRegions = new HashMap<>();
                    for (SnpFile a: aff){
                        if (!a.chromFiles.containsKey(c)){
                            /*we need all affected files 
                             * to contain a chromosome or by definition 
                             * they can't have any shared regions
                             */
                            progress += aff.size() + un.size();
                            updateProgress(progress, target);
                            continue chroms;
                        }
                    }
                    for (SnpFile a: aff){
                        if (isCancelled()){
                           updateMessage("Cancelled");
                            return null;
                        }
                        ArrayList<SnpFile.SnpLine> lines = a.readChromFile(c);
                        if (lines == null){/*this should have been taken care of by
                         * the first loop through our SnpFiles, but just to be sure...
                         */
                            progress += aff.size() + un.size();
                            updateProgress(progress, target);
                            continue chroms;
                        }
                        
                        ArrayList<RegionSummary> regions = findRegions(lines);
                        ArrayList<RegionSummary> checkedRegions = new ArrayList<>();
                        for (RegionSummary r : regions){
                            if (r.getLength() >= regionLength){
                                checkedRegions.add(r);
                            }
                        }
                        affFileRegions.put(a, checkedRegions);
                        affFileLines.put(a, lines);//reference subsets of these snps when
                                                   //checking concordance
                    }
                    /* compare affFileRegions to find overlapping regions
                     * if concordant is true than check for concordance on overlap
                     * by reading affFileLines. 
                     * 
                     * we set our sharedRegions up first by copying the regions from our
                     * first file.  We'll strip out and modify regions to only represent
                     * overlaps by iterating over our the rest of our affected files
                     */
                    SnpFile firstAff = aff.get(0);
                    ArrayList<RegionSummary> sharedRegions = affFileRegions.get(firstAff);
                    updateProgress(++progress, target);
                    if (aff.size() > 1){
                        //find all overlaps in each other file
                        for (int i = 1; i < aff.size(); i++ ){
                            if (isCancelled()){
                               updateMessage("Cancelled");
                                return null;
                            }
                            SnpFile nextAff = aff.get(i);
                            sharedRegions = findOverlaps(sharedRegions, affFileRegions.get(nextAff));
                            if (sharedRegions == null){
                                progress += aff.size() - (i + 1); 
                                updateProgress(progress, target);
                                break;
                            }else if (concordant){
                                ArrayList<Integer> indicesToRemove = new ArrayList<>();
                                for (int j = sharedRegions.size() -1; j >= 0; j--){
                                    if (isCancelled()){
                                       updateMessage("Cancelled");
                                        return null;
                                    }
                                    List<SnpFile.SnpLine> firstAffLines = 
                                            getSnpsByCoordinate(affFileLines.get(firstAff), 
                                            sharedRegions.get(j).getStartPos(), 
                                            sharedRegions.get(j).getEndPos());
                                    List<SnpFile.SnpLine> nextAffLines = 
                                            getSnpsByCoordinate(affFileLines.get(nextAff), 
                                            sharedRegions.get(j).getStartPos(), 
                                            sharedRegions.get(j).getEndPos());
                                    if (firstAffLines != null && nextAffLines != null){
                                        if (! isConcordant(firstAffLines, nextAffLines)){
                                            indicesToRemove.add(j);
                                        }
                                    }
                                }
                                for (int j = 0; j < indicesToRemove.size(); j++){
                                    sharedRegions.remove((int) indicesToRemove.get(j));
                                }
                            }
                            updateProgress(++progress, target);
                        }
                    }
                    if (sharedRegions != null && !sharedRegions.isEmpty()){
                        int unCounter = 0;
                        for (SnpFile u: un){
                            if (isCancelled()){
                               updateMessage("Cancelled");
                                return null;
                            }
                            Collections.sort(sharedRegions);
                            ArrayList<SnpFile.SnpLine> lines = u.readChromFile(c);
                            if (lines == null){
                                progress += un.size() - unCounter;
                                continue;
                            }
                            ArrayList<RegionSummary> uregions = findRegions(lines);
                            ArrayList<RegionSummary> unshared = 
                                    findOverlaps(sharedRegions, uregions);
                            for (RegionSummary unshare: unshared){
                                if (concordant){
                                    List<SnpFile.SnpLine> firstAffLines = 
                                                getSnpsByCoordinate(affFileLines.get(firstAff), 
                                                unshare.getStartPos(), unshare.getEndPos());
                                    List<SnpFile.SnpLine> unLines = 
                                                getSnpsByCoordinate(lines, 
                                                unshare.getStartPos(), unshare.getEndPos());
                                    if (firstAffLines != null && unLines != null){
                                        if (isConcordant(firstAffLines, unLines)){
                                            subtractRegion(sharedRegions, unshare);
                                        }
                                    }
                                }else{
                                    subtractRegion(sharedRegions, unshare);
                                }
                            }
                            updateProgress(++progress, target);
                            unCounter++;
                        }
                    }else{
                        progress += un.size();
                        updateProgress(progress, target);
                    }
                    if (sharedRegions != null){
                        for (RegionSummary r: sharedRegions){
                            if (isCancelled()){
                               updateMessage("Cancelled");
                                return null;
                            }
                            if (r.getLength() >= minRegionToReport){
                                int maxCalls = 0;
                                for (SnpFile a: affFileLines.keySet()){
                                    int calls = 0;
                                    List<SnpFile.SnpLine> rLines = 
                                            getSnpsByCoordinate(affFileLines.get(a), 
                                            r.getStartPos(), r.getEndPos());
                                    for (SnpFile.SnpLine s: rLines){
                                        if (! s.getCall().equalsIgnoreCase("NoCall")){
                                            calls++;
                                        }
                                    }
                                    maxCalls = Math.max(maxCalls, calls);
                                }
                                if (maxCalls >= minRunLengthToReport){
                                    r.setChromosome(c);
                                    regionsToReturn.add(r);
                                }
                            }
                        }
                    }
                }
                updateProgress(target, target);
                Collections.sort(regionsToReturn);
                joinCloseRegions(regionsToReturn);
                return regionsToReturn;
            }
        };
    }//end of createTask
    
    /* joins regions closer than half the length of regionLength
     * regs must be sorted!
     * 
     */
    private void joinCloseRegions(ArrayList<RegionSummary> regs){
        if (regs.size() < 2){
            return;
        }
        ArrayList<RegionSummary> joinedRegions = new ArrayList<>();
        Iterator<RegionSummary> regit = regs.iterator();
        RegionSummary prevRegion = regit.next();
        while (regit.hasNext()){
            RegionSummary region = regit.next();
            if (region.getChromosome().equalsIgnoreCase(prevRegion.getChromosome()) && 
                    region.getStartPos() - prevRegion.getEndPos() <= regionLength/2){
                prevRegion.setEndPos(region.getEndPos());
                prevRegion.setEndId(region.getEndId());
            }else{
                joinedRegions.add(prevRegion);
                prevRegion = region;
            }
        }
        joinedRegions.add(prevRegion);
        regs.clear();
        regs.addAll(joinedRegions);
    }
    
    private void subtractRegion(ArrayList<RegionSummary> regs,
            RegionSummary subtract){
        final int subStart = subtract.getStartPos();
        final int subEnd = subtract.getEndPos();
        final String subStartId = subtract.getStartId();
        final String subEndId = subtract.getEndId();
        int i = binSearchRegionSummariesByPosition(regs, subStart, subEnd);
        if (i > -1){
            ArrayList<Integer> indicesToRemove = new ArrayList<>();
            ArrayList<RegionSummary> replacementRegions = new ArrayList<>();
            int listStart = regs.get(i).getStartPos();
            int listEnd = regs.get(i).getEndPos();
            String listStartId = regs.get(i).getStartId();
            String listEndId = regs.get(i).getEndId();
            /*mark region for removal, if subtract covers entire reg, just remove.
             * Otherwise add replacement region(s) for uncovered portion(s)
             */
            indicesToRemove.add(i);
            if (subStart > listStart || subEnd < listEnd){
                if (subStart > listStart){
                    replacementRegions.add(new RegionSummary(listStart, subStart, 0, 0, listStartId, subEndId));
                }
                if (subEnd < listEnd){
                    replacementRegions.add(new RegionSummary(subEnd, listEnd, 0, 0, subStartId, listEndId));
                }
            }
            for (int j = i - 1; j >= 0; j--) {
                if (getOverlapCoordinates(regs.get(j), subtract) != null){
                    indicesToRemove.add(j);
                    listStart = regs.get(j).getStartPos();
                    listEnd = regs.get(j).getEndPos();
                    listStartId = regs.get(j).getStartId();
                    listEndId = regs.get(j).getEndId();
                    if (subStart > listStart || subEnd < listEnd){
                        if (subStart > listStart){
                            replacementRegions.add(new RegionSummary(listStart, subStart, 0, 0, listStartId, subStartId));
                        }
                        if (subEnd < listEnd){
                            replacementRegions.add(new RegionSummary(subEnd, listEnd, 0, 0, subEndId, listEndId));
                        }
                    }
                }else{
                    break;
                }
            }
            for (int j = i + 1; j < regs.size(); j++) {
                if (getOverlapCoordinates(regs.get(j), subtract) != null){
                    indicesToRemove.add(j);
                    listStart = regs.get(j).getStartPos();
                    listEnd = regs.get(j).getEndPos();
                    listStartId = regs.get(j).getStartId();
                    listEndId = regs.get(j).getEndId();
                    if (subStart > listStart || subEnd < listEnd){
                        if (subStart > listStart){
                            replacementRegions.add(new RegionSummary(listStart, subStart, 0, 0, listStartId, subStartId));
                        }
                        if (subEnd < listEnd){
                            replacementRegions.add(new RegionSummary(subEnd, listEnd, 0, 0, subEndId, listEndId));
                        }
                    }
                }else{
                    break;
                }
            }
           if (!indicesToRemove.isEmpty()){
               Collections.sort(indicesToRemove, Collections.reverseOrder());
               for (int r: indicesToRemove){
                   regs.remove(r);
               }
           }
           if (!replacementRegions.isEmpty()){
               regs.addAll(replacementRegions);
               
           }
        }
    }
    
    private boolean isConcordant(List<SnpFile.SnpLine> r1, List<SnpFile.SnpLine> r2){
        Integer matchingSnpCount = 0;
        Integer disconcordantCount = 0;
        Iterator<SnpFile.SnpLine> r1iter = r1.iterator();
        Iterator<SnpFile.SnpLine> r2iter = r2.iterator();
        if (!r2iter.hasNext()){
            //assume dischordant(?)
            return false;
        }
        SnpFile.SnpLine s2 = r2iter.next();
        while (r1iter.hasNext()){
            SnpFile.SnpLine s1 = r1iter.next();
            while (s2.getPosition() < s1.getPosition() && r2iter.hasNext()){
                s2 = r2iter.next();
            }
            if (s1.getPosition() == s2.getPosition() 
                    && s1.getId().equalsIgnoreCase(s2.getId())){
                /*consider het calls as errors rather than dischordant.
                 * Call only dischordant if one is AA and other is BB
                 */
                if (s1.getCall().matches("(\\w)\\1")
                        && s2.getCall().matches("(\\w)\\1")){
                    matchingSnpCount++;
                    if (!s1.getCall().equalsIgnoreCase(s2.getCall())){
                        disconcordantCount++;
                    }
                }
            }
        }
                
        if (matchingSnpCount < 1){
            // assume disconcordant?
            return false;
        }
        if (disconcordantCount.doubleValue()/matchingSnpCount.doubleValue()
                < disconcordantCutoff){
            return true;
        }else{
            return false;
        }
    }
    
    /*retrieve matching SNP lines using coordinates 
     * rather than indices
     */
    private List<SnpFile.SnpLine>  getSnpsByCoordinate 
            (ArrayList<SnpFile.SnpLine> lines, int start, int end){
        if (start > end){
            return null;
        }
        int u = lines.size() - 1;
        int l = 0;
        while (l <= u){
            int i = (u + l)/2;
            int linePos = lines.get(i).getPosition();
            if (linePos > end){
                u = i - 1;
            }else if (linePos < start){
                l = i + 1;
            }else{//intersects
                int startIndex = 0;
                int endIndex = lines.size() -1;
                for (int j = i; j >= 0; j--){
                    if (lines.get(j).getPosition() <= start){
                        startIndex = j;
                        break;
                    }
                }
                for (int j = i; j < lines.size(); j++){
                    if (lines.get(j).getPosition() >= end){
                        endIndex = j;
                        break;
                    }
                }
                return lines.subList(startIndex, endIndex + 1);                
            }
        }//not found
        return null;
    }
    
    private ArrayList<RegionSummary> findOverlaps(ArrayList<RegionSummary> rList1, 
            ArrayList<RegionSummary> rList2){
        ArrayList<RegionSummary> overlaps = new ArrayList<>();
                
        for (RegionSummary r1: rList1){
                       
            ArrayList<RegionSummary> o = searchRegionSummaryByPosition(r1, rList2);
            if (o != null){
                overlaps.addAll(o);
            }
        }
        return overlaps;
    }
    
    private int binSearchRegionSummariesByPosition(ArrayList<RegionSummary> rList, 
            final int start, final int end){
        int u = rList.size() - 1;
        int l = 0;
        while (l <= u){
            int i = (u + l)/2;
            int listStart = rList.get(i).getStartPos();
            int listEnd = rList.get(i).getEndPos();
            if (listStart > end){
                u = i - 1;
            }else if (listEnd < start){
                l = i + 1;
            }else{//intersection
                return i;
            }
        }//not found
        return -1;
    }
    
    private ArrayList<RegionSummary> searchRegionSummaryByPosition(RegionSummary r, 
            ArrayList<RegionSummary> rList){
        ArrayList<RegionSummary> overlap = new ArrayList<>();
        int regionStart = r.getStartPos();
        int regionEnd = r.getEndPos();
        int i = binSearchRegionSummariesByPosition(rList, regionStart, regionEnd);
        if (i > -1){
            int listEnd = rList.get(i).getEndPos();
            int start;// = Math.max(listStart, regionStart);
            int end;// = Math.min(listEnd, regionEnd);
            String endId;
            String startId;
            if (rList.get(i).getStartPos() > regionStart){
                start = rList.get(i).getStartPos();
                startId = rList.get(i).getStartId();
            }else{
                start = regionStart;
                startId = r.getStartId();
            }
            if (rList.get(i).getEndPos() < regionEnd){
                end = rList.get(i).getEndPos();
                endId = rList.get(i).getEndId();
            }else{
                end = regionEnd;
                endId = r.getEndId();
            }
            
            overlap.add(new RegionSummary(start, end, 0, 0, startId, endId));
            for (int j = i - 1; j >= 0; j--) {
                RegionSummary intersect = (getOverlapCoordinates(r, rList.get(j)));
                if (intersect != null) {
                    overlap.add(intersect);
                } else {
                    break;
                }
            }
            for (int j = i + 1; j < rList.size(); j++) {
                RegionSummary intersect = (getOverlapCoordinates(r, rList.get(j)));
                if (intersect != null) {
                    overlap.add(intersect);
                } else {
                    break;
                }
            }
            return overlap;
        }else{//not found
            return null;
        }
    }
    
    /*returns null if regions don't overlap, otherwise returns overlapping
     * RegionSummary with indexes abitrarily set to 0
     */
    private RegionSummary getOverlapCoordinates(RegionSummary r1, RegionSummary r2){
        if (r1.getStartPos() >= r2.getStartPos() 
                && r1.getStartPos() <= r2.getEndPos()){
           return getOverlap(r1, r2);
        }else if (r1.getEndPos() <= r2.getEndPos() 
                && r1.getEndPos() >= r2.getStartPos()){
           return getOverlap(r1, r2);  
        }else if (r2.getStartPos() >= r1.getStartPos() 
                && r2.getStartPos() <= r1.getEndPos()){
            return getOverlap(r1, r2);
        }else if (r2.getEndPos() <= r1.getEndPos() 
                && r2.getEndPos() >= r1.getStartPos()){
            return getOverlap(r1, r2);
        }else{
            return null;
        }
    }
    
    //works for regions we've already determined overlap, does not check for overlap
    private RegionSummary getOverlap(RegionSummary r1, RegionSummary r2){
        int start;
        int end;
        String startId;
        String endId;
        if (r1.getStartPos() >= r2.getStartPos()){
            start = r1.getStartPos();
            startId = r1.getStartId();
        }else{
            start = r2.getStartPos();
            startId = r2.getStartId();
        }
        if (r1.getEndPos() <= r2.getEndPos()){
            end = r1.getEndPos();
            endId = r1.getEndId();
        }else{
            end = r2.getEndPos();
            endId = r2.getEndId();
        }
        return new RegionSummary(start, end, 0, 0, startId, endId);
    }
    
    /*identify all putative homozygous regions for this set of SnpLines 
     * (i.e. for one chrom in one file). Return a hashmap giving values for 
     * start and end indexes and start and end chromosomal positions 
     * (startPos, endPos, startIndex and endIndex).
     */
    private ArrayList<RegionSummary> findRegions(ArrayList<SnpFile.SnpLine> l){
        ArrayList<RegionSummary> homRegions = new ArrayList<>();
        int i;
        for (i = 0; i < l.size() - windowSize; i += windowSize/2){            
            List<SnpFile.SnpLine> window = l.subList(i, i + windowSize);
            /*remove no calls and replace. Use the no. removed to move
             * i accordingly but only after recording the startIndex in reg
             */
            int removed = removeNoCalls(l, window, i + windowSize - 1, false);
            if (isHomozygous(window)){
                RegionSummary reg = new RegionSummary();
                reg.setStartPos(window.get(0).getPosition());
                reg.setEndPos(window.get(window.size()-1).getPosition());
                reg.setStartIndex(i);
                reg.setStartId(window.get(0).getId());
                reg.setEndId(window.get(window.size()-1).getId());
                i += removed;
                reg.setEndIndex(i + removed + window.size());
                homRegions.add(reg);
            }
        }
        //collect remainder
        if (i < l.size()){
            List<SnpFile.SnpLine> window = l.subList(i, l.size());
            int removed = removeNoCalls(l, window, i + windowSize - 1, false);
            if (isHomozygous(window)){
                RegionSummary reg = new RegionSummary();
                reg.setStartPos(window.get(0).getPosition());
                reg.setEndPos(window.get(window.size()-1).getPosition());
                reg.setStartId(window.get(0).getId());
                reg.setEndId(window.get(window.size()-1).getId());
                reg.setStartIndex(i);
                reg.setEndIndex(l.size() -1 );
                homRegions.add(reg);
            }
        }
        homRegions = mergeIntraChromRegions(homRegions);
        refineRegions(homRegions, l);
        return homRegions;
    }
    
    /*look at edges of regions and set start and end points at the edge
     * of a clearly defined heterozygous run
     */
    private void  refineRegions(ArrayList<RegionSummary> regions, ArrayList<SnpFile.SnpLine> l){
        for (RegionSummary r: regions){
            //check start
            int startIndex = r.getStartIndex() + refineWindow;//start one refineWindow into the region
            for (; startIndex >= 0; startIndex--){
                if (startIndex + refineWindow >= l.size()){
                    continue;
                }
                List<SnpFile.SnpLine> startRefine = l.subList(startIndex, startIndex + refineWindow);
                int removed = doNotRemoveNoCalls(l, startRefine, startIndex - 1, true);
                startIndex -= removed;
                if (startRefine.size() < 1){
                    r.setStartIndex(0);
                    r.setStartIndex(1);
                    r.setStartId(l.get(0).getId());
                    break;
                }
                ArrayList<String> refineCalls = getAllCalls(startRefine);
                double hets  = 0;
                double calls = 0;
                for (String rCall: refineCalls){
                    if (rCall.equalsIgnoreCase("AB")){
                        hets++;
                    }else if (!rCall.equalsIgnoreCase("NoCall")){
                        calls++;
                    }
                }
                if (hets/calls > refineCutoff){
                    int refineIndex = refineCalls.lastIndexOf("AB");
                    if (refineIndex == -1){
                        refineIndex = refineCalls.lastIndexOf("ab");
                    }
                    r.setStartIndex(startIndex + refineIndex);
                    r.setStartPos(l.get(startIndex + refineIndex).getPosition());
                    r.setStartId(l.get(r.getStartIndex()).getId());
                    break;
                }
            }
            if (startIndex <= 0){//we've hit the beginning and probably not refined
                r.setStartIndex(0);
                r.setStartPos(1);
                r.setStartId(l.get(0).getId());
            }
            //finished refining beginning 
            //check end
            int endIndex = Math.max(r.getEndIndex() - refineWindow, 0);
            for (; endIndex < l.size(); endIndex++){
                int subEnd = Math.min(endIndex + refineWindow + 1, l.size());
                List<SnpFile.SnpLine> endRefine = l.subList(endIndex, subEnd);
                doNotRemoveNoCalls(l, endRefine, endIndex + 1, false);
                if (endRefine.size() < 1){
                    r.setEndIndex(l.size() - 1);
                    String chromosome = l.get(0).getChromosome();
                    int chromLength = aff.get(0).chromLengths.get(chromosome);
                    r.setEndPos(chromLength);
                    r.setEndId(l.get(r.getEndIndex()).getId());
                }
                ArrayList<String> refineCalls = getAllCalls(endRefine);
                double hets = 0;
                double calls = 0;
                for (String rCall: refineCalls){
                    if (rCall.equalsIgnoreCase("AB")){
                        hets++;
                    }else if (!rCall.equalsIgnoreCase("NoCall")){
                        calls++;
                    }
                }
                if (hets/calls > refineCutoff){
                    int refineIndex = refineCalls.indexOf("AB");
                    if (refineIndex == -1){
                        refineIndex = refineCalls.indexOf("ab");
                    }
                    r.setEndIndex(endIndex + refineIndex );
                    r.setEndPos(l.get(r.getEndIndex()).getPosition());
                    r.setEndId(l.get(r.getEndIndex()).getId());
                    break;
                }
            }
            if (endIndex >= l.size()){//we've hit the end and probably not refined
                r.setEndIndex(l.size() - 1);
                String chromosome = l.get(0).getChromosome();
                int chromLength;
                try{
                    chromLength = chromLengths.getLength(chromosome);
                }catch (Exception ex){
                    chromLength = 999999999;
                }
                r.setEndPos(chromLength);
                r.setEndId(l.get(r.getEndIndex()).getId());
            }
        }
    }

    //for a list of SnpLines return an array of call codes
    private ArrayList<String> getAllCalls(List<SnpFile.SnpLine> snps){
        ArrayList<String> calls = new ArrayList<>();
        for (SnpFile.SnpLine s: snps){
            calls.add(s.getCall());
        }
        return calls;
    }
    
    /*merge overlapping regions into single regions from single sample
     * our input should already be sorted in coordinate order
     */
    private ArrayList<RegionSummary> mergeIntraChromRegions(ArrayList<RegionSummary> regions){
        RegionSummary previousRegion = new RegionSummary();
        ArrayList<RegionSummary> merged = new ArrayList<>();
        for (RegionSummary r: regions){
            if (previousRegion.isEmpty()){
                previousRegion = r;
            }else{
                if (previousRegion.getEndIndex() >= r.getStartIndex()){
                    if (previousRegion.getEndIndex() < r.getEndIndex()){
                        previousRegion.setEndIndex(r.getEndIndex());
                        previousRegion.setEndPos(r.getEndPos());
                        previousRegion.setEndId(r.getEndId());
                    }
                }else{
                    merged.add(previousRegion);
                    previousRegion = r;
                }
            }
        }
        merged.add(previousRegion);
                
        return merged;
    }
    /*don't removenocalls but add calls as replacements
     * 
     */
    private int doNotRemoveNoCalls(List<SnpFile.SnpLine> lines, 
            List<SnpFile.SnpLine> region, int nextIndex, boolean addToBeginning){
        int replacements_counter = 0;
        int originalSize = region.size();
        do{
            int replacements = 0;
            for (int i = 0; i < region.size(); i++){
                String call = region.get(i).getCall();
                if (call.equalsIgnoreCase("NoCall")){
                    replacements++;
                }
            }
            /*if we remove from the end of the list 
             * we won't affect the indices
             * of items we want to remove
             */
            
            if (addToBeginning){
                for (int i = 0; i < replacements - replacements_counter
                        && nextIndex >= 0; i++){
                    region.add(0, lines.get(nextIndex--));
                }
            }else{
                for (int i = 0; i < replacements - replacements_counter
                        && nextIndex < lines.size(); i++){
                        region.add(lines.get(nextIndex++));
                }
            }
            replacements_counter = replacements;
        }while (region.size() - replacements_counter < originalSize);
        return replacements_counter;
    }
    
    /* remove snps from region with no genotype call and 
     * replace with next available line from lines
     */
    private int removeNoCalls(List<SnpFile.SnpLine> lines, 
            List<SnpFile.SnpLine> region, int nextIndex, boolean addToBeginning){
        int removed;
        int removed_counter = 0;
        do{
            ArrayList<Integer> indicesToRemove = new ArrayList<>();
            for (int i = 0; i < region.size(); i++){
                String call = region.get(i).getCall();
                if (call.equalsIgnoreCase("NoCall")){
                    indicesToRemove.add(i);
                }
            }
            /*if we remove from the end of the list 
             * we won't affect the indices
             * of items we want to remove
             */
            Collections.sort(indicesToRemove, Collections.reverseOrder());
            for (int i: indicesToRemove){
                region.remove(i);
            }
            if (addToBeginning){
                for (int i = 0; i < indicesToRemove.size()
                        && nextIndex >= 0; i++){
                    region.add(0, lines.get(nextIndex--));
                }
            }else{
                for (int i = 0; i < indicesToRemove.size() 
                        && nextIndex < lines.size(); i++){
                        region.add(lines.get(nextIndex++));
                }
            }
            removed = indicesToRemove.size();
            removed_counter += removed;
        }while (removed > 0);
        return removed_counter;
    }
    
    /* calculate homozygosity on the basis of less than 5 hets in 
     * 100 calls
     */
    private boolean isHomozygous(List<SnpFile.SnpLine> r){
        int hets = 0;
        int calls = 0;
        for (SnpFile.SnpLine s: r){
            if (s.getCall().equalsIgnoreCase("AB")){
                hets++;
            }else if (!s.getCall().equalsIgnoreCase("NoCall")){
                calls++;
            }
        }
        if (calls == 0){
            return false;
        }
        double fractionHet = ((double)hets) / (double) calls; 
        if (fractionHet < hetCutoff){
            return true;
        }else{
            return false;
        }
    }
    
        
    
}//end of RegionFinder class