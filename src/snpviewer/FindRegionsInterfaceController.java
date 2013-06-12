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
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialogs;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;



/**
 * FXML Controller class
 *
 * @author david
 */
public class FindRegionsInterfaceController  implements Initializable {
    
    //settings pane
    @FXML
    TextField regionLengthField;
    @FXML
    TextField reportLengthField;
    @FXML
    TextField reportMinSnpsField;
    @FXML
    CheckBox concordantCheckBox;
    @FXML
    Button okButton;
    @FXML
    Button cancelButton;
    
    private String regionLength;
    private String reportLength;
    private String reportMinSnps;
    private boolean checkConcordance;
    private boolean cancelled = true;//assume true in case window closed
    
    
    //advanced pane
    @FXML
    Pane anchorPane;
    @FXML
     TextField windowSizeField;
    @FXML
     TextField hetToleranceField;
    @FXML
     TextField dischordToleranceField;
    @FXML
     TextField refineSizeField;
    @FXML
     TextField refineToleranceField;
    @FXML
     CheckBox autosomesOnlyCheckbox;
    
    private String window;
    private String hetTolerance;
    private String dischordTolerance;
    private String refineSize;
    private String refineTolerance;
    private boolean autosomesOnly = true;
    
    NumberFormat nf = NumberFormat.getNumberInstance();
    //NumberFormat intNf = NumberFormat.getIntegerInstance();
   
   
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        //when user clicks ok set all variables to the values set in text box
        okButton.setOnAction(new EventHandler<ActionEvent>(){
            @Override
            public void handle(ActionEvent e){
                Platform.runLater(new Runnable(){
                    @Override
                    public void run(){
                        boolean fieldsOk = allFieldsAreValid();
                    
                        if (!fieldsOk){
                            return;
                        }
                        cancelled = false;
                        regionLength = regionLengthField.getText();
                        reportLength = reportLengthField.getText();
                        reportMinSnps = reportMinSnpsField.getText();
                        checkConcordance = concordantCheckBox.isSelected();

                        window = windowSizeField.getText();
                        hetTolerance = hetToleranceField.getText();
                        dischordTolerance = dischordToleranceField.getText();
                        refineSize = refineSizeField.getText();
                        refineTolerance = refineToleranceField.getText();
                        autosomesOnly = autosomesOnlyCheckbox.isSelected();

                        Stage stage = (Stage) okButton.getScene().getWindow();
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
                        cancelled = true;
                        Stage stage = (Stage) cancelButton.getScene().getWindow();
                        stage.close();
                    }
                });
            }
        });
        
        //basic methods to encourage user to only type appropriate numbers
        regionLengthField.addEventFilter(KeyEvent.KEY_TYPED, checkDecimal());
        reportLengthField.addEventFilter(KeyEvent.KEY_TYPED, checkNumeric());
        reportMinSnpsField.addEventFilter(KeyEvent.KEY_TYPED, checkNumeric());
        windowSizeField.addEventFilter(KeyEvent.KEY_TYPED, checkNumeric());
        hetToleranceField.addEventFilter(KeyEvent.KEY_TYPED, checkDecimal());
        dischordToleranceField.addEventFilter(KeyEvent.KEY_TYPED, checkDecimal());
        refineSizeField.addEventFilter(KeyEvent.KEY_TYPED, checkNumeric());
        refineToleranceField.addEventFilter(KeyEvent.KEY_TYPED, checkDecimal());
        //ensure regionLength argument is valid
        
        Tooltip regionLengthTooltip = new Tooltip();
        regionLengthTooltip.setText("Enter a value in Mb between 0.1 and 50.0");
        regionLengthField.setTooltip(regionLengthTooltip);
        regionLengthField.focusedProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> observable,
          Boolean oldValue, Boolean newValue ){
                if (!regionLengthField.isFocused()){
                    checkDoubleTextField(regionLengthField, 
                            "Minimum Region Size Per Sample", 0.1, 50.0, "1.0", true);
                }
            }
        });
        
        Tooltip reportLengthTooltip = new Tooltip();
        reportLengthTooltip.setText("Enter a value in bp between 1,000 and 50,000,000");
        reportLengthField.setTooltip(reportLengthTooltip);
        reportLengthField.focusedProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> observable,
          Boolean oldValue, Boolean newValue ){
                if (!reportLengthField.isFocused()){
                    checkIntegerTextField(reportLengthField, 
                            "Only Report Regions Longer Than...", 1000, 50000000, 
                            "10000", true);
                }
            }
        });
        
        Tooltip reportMinSnpsTooltip = new Tooltip();
        reportMinSnpsTooltip.setText("Enter a value (no. of SNPs) between 5 and 10,000");
        reportMinSnpsField.setTooltip(reportMinSnpsTooltip);
        reportMinSnpsField.focusedProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> observable,
          Boolean oldValue, Boolean newValue ){
                if (!reportMinSnpsField.isFocused()){
                    checkIntegerTextField(reportMinSnpsField, 
                            "Only Report Regions With At Least This Many SNPs", 5, 10000, 
                            "25", true);
                }
            }
        });
        
        Tooltip windowSizeTooltip = new Tooltip();
        windowSizeTooltip.setText("Enter a value (no. of SNPs) between 10 and 10,000");
        windowSizeField.setTooltip(windowSizeTooltip);
        windowSizeField.focusedProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> observable,
          Boolean oldValue, Boolean newValue ){
                if (!windowSizeField.isFocused()){
                    checkIntegerTextField(windowSizeField, "Window Size", 10, 
                            10000, "100", true);
                }
            }
        });
        
        Tooltip hetToleranceTooltip = new Tooltip();
        hetToleranceTooltip.setText("Enter a fraction value between 0.00 and 0.20");
        hetToleranceField.setTooltip(hetToleranceTooltip);
        hetToleranceField.focusedProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> observable,
          Boolean oldValue, Boolean newValue ){
                if (!hetToleranceField.isFocused()){
                    checkDoubleTextField(hetToleranceField, 
                            "Heterozygous Tolerance", 0.0, 0.2, "0.05", true);
                }
            }
        });
        
        Tooltip dischordToleranceTooltip = new Tooltip();
        dischordToleranceTooltip.setText("Enter a fraction value between 0.00 and 0.20");
        dischordToleranceField.setTooltip(dischordToleranceTooltip);
        dischordToleranceField.focusedProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> observable,
          Boolean oldValue, Boolean newValue ){
                if (!dischordToleranceField.isFocused()){
                    checkDoubleTextField(dischordToleranceField, 
                            "Dischordant Tolerance", 0.0, 0.2, "0.05", true);
                }
            }
        });
        
        Tooltip refineSizeTooltip = new Tooltip();
        refineSizeTooltip.setText("Enter a value (no. of SNPs) between 10 and 100");
        refineSizeField.setTooltip(refineSizeTooltip);
        refineSizeField.focusedProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> observable,
          Boolean oldValue, Boolean newValue ){
                if (!refineSizeField.isFocused()){
                    checkIntegerTextField(refineSizeField, "Refine Window Size", 1, 
                            100, "10", true);
                }
            }
        });
        
        Tooltip refineToleranceTooltip = new Tooltip();
        refineToleranceTooltip.setText("Enter a fraction value between 0.00 and 0.40");
        refineToleranceField.setTooltip(refineToleranceTooltip);
        refineToleranceField.focusedProperty().addListener(new ChangeListener<Boolean>(){
            @Override
            public void changed(ObservableValue<? extends Boolean> observable,
          Boolean oldValue, Boolean newValue ){
                if (!refineToleranceField.isFocused()){
                    checkDoubleTextField(refineToleranceField, 
                            "Refine Het Tolerance", 0.0, 0.4, "0.1", true);
                }
            }
        });
       
    }//end of initialize
    
    private boolean allFieldsAreValid(){
        
        if(!checkDoubleTextField(regionLengthField, 
                "Minimum Region Size Per Sample", 0.1, 50.0, "1.0", false)){
            regionLengthField.requestFocus();
            return false;
        }
        if (!checkIntegerTextField(reportLengthField, "Only Report Regions Longer Than...", 
                1000, 50000000, "10000", false)){
            reportLengthField.requestFocus();
            return false;
        }
        if (!checkIntegerTextField(reportMinSnpsField, 
                            "Only Report Regions With At Least This Many SNPs", 5, 10000, 
                            "25", false)){
            reportMinSnpsField.requestFocus();
            return false;
        }
        if(!checkIntegerTextField(windowSizeField, "Window Size", 10, 10000, "100", false)){
            return false;
        }
        if (!checkDoubleTextField(hetToleranceField, "Heterozygous Tolerance", 
                0.0, 0.2, "0.05", false)){
            return false;
        }
        if (!checkDoubleTextField(dischordToleranceField, "Dischordant Tolerance",
                0.0, 0.2, "0.05", false)){
            return false;
        }
        if (!checkIntegerTextField(refineSizeField, "Refine Window Size", 1, 
                            100, "10", false)){
            return false;
        }
        if (!checkDoubleTextField(refineToleranceField, "Refine Het Tolerance", 
                0.0, 0.4, "0.1", false)){
            return false;
        }
        return true;    
    }
    
    EventHandler<KeyEvent> checkNumeric(){
        return new EventHandler<KeyEvent>(){
            @Override  
            public void handle(KeyEvent ke) {
                if (!ke.getCharacter().matches("\\d")){
                    ke.consume();
                }
            }
        };
    }
    
    EventHandler<KeyEvent> checkDecimal(){
        return new EventHandler<KeyEvent>(){
            @Override  
            public void handle(KeyEvent ke) {
                if (!ke.getCharacter().matches("[\\d.]")){
                    ke.consume();
                }
            }
        };
    }
    
    //check value is a valid double number and is within range for text field
    private boolean checkDoubleTextField(TextField field, String name, double min, 
            double max, String defValue, boolean showError){
        String txt = field.getText();
        try {
            Double value = Double.parseDouble(txt);
            if (value < min || value > max){
                if (showError){
                    Dialogs.showErrorDialog(null, "Invalid value (" + txt + ") for "
                    + "'" + name + "'.\n\nPlease enter a value between " + nf.format(min) 
                    + " and " + nf.format(max) + ".", "Please choose a valid number for " 
                    + name +".", "Snp View");
                }
                field.setText(defValue);
                return false;

            }
        }catch (NumberFormatException ex){
            if (showError){
                Dialogs.showErrorDialog(null, "Invalid number format (" + txt + ") "
                    +"for '"+ name +"'.\n\nPlease enter a value between " + nf.format(min) 
                    + " and " + nf.format(max) + ".", 
                    "Please enter a valid number for " + name +".", "Snp View", ex);
            }
            field.setText(defValue);
            return false;
        }
        return true;
    }
    
    //check value is a valid int and is within range for text field
    private boolean checkIntegerTextField(TextField field, String name, int min, 
            int max, String defValue, boolean showError){
        String txt = field.getText();
        try {
            Integer value = Integer.parseInt(txt);
            if (value < min || value > max){
                if(showError){
                    Dialogs.showErrorDialog(null, "Invalid value (" + txt + ") for "
                    + "'" + name + "'.\nPlease enter a value between " + min 
                    + " and " + max + ".", "Please choose a valid number for " 
                    + name +".", "Snp View");
                }
                field.setText(defValue);
                return false;
            }
        }catch (NumberFormatException ex){
            if(showError){
                Dialogs.showErrorDialog(null, "Invalid number format (" + txt + ") "
                    +"for '"+ name +"'.\nPlease enter a value between " + min 
                    + " and " + max + ".", 
                    "Please enter a valid number for " + name +".", "Snp View", ex);
            }
            field.setText(defValue);
            return false;
        }
        return true;
    }
    
    //methods for returning values set by text fields
    public String getRegionLength(){
        return regionLength;
    }
    public String getReportLength(){
        return reportLength;
    }
    
    public String getReportMinSnps(){
        return reportMinSnps;
    }
    
    public boolean getCheckConcordance(){
        return checkConcordance;
    }
    
    public String getWindow(){
        return window;
    }
    public String getHetTolerance(){
        return hetTolerance;
    }
    public String getDischordTolerance(){
        return dischordTolerance;
    }
    public String getRefineSize(){
        return refineSize;
    }
    public String getRefineTolerance(){
        return refineTolerance;
    }
    public boolean getAutosomesOnly(){
        return autosomesOnly;
    }
    public boolean getCancelled(){
        return cancelled;
    }
}
