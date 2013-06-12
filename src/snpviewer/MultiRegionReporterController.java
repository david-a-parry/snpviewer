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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author david
 */
public class MultiRegionReporterController implements Initializable {

   @FXML
   TableView<RegionSummary> snpTable;
   @FXML
   TableColumn chromCol;
   @FXML
   TableColumn startPosCol;
   @FXML
   TableColumn endPosCol;
   @FXML
   TableColumn startIdCol;
   @FXML
   TableColumn endIdCol;
   @FXML
   TableColumn lengthCol;
   @FXML
   Button okButton;
   @FXML 
   Label summaryLabel;
   @FXML
   TextArea snpTextSummary;
   
   NumberFormat nf = NumberFormat.getNumberInstance();
   ChromComparator chromCompare = new ChromComparator();
   
   private final ObservableList<RegionSummary> data = FXCollections.observableArrayList();
   
   @Override
    public void initialize(URL url, ResourceBundle rb) {
        chromCol.setCellValueFactory(new 
                PropertyValueFactory<RegionSummary, String>("chromosome"));
        startPosCol.setCellValueFactory(new 
                PropertyValueFactory<RegionSummary, Integer>("startPos"));
        endPosCol.setCellValueFactory(new 
                PropertyValueFactory<RegionSummary, Integer>("endPos"));
        startIdCol.setCellValueFactory(new 
                PropertyValueFactory<RegionSummary, String>("startId"));
        endIdCol.setCellValueFactory(new 
                PropertyValueFactory<RegionSummary, String>("endId"));
        lengthCol.setCellValueFactory(new 
                PropertyValueFactory<RegionSummary, String>("length"));
        chromCol.setComparator(chromCompare);
        snpTable.getSortOrder().add(chromCol);
        snpTable.getSortOrder().add(startPosCol);
        snpTable.getSortOrder().add(endPosCol);    
        snpTable.getSortOrder().add(lengthCol);
        
        snpTable.getSelectionModel().setCellSelectionEnabled(true);
        snpTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        

        
        MenuItem item = new MenuItem("Copy");
        item.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                ObservableList<TablePosition> posList = snpTable.getSelectionModel().getSelectedCells();
                int old_r = -1;
                StringBuilder clipboardString = new StringBuilder();
                for (TablePosition p : posList) {
                    int r = p.getRow();
                    int c = p.getColumn();
                    Object cell = snpTable.getColumns().get(c).getCellData(r);
                    if (cell == null)
                        cell = "";
                    if (old_r == r)
                        clipboardString.append('\t');
                    else if (old_r != -1)
                        clipboardString.append('\n');
                    clipboardString.append(cell);
                    old_r = r;
                }
                final ClipboardContent content = new ClipboardContent();
                content.putString(clipboardString.toString());
                Clipboard.getSystemClipboard().setContent(content);
            }
        });
        ContextMenu menu = new ContextMenu();
        menu.getItems().add(item);
        snpTable.setContextMenu(menu);
        
        
        okButton.setOnAction(new EventHandler<ActionEvent>(){
            @Override
            public void handle(ActionEvent e){
                Platform.runLater(new Runnable(){
                    @Override
                    public void run(){
                        Stage stage = (Stage) okButton.getScene().getWindow();
                        stage.close();
                    }
                });
            }
        });
        
    }
    
    public void displayData(ArrayList<RegionSummary> regions){
        int totalRegions = 0;
        double totalLength = 0;
        StringBuilder summaryString = new StringBuilder();
        for (RegionSummary r: regions){
            StringBuilder tempString = new StringBuilder();
            data.add(r);
            snpTable.setItems(data);
            tempString.append("chr").append(r.getCoordinateString());
            while (tempString.length() < 30){
                tempString.append(" ");
            }
            tempString.append("(").append(r.getIdLine()).append(")");
            while (tempString.length() < 55){
                tempString.append(" ");
            }
            tempString.append(nf.format(r.getLength())).append(" bp\n");
            summaryString.append(tempString);
            totalRegions++;         
            totalLength += r.getLength();
        }
        snpTextSummary.setText(summaryString.toString());
        totalLength /= 1000000;
        summaryLabel.setText(totalRegions + " regions/" + nf.format(totalLength) + " Mb");
    }
}
