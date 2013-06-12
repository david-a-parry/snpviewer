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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author david
 */
public class RemoveSamplesInterfaceController implements Initializable {

    
    @FXML
    AnchorPane anchorPane;
    @FXML
    Pane affectedPane;
    @FXML
    VBox affVbox;
    @FXML
    VBox unVbox;
    @FXML
    Pane unaffectedPane;
    @FXML
    Button removeButton;
    @FXML
    Button cancelButton;
    
    List<Integer> samplesToRemove = new ArrayList<>();//indices of samples from list to remove
    List<CheckBox> checkBoxes = new ArrayList<>();
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        removeButton.setOnAction(new EventHandler<ActionEvent>(){
            @Override
            public void handle(ActionEvent e){
                Platform.runLater(new Runnable(){
                    @Override
                    public void run(){
                        samplesToRemove.clear();
                        for (int i = 0; i < checkBoxes.size(); i++){
                            if (checkBoxes.get(i).isSelected()){
                                samplesToRemove.add(i);
                            }
                        }
                        Stage stage = (Stage) removeButton.getScene().getWindow();
                        stage.close();
                    }
                });
            }
        });
        cancelButton.setOnAction(new EventHandler<ActionEvent>(){
            @Override
            public void handle(ActionEvent e){
                Platform.runLater(new Runnable(){
                    @Override
                    public void run(){
                        samplesToRemove.clear();
                        Stage stage = (Stage) cancelButton.getScene().getWindow();
                        stage.close();
                    }
                });
            }
        });
        
    }
    public void setSamples(List<SnpFile> affected, List<SnpFile> unaffected){
        
        for (SnpFile s: affected){
            CheckBox sCheck = new CheckBox();
            if (s.getSampleName() != null && !s.getSampleName().isEmpty()){
                sCheck.setText(s.getSampleName());
            }else{
                sCheck.setText(s.inputFile.getName());
            }
            sCheck.setVisible(true);
            affVbox.getChildren().add(sCheck);
            checkBoxes.add(sCheck);
        }
        for (SnpFile s: unaffected){
            CheckBox sCheck = new CheckBox();
            if (s.getSampleName() != null && !s.getSampleName().isEmpty()){
                sCheck.setText(s.getSampleName());
            }else{
                sCheck.setText(s.inputFile.getName());
            }
            sCheck.setVisible(true);
            unVbox.getChildren().add(sCheck);
            checkBoxes.add(sCheck);
        }
    }
    
    public List getSamplesToRemove(){
        return samplesToRemove;
    }
}