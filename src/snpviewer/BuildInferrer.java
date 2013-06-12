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
 * rather cludgy and limited class to determine genome build version
 * based on the coordinates of a few SNPs
 */
package snpviewer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author davidparry
 */
public class BuildInferrer {
    HashMap<String, HashMap<String, Integer>> sentinalSnps = new HashMap<>(); /*hash of build strings
     * containing hash of SNP IDs vs position. Uses a set of SNPs that allow
     * you to distinguish between hg19 and hg18 builds
     */
    BuildInferrer(){
        populateSentinalSnps();
    }
    
    public String inferBuild(SnpFile snpfile){
        List<SnpFile.SnpLine> lines = snpfile.getSnpsInRegion("22", 14435171, 16061342);
        lines.addAll(snpfile.getSnpsInRegion("22", 46249167, 47879110));
        String build = null;
        for (SnpFile.SnpLine s: lines){
            for (String buildKey: sentinalSnps.keySet()){
                if (sentinalSnps.get(buildKey).containsKey(s.getId().toLowerCase())){
                    Integer coord = sentinalSnps.get(buildKey).get(s.getId().toLowerCase());
                    if (coord.equals(s.getPosition())){
                        if (build != null && ! build.equals(buildKey)){
                            return null;//our sentinal shouldn't match more than one build - can't determine build
                        }else{
                            build = buildKey;
                        }
                    }
                }
            }
        }
        return build;
    }
    
    
    private void populateSentinalSnps(){
        HashMap<String, Integer> hg19 = new HashMap<>();
        HashMap<String, Integer> hg18 = new HashMap<>();
        ArrayList<Integer> hg19Positions = new ArrayList<>(Arrays.asList(
                16055171, 16055207, 16061342, 47870503, 47874672, 47879110));
        ArrayList<Integer> hg18Positions = new ArrayList<>(Arrays.asList(
                14435171, 14435207, 14441342, 46249167, 46253336, 46257774));
        ArrayList<String> ids = new ArrayList<>(Arrays.asList(
                "rs12628452", "rs7291810", "rs10154759", "rs2337501", 
                "rs4568046", "rs8139889"));
        for (int i = 0; i < ids.size(); i++){
            hg18.put(ids.get(i), hg18Positions.get(i));
            hg19.put(ids.get(i), hg19Positions.get(i));
        }
        sentinalSnps.put("hg19", hg19);
        sentinalSnps.put("hg18", hg18);
        /*hg19 
         * 16055171	rs12628452
         * 16055207	rs7291810
         * 16061342	rs10154759
         * 47870503	rs2337501
         * 47874672	rs4568046
         * 47879110	rs8139889
         * 
         * /hg18
         * 14435171	RS12628452
         * 14435207	RS7291810
         * 14441342	RS10154759
         * 46249167	RS2337501
         * 46253336	RS4568046
         * 46257774	RS8139889
         * 
         */
        
    }
}
