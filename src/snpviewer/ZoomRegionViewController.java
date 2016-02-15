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
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.imageio.ImageIO;

/**
 * FXML Controller class
 *
 * @author david
 */
public class ZoomRegionViewController implements Initializable {

    @FXML
    Button okButton; 
    @FXML
    SplitPane splitPane;
    @FXML
    TextField regionField;
    @FXML
    TextField selectionIndicator;
    @FXML
    Label sampleLabel;
    @FXML
    ContextMenu cm = new ContextMenu();
    @FXML
    Pane selectionOverlayPane;
    
    Rectangle parentRectangle;
    
    Rectangle dragSelectRectangle = new Rectangle();
    SimpleDoubleProperty dragSelectRectInitX = new SimpleDoubleProperty();
    SimpleDoubleProperty dragSelectRectInitY = new SimpleDoubleProperty();
    SimpleDoubleProperty dragSelectRectX = new SimpleDoubleProperty();
    SimpleDoubleProperty dragSelectRectY = new SimpleDoubleProperty();
    SimpleDoubleProperty anchorInitX = new SimpleDoubleProperty();
    
    ArrayList<SnpFile> snpSamples = new ArrayList<>();
    final NumberFormat nf = NumberFormat.getIntegerInstance();
    Double regionLength = new Double(0);
    Double regionStart = new Double(0);
    String chrom = new String();
    private ArrayList<Pane> panesToAdd = new ArrayList<>();
    SnpViewer mainController = new SnpViewer();
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
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
        
       dragSelectRectangle.widthProperty().bind(dragSelectRectX.subtract(dragSelectRectInitX));
       dragSelectRectangle.heightProperty().bind(selectionOverlayPane.heightProperty());
       //dragSelectRectangle.strokeProperty().set(colorComponants[Colors.line.value]);
       dragSelectRectangle.setStrokeWidth(4.0);
       
       //dragSelectRectangle.setBlendMode(BlendMode.SCREEN);
       dragSelectRectangle.setOpacity(0.45);
       dragSelectRectangle.setVisible(false);
       selectionOverlayPane.getChildren().add(dragSelectRectangle);
        
        final ContextMenu scm = new ContextMenu();//for selection
        
        final MenuItem ocmItem1 = new MenuItem("Save Image to File");
        ocmItem1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                Platform.runLater(new Runnable(){
                    @Override
                    public void run(){
                        savePanesToPng();
                    }
                });
            }
        });
        final MenuItem ocmItem2 = new MenuItem("Write Region to File");
        ocmItem2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                Platform.runLater(new Runnable(){
                    @Override
                    public void run(){
                        writeThisRegion();
                    }
                });
            }
        });
        final MenuItem ocmItem3 = new MenuItem("Add Zoomed Region to Saved Regions");
        ocmItem3.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                Platform.runLater(new Runnable(){
                    @Override
                    public void run(){
                         saveZoomedRegion();
                    }
                });
            }
        });
        
        cm.getItems().add(ocmItem1);
        cm.getItems().add(ocmItem2);
        cm.getItems().add(ocmItem3);

        
        final MenuItem scmItem1 = new MenuItem("Display Flanking SNP IDs");
        scmItem1.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                /* get coordinates of selection and report back
                 * flanking snp ids and coordinates
                 */
                displayFlankingSnpIDs(dragSelectRectangle);
            
            }
        });
        final MenuItem scmItem2 = new MenuItem("Write Selected Region to File");
        scmItem2.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                Platform.runLater(new Runnable(){
                    @Override
                    public void run(){
                /* get coordinates of selection and report back
                 * write SNPs in region to file
                 */
                        writeRegionToFile(dragSelectRectangle);
                        }
                });
            }
        });
        final MenuItem scmItem3 = new MenuItem("Add to Saved Regions");
        scmItem3.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                /* get coordinates of selection and report back
                 * write SNPs in region to file
                 */
               saveSelection(dragSelectRectangle);
            }
        });
        
        
        scm.getItems().add(scmItem1);
        scm.getItems().add(scmItem2);
        scm.getItems().add(scmItem3);
        
        
        
        selectionOverlayPane.addEventHandler(MouseEvent.MOUSE_PRESSED, 
           new EventHandler<MouseEvent>(){
               @Override
               public void handle(MouseEvent e){
                   if (scm.isShowing()){
                           scm.hide();
                   }
                   if (!e.isPrimaryButtonDown()){
                       if (e.isSecondaryButtonDown()){
                            //check we're not overlapping selection
                           if (e.getX() >= dragSelectRectangle.getX() 
                                   && e.getX() <= (dragSelectRectangle.getX() 
                                   + dragSelectRectangle.getWidth())){
                               return;
                           }
                           
                           cm.show(splitPane, e.getScreenX(), e.getScreenY());
                           
                           return;
                       }
                   }
                   if (cm.isShowing()){
                       cm.hide();
                   }

                   dragSelectRectangle.strokeProperty().set(mainController.getColors().get(SnpViewer.Colors.line.value));
                   dragSelectRectangle.fillProperty().set(mainController.getColors().get(SnpViewer.Colors.fill.value));
                   dragSelectRectX.set(0);
                   dragSelectRectangle.setVisible(true);
                   dragSelectRectangle.setX(e.getX());
                   dragSelectRectangle.setY(0);
                   dragSelectRectInitX.set(e.getX());
                   anchorInitX.set(e.getX());


               }
           });
        
        selectionOverlayPane.addEventHandler(MouseEvent.MOUSE_DRAGGED, 
               new EventHandler<MouseEvent>(){
                   @Override
                   public void handle(MouseEvent e){
                       if (!e.isPrimaryButtonDown()){
                           return;
                       }
                       dragSelectRectangle.setVisible(true);
                       if (e.getX() > anchorInitX.doubleValue()){//dragging to the right
                           if (e.getX() <=selectionOverlayPane.getLayoutX() + selectionOverlayPane.getWidth()){
                               //mouse is before the edge of the pane
                               dragSelectRectInitX.set(anchorInitX.doubleValue());
                               dragSelectRectX.set(e.getX());
                            }else{
                               //mouse is over the edge
                               dragSelectRectX.set(selectionOverlayPane.getWidth());
                           }
                       }else{
                           if (e.getX() > selectionOverlayPane.getLayoutX()){
                               dragSelectRectInitX.set(e.getX());
                               dragSelectRectangle.setX(e.getX());
                               dragSelectRectX.set(anchorInitX.doubleValue());
                           }else{
                               dragSelectRectInitX.set(0);
                               dragSelectRectangle.setX(0);
                           /* the two lines below are just to trigger 
                            * dragSelectRectangle.widthProperty listener 
                            * so that start coordinate changes to 1
                            */
                               dragSelectRectX.set(anchorInitX.doubleValue()+1);
                               dragSelectRectX.set(anchorInitX.doubleValue()+1);
                               
                           }
                       }
                   }
               });
       
       selectionOverlayPane.addEventHandler(MouseEvent.MOUSE_RELEASED, 
               new EventHandler<MouseEvent>(){
                   @Override
                   public void handle(MouseEvent e){
                       //dragSelectRectX.set(e.getX());
                       if (!e.isPrimaryButtonDown()){
                           return;
                       }
                       dragSelectRectangle.setVisible(true);
                       if (dragSelectRectangle.getWidth() == 0){
                           clearDragSelectRectangle();
                       }
                   }
               });
        
       selectionOverlayPane.heightProperty().addListener(new ChangeListener<Number>() {
            @Override 
            public void changed(ObservableValue<? extends Number> 
                    observableValue, Number oldSceneWidth, Number newSceneWidth) {
                windowResized(new ActionEvent());
                
            }
        });
        
        selectionOverlayPane.widthProperty().addListener(new ChangeListener<Number>() {
            @Override 
            public void changed(ObservableValue<? extends Number> 
                    observableValue, Number oldSceneWidth, Number newSceneWidth) {
                windowResized(new ActionEvent());
            }
        });
       
       dragSelectRectangle.widthProperty().addListener(new ChangeListener() {
            @Override 
            public void changed(ObservableValue observableValue, 
                Object oldValue, Object newRectWidth) {
                if (dragSelectRectangle.getWidth() > 0){
                    double startCoordinate = regionStart + (regionLength/
                            splitPane.getWidth() * dragSelectRectangle.getX());
                    double endCoordinate = startCoordinate + (regionLength/splitPane.getWidth() * 
                            dragSelectRectangle.getWidth());
                   if (dragSelectRectangle.getX() ==0){
                       startCoordinate = regionStart;
                   }
                   selectionIndicator.setText("chr" + chrom + ":" + nf.format(startCoordinate)
                           + "-" + nf.format(endCoordinate));          
                }else{
                    selectionIndicator.setText("");
                }
            }
       });
       
       dragSelectRectangle.addEventHandler(MouseEvent.MOUSE_CLICKED, 
           new EventHandler<MouseEvent>(){
           @Override
           public void handle (MouseEvent e){
               if (e.getButton() == MouseButton.SECONDARY){
                    
                    if (cm.isShowing()){
                        cm.hide();
                    }

                    scm.show(selectionOverlayPane, e.getScreenX(), e.getScreenY());
               }
           }
       });          
       
        
        selectionOverlayPane.addEventHandler(MouseEvent.MOUSE_MOVED, 
            new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    
                    if (regionLength == null || regionLength <= 0){
                        return;
                    }
                    if (regionStart == null){
                        return;
                    }
                    if (chrom == null){
                        return;
                    }
                   double coordinate = regionStart + (regionLength/splitPane.getWidth() * e.getX());
                   
                   setRegionLabel("chr" + chrom + ":" + nf.format(coordinate));
               }
       });
        
    }
    
    public void windowResized (ActionEvent event){
        if (dragSelectRectangle.getWidth() > 0){
            dragSelectRectangle.setX(0);
            dragSelectRectX.set(0);
            dragSelectRectInitX.set(0);
            dragSelectRectangle.setVisible(false);
        }
    }
    
    public ArrayList<Pane> setPanes(ArrayList<SnpFile> snpFiles){//uses aff and unaff to arrange split panes accordingly
        if (snpFiles == null || snpFiles.size() < 1){
            return null;
        }
        snpSamples.addAll(snpFiles);
        splitPane.getItems().clear();
        splitPane.setMinHeight(550);
        splitPane.setMinWidth(850);
        for (final SnpFile s: snpFiles){
            Pane pane = new Pane();
            pane.setMinHeight(splitPane.getMinHeight()/snpFiles.size());
            pane.setMinWidth(splitPane.getMinWidth());
            pane.addEventHandler(MouseEvent.MOUSE_ENTERED, 
            new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    String sampleName; 
                    if (s.getSampleName() != null && 
                            s.getSampleName().trim().length() > 0){
                        sampleName = s.getSampleName();
                    }else{
                        sampleName = s.getInputFileName();
                    }
                    sampleLabel.setText("Sample: " + sampleName);
                }
            });
            pane.addEventHandler(MouseEvent.MOUSE_EXITED, 
            new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                    sampleLabel.setText("Sample: ");
                }
            });
            panesToAdd.add(pane);
        }
        splitPane.getItems().addAll(panesToAdd);
        return panesToAdd;
    }
    
    public void tidyPanes(){
        for (Object o: splitPane.getItems()){
            if (o instanceof Pane){
                Pane p = (Pane) o;
                p.minWidthProperty().bind(splitPane.widthProperty());
                p.minHeightProperty().bind(splitPane.heightProperty().divide(panesToAdd.size()));
            }
        }
    }
    
    public void setParentController(SnpViewer parentController){
        mainController = parentController;
    }
    
    public SplitPane getSplitPane(){
        return splitPane;
    }
    
    public void savePanesToPng(){
        
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "PNG Image Files (*.png)", "*.png");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setTitle("Save View as Image (.png) File...");
        File pngFile = fileChooser.showSaveDialog(splitPane.getScene().getWindow());
        if (pngFile == null){
            return;
        }else if (!pngFile.getName().endsWith(".png")){
            pngFile = new File(pngFile.getAbsolutePath() + ".png");
        }
        WritableImage image = splitPane.snapshot(null, null);
        if (image == null){
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.getDialogPane().setPrefSize(420, 200);
            error.setResizable(true);
            error.setTitle("SnpViewer");
            error.setHeaderText("PNG conversion failed");
            error.setContentText("Error attempting to save zoomed view"
                    + " to image file");
            error.showAndWait();
            return;
        }
        try{
            ImageIO.write(SwingFXUtils.fromFXImage(image, null),
                    "png", pngFile);
            Alert info = new Alert(AlertType.INFORMATION);
            info.setTitle("SnpViewer");
            info.setHeaderText("Image Saved");
            info.setContentText("Sucessfully saved current view to " + 
                    pngFile.getName());
        }catch (IOException ex) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.getDialogPane().setPrefSize(420, 200);
            error.setResizable(true);
            error.setTitle("SnpViewer");
            error.setHeaderText("PNG conversion failed");
            error.setContentText("Error attempting to save zoomed view "
                    + " to image file\n" + ex.getLocalizedMessage());
            error.showAndWait();
        }
    }
    
    public void setLoadingRectangle(Rectangle rect){
        parentRectangle = rect;
    }
    public void writeThisRegion(){
        Stage stage =  (Stage) mainController.chromSplitPane.getScene().getWindow();
        stage.toFront();
        mainController.writeRegionToFile(chrom, regionStart, regionStart + regionLength);
    }
    
    public void displayFlankingSnpIDs(Rectangle rectangle){
        double startCoordinate = regionStart + (regionLength/splitPane.getWidth() 
                * rectangle.getX());
        double endCoordinate = startCoordinate + (regionLength/splitPane.getWidth() 
                * rectangle.getWidth());
       if (dragSelectRectangle.getX() ==0){
           startCoordinate = regionStart;
       }
       mainController.displayFlankingSnpIDs(chrom, startCoordinate, endCoordinate);
    }
    
    public void writeRegionToFile(Rectangle rectangle){
        Stage stage =  (Stage) mainController.chromSplitPane.getScene().getWindow();
        stage.toFront();
        double startCoordinate = regionStart + (regionLength/splitPane.getWidth() 
                * rectangle.getX());
        double endCoordinate = startCoordinate + (regionLength/splitPane.getWidth() 
                * rectangle.getWidth());
       if (dragSelectRectangle.getX() ==0){
           startCoordinate = regionStart;
       }
       mainController.writeRegionToFile(chrom, startCoordinate, endCoordinate);
    }
    
    public void saveSelection(Rectangle rectangle){
        Stage stage =  (Stage) mainController.chromSplitPane.getScene().getWindow();
        stage.toFront();
        double startCoordinate = regionStart + (regionLength/splitPane.getWidth() 
                * rectangle.getX());
        double endCoordinate = startCoordinate + (regionLength/splitPane.getWidth() 
                * rectangle.getWidth());
       if (dragSelectRectangle.getX() ==0){
           startCoordinate = regionStart;
       }
       mainController.saveRegion(chrom, startCoordinate, endCoordinate);
    }
    
    public void saveZoomedRegion(){
        Stage stage =  (Stage) mainController.chromSplitPane.getScene().getWindow();
        stage.toFront();
        mainController.saveRegion(chrom, regionStart, regionStart + regionLength);
    }
    
    public void setRegionLabel(String labelText){
        regionField.setText(labelText);
    }
    public void setRegionLength(Double length){
        regionLength = length;
    }
    public void setRegionStart(Double length){
        regionStart = length;
    }
    public void setChromosome(String chromosome){
        chrom = chromosome;
    }
    private void clearDragSelectRectangle(){
        dragSelectRectangle.setX(0);
        dragSelectRectX.set(0);
        dragSelectRectInitX.set(0);
        dragSelectRectangle.setVisible(false);
    }
   
}
    