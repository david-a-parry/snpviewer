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

import java.io.File;
import java.io.IOException;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javax.imageio.ImageIO;

/**
 *
 * @author davidparry
 */
public class DrawPaneToPng extends Service {
    private Image image;
    private File temp; 
    DrawPaneToPng(Image img){
        image = img;
    }
    
    @Override
    protected  Task createTask() {
        return new Task() {
            @Override
            protected Object call() throws Exception {
                try{
                    temp = File.createTempFile("tempchrompicture", ".tmp");
                }catch (Exception ex){
                    return null;
                
                }
                if (image == null){
                    return null;
                }
                try{
                    ImageIO.write(SwingFXUtils.fromFXImage(image, null),
                            "png", temp);
                } catch (IOException ex) { 
                    System.out.println(ex.getMessage());
                    return null;
                }
                return true;
            }
        };
    }//end of createTask
    public File getImageFile(){
        return temp;
    }
}
