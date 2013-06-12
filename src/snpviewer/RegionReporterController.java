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

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author davidparry
 */


public class RegionReporterController  {
    /**
     * Initializes the controller class.
     */
    @FXML
    AnchorPane mainPane;
    @FXML
    Button okButton;
    @FXML
    Label idLabel;
    @FXML
    TextField idField;
    @FXML
    Label coordLabel;
    @FXML
    TextField coordField;
    
    
    public void initialize() {
    }    
    public void okButtonFired(ActionEvent ev){
        Platform.runLater(new Runnable(){
            @Override
            public void run(){
                Stage stage = (Stage) okButton.getScene().getWindow();
                stage.close();
            }
        });
    }
    public void setIds(String ids){
        idField.setText(ids);
    }
    public void setCoordinates(String coord){
        coordField.setText(coord);
    }
}
