
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

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import static java.lang.System.getProperty;
import java.net.URL;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Line;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javax.imageio.ImageIO;
import static java.nio.file.StandardCopyOption.*;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ComparisonOperator;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Hyperlink;


/**
 * FXML Controller class
 *
 * @author david
 * 
 * 
 */
public class SnpViewer extends Application implements Initializable, Serializable {
    
    String VERSION = "0.9.2";
    
    ArrayList<SnpFile> affFiles = new ArrayList<>();
    ObservableList<SnpFile> affObserve = FXCollections.observableList(affFiles);

    ArrayList<SnpFile> unFiles = new ArrayList<>();
    ObservableList<SnpFile> unObserve = FXCollections.observableList(unFiles);
    
    ArrayList<HashMap<String, ArrayList<Line>>> genotypeLineMap = new ArrayList<>();
    //keys are lowercase call codes (e.g. "aa"), values are list of lines to draw
    Object chromosomeBoxList[]; //list of available chromosomes in choicebox
    String genomeVersion = "";
    NumberFormat nf = NumberFormat.getIntegerInstance(); //number formatting for positionIndicator
    
    boolean projectRunning = false; //becomes true once we click newProjectButton
    boolean progressMode = false; //becomes true when in progressMode
    File snpViewSaveDirectory;// directory for program files
    File projectFile;
    File lastLoadedDir = new File(getProperty("user.home")); 
//directory of last loaded snp data (or home dir if none loaded)
    
    //regions found automatically and/or manually saved
    ArrayList<RegionSummary> savedRegions = new ArrayList<>();
    //graphical display of savedRegions for current chromosome view
    ArrayList<Rectangle> savedRegionsDisplay = new ArrayList<>();
    //RegionSummaries corresponding to each displayed saved region
    ArrayList<RegionSummary> savedRegionsReference = new ArrayList<>();

   
    //panes and labels
    @FXML
    StackPane mainPane;
    @FXML
    Window mainWindow;
    @FXML
    SplitPane horizontalSplit;
    @FXML
    SplitPane chromSplitPane;
    @FXML
    SplitPane labelSplitPane;
    @FXML
    TextField positionIndicator;
    @FXML
    TextField selectionIndicator;
    @FXML
    Label buildLabel;
    @FXML
    Label projectLabel;
    @FXML
    Label qualityLabel;
    
    //menu componants
    @FXML
    MenuBar mainMenu;
    @FXML 
    Menu fileMenu;
    @FXML
    Menu sampleMenu;
    @FXML
    Menu goMenu;
    @FXML
    Menu helpMenu;
    @FXML
    MenuItem closeMenu;
    @FXML
    MenuItem cacheChromsMenu;
    @FXML
    MenuItem saveToPngMenu;
    @FXML
    MenuItem newProjectMenu;
    @FXML
    MenuItem loadProjectMenu;  
    @FXML
    MenuItem saveColoursMenu;
    @FXML
    MenuItem loadColoursMenu;
    @FXML
    MenuItem resetColoursMenu;
    @FXML
    MenuItem autoFindRegions;
    @FXML
    MenuItem nextChromMenu;
    @FXML
    MenuItem prevChromMenu;
    @FXML
    MenuItem firstChromMenu;
    @FXML
    MenuItem lastChromMenu;
    @FXML
    MenuItem addAffSampleMenu;
    @FXML
    MenuItem addUnSampleMenu;
    @FXML
    MenuItem removeSampleMenu;
    @FXML
    MenuItem redrawMenu;
    @FXML
    CheckMenuItem hideSavedRegionsMenu;
    @FXML
    MenuItem outputSavedRegionsMenu;
    @FXML
    MenuItem displaySavedsRegionsMenu;
    @FXML
    MenuItem clearSavedRegionsMenu;
    @FXML
    MenuItem aboutMenu;
    @FXML
    RadioMenuItem noFilteringRadio;
    @FXML
    RadioMenuItem filter99pt9;
    @FXML
    RadioMenuItem filter99pt5;
    @FXML
    RadioMenuItem filter99;
    @FXML
    RadioMenuItem filter95;
    @FXML
    RadioMenuItem filter90;
    
    //buttons etc.
    @FXML
    Button cacheChromsButton;
    @FXML
    Button findRegionsButton;
    @FXML
    Button addAffected;
    @FXML
    Button addUnaffected;
    @FXML
    Button newProjectButton;
    @FXML
    Button loadProjectButton;
    @FXML
    Button saveProjectButton;
    @FXML
    Button redrawButton;
    @FXML
    CheckBox redrawCheckBox;
    @FXML
    ProgressIndicator loadProgress;
    @FXML
    ChoiceBox chromosomeSelector;
    
    //progress components
    @FXML
    ProgressBar progressBar;
    @FXML
    Button cancelButton;
    @FXML
    Label progressTitle;
    @FXML
    Label progressMessage;

    //Genotype and selection colours
    @FXML
    ColorPicker colorPicker = new ColorPicker();
    @FXML
    ChoiceBox colorComponantSelector;
    @FXML
    Color aaColor = Color.BLACK;
    @FXML
    Color bbColor = Color.web("#4d4d4d");
    @FXML
    Color abColor = Color.DEEPPINK;
    @FXML
    Color dragSelectLineColor = Color.web("#8099ff");
    @FXML
    Color dragSelectFillColor = Color.YELLOW;
    @FXML
    Color savedRegionsLineColor = Color.CYAN;
    @FXML
    Color savedRegionsFillColor = Color.LIME;
    Color[] colorComponants = {aaColor, bbColor, abColor, dragSelectLineColor, 
        dragSelectFillColor, savedRegionsLineColor, savedRegionsFillColor};
    ArrayList<Color> colorComp = new ArrayList<>();
    ObservableList<Color> colorObserve;
    
    //Selections
    @FXML
    Pane selectionOverlayPane = new Pane();
    @FXML
    Rectangle dragSelectRectangle = new Rectangle();
    SimpleDoubleProperty dragSelectRectInitX = new SimpleDoubleProperty();
    SimpleDoubleProperty dragSelectRectInitY = new SimpleDoubleProperty();
    SimpleDoubleProperty dragSelectRectX = new SimpleDoubleProperty();
    SimpleDoubleProperty dragSelectRectY = new SimpleDoubleProperty();
    SimpleDoubleProperty anchorInitX = new SimpleDoubleProperty();
    ContextMenu ocm;
    
    Double qualityFilter = null;
    
    public static void main(String[] args) {
        
        Application.launch(SnpViewer.class, (java.lang.String[])null);
    
    }

    @Override
    public void start(final Stage primaryStage) {
        try {
            //FXMLLoader loader = new FXMLLoader(SnpViewer.class.getResource("SnpView.fxml"));
            AnchorPane page = (AnchorPane) FXMLLoader.load(SnpViewer.class.getResource("SnpView.fxml"));
            
            Scene scene = new Scene(page);
            primaryStage.setScene(scene);
            primaryStage.setTitle("SNP Viewer");
            //scene.getStylesheets().add(SnpViewer.class.getResource("SnpViewerStyleSheet.css").toExternalForm());
            primaryStage.show();
            primaryStage.getIcons().add(new Image(this.getClass().
                    getResourceAsStream("icon.png")));
            
            /*primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    try{
                        DialogResponse response = Dialogs.showConfirmDialog(primaryStage,
                                   "Any unsaved changes will be lost",
                                   "Really quit?", "SNP View", DialogOptions.OK_CANCEL);
                        if (DialogResponse.OK.equals(response)){
                            stop();
                        }else {
                            event.consume();
                        }
                    }catch (Exception ex) {
                        Logger.getLogger(SnpViewer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });*/
        } catch (Exception ex) {
            Logger.getLogger(SnpViewer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        labelSplitPane.setDividerPositions();
        chromSplitPane.setDividerPositions();
        Pane lpane = (Pane) horizontalSplit.getItems().get(0);
        SplitPane.setResizableWithParent(lpane, false);
        //mnemonics/shortcuts for menus        
        mainMenu.useSystemMenuBarProperty().set(true);
        fileMenu.setMnemonicParsing(true);
        sampleMenu.setMnemonicParsing(true);
        goMenu.setMnemonicParsing(true);
        helpMenu.setMnemonicParsing(true);
        newProjectMenu.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
        loadProjectMenu.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN));
        addAffSampleMenu.setAccelerator(new KeyCodeCombination(KeyCode.A, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        addUnSampleMenu.setAccelerator(new KeyCodeCombination(KeyCode.U, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));        
        nextChromMenu.setAccelerator(new KeyCodeCombination(KeyCode.EQUALS, KeyCombination.SHORTCUT_DOWN));
        prevChromMenu.setAccelerator(new KeyCodeCombination(KeyCode.MINUS, KeyCombination.SHORTCUT_DOWN));
        firstChromMenu.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT1, KeyCombination.SHORTCUT_DOWN));
        lastChromMenu.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT0, KeyCombination.SHORTCUT_DOWN));
        redrawMenu.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN));
        cacheChromsMenu.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        saveToPngMenu.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        autoFindRegions.setAccelerator(new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN));
        //need to disable hideSavedRegionsMenu accelerator for linux - doesn't work for check menus
        hideSavedRegionsMenu.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        clearSavedRegionsMenu.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        displaySavedsRegionsMenu.setAccelerator(new KeyCodeCombination(KeyCode.T, KeyCombination.SHORTCUT_DOWN));
        outputSavedRegionsMenu.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN));
        removeSampleMenu.setAccelerator(new KeyCodeCombination(KeyCode.R, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN));
        
        
        //set radio menu item toggle group
        ArrayList<RadioMenuItem> callQualityRadios = new ArrayList<>(Arrays.asList(
            noFilteringRadio, filter99pt9, filter99pt5, filter99, filter95, filter90)); 
        ToggleGroup callQualityToggle = new ToggleGroup();
        for (RadioMenuItem r: callQualityRadios){
            r.setToggleGroup(callQualityToggle);
        }
        noFilteringRadio.setOnAction(new EventHandler(){
            @Override
            public void handle(Event ev){
                setQualityFilter(null);
            }
        });
        
        filter99pt9.setOnAction(new EventHandler(){
            @Override
            public void handle(Event ev){
                setQualityFilter(0.001);
            }
        });
        
        filter99pt5.setOnAction(new EventHandler(){
            @Override
            public void handle(Event ev){
                setQualityFilter(0.005);
            }
        });
        
        filter99.setOnAction(new EventHandler(){
            @Override
            public void handle(Event ev){
                setQualityFilter(0.01);
            }
        });
        
        filter95.setOnAction(new EventHandler(){
            @Override
            public void handle(Event ev){
                setQualityFilter(0.05);
            }
        });
        
        filter90.setOnAction(new EventHandler(){
            @Override
            public void handle(Event ev){
                setQualityFilter(0.10);
            }
        });
        
        nextChromMenu.setOnAction(new EventHandler(){
            @Override
            public void handle(Event ev){
                selectNextChromosome(true);
            }
        });
        
        prevChromMenu.setOnAction(new EventHandler(){
            @Override
            public void handle(Event ev){
                selectNextChromosome(false);
            }
        });
        
        firstChromMenu.setOnAction(new EventHandler(){
            @Override
            public void handle(Event ev){
                if (!cancelButton.isDisabled()){
                        cancelButton.fire();
                }
                if (!chromosomeSelector.isDisabled()){
                    
                    chromosomeSelector.getSelectionModel().selectFirst();
                }
            }
        });
        
        lastChromMenu.setOnAction(new EventHandler(){
            @Override
            public void handle(Event ev){
                if (!cancelButton.isDisabled()){
                        cancelButton.fire();
                }
                if (!chromosomeSelector.isDisabled()){
                    chromosomeSelector.getSelectionModel().selectLast();
                }
            }
        });
        
        hideSavedRegionsMenu.setOnAction(new EventHandler(){
            @Override
            public void handle(Event ev){
                showHideSavedRegions();
            }
        });
        
        colorComp.addAll(Arrays.asList(colorComponants));
        
        //selection context menu
        final ContextMenu scm = new ContextMenu();
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
        final MenuItem scmItem3 = new MenuItem("Add To Saved Regions");
        scmItem3.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                /* get coordinates of selection and report back
                 * write SNPs in region to file
                 */
                saveSelection();
            }
        });
        final MenuItem scmItem4 = new MenuItem("Show/Hide Saved Regions");
        scmItem4.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                /* get coordinates of selection and report back
                 * write SNPs in region to file
                 */
                hideSavedRegionsMenu.selectedProperty().setValue(!hideSavedRegionsMenu.isSelected());
                hideSavedRegionsMenu.fire();
            }
        });
        final MenuItem scmItem5 = new MenuItem("Zoom Region");
        scmItem5.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                Platform.runLater(new Runnable(){
                    @Override
                    public void run(){
                        /* get coordinates of selection and report back
                         * write SNPs in region to file
                         */
                        zoomRegion(dragSelectRectangle);
                    }
                });
            }
        });
        final MenuItem scmItem6 = new MenuItem("Write Saved Regions to File");
        scmItem6.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                Platform.runLater(new Runnable(){
                    @Override
                    public void run(){
                    /* get coordinates of selection and report back
                     * write SNPs in region to file
                     */
                        writeSavedRegionsToFile();
                    }
                });
            }
        });
        
        scm.getItems().add(scmItem1);
        scm.getItems().add(scmItem2);
        scm.getItems().add(scmItem3);
        scm.getItems().add(scmItem4);        
        scm.getItems().add(scmItem5);
        scm.getItems().add(scmItem6);
        //overlayPane context menu
        ocm = new ContextMenu();
        final MenuItem ocmItem1 = new MenuItem("Save Image to File");
        ocmItem1.setOnAction(new EventHandler<ActionEvent>() {
        @Override
            public void handle(ActionEvent e) {
                Platform.runLater(new Runnable(){
                    @Override
                    public void run(){
                        drawPaneToPng();
                    }
                });
            }
        });
        
        ocm.getItems().add(ocmItem1);
        ocm.getItems().add(scmItem4);
        ocm.getItems().add(scmItem6);
        

        
        //color selections
        colorComponantSelector.getItems().clear();
        colorComponantSelector.getItems().add("AA");
        colorComponantSelector.getItems().add("BB");
        colorComponantSelector.getItems().add("AB");
        colorComponantSelector.getItems().add("Selection Outline");
        colorComponantSelector.getItems().add("Selection Fill");
        colorComponantSelector.getItems().add("Saved Region Outline");
        colorComponantSelector.getItems().add("Saved Region Fill");
        colorComponantSelector.getSelectionModel().selectFirst();
        colorPicker.setValue(colorComponants[0]);
        colorComponantSelector.getSelectionModel().selectedIndexProperty().addListener
                (new ChangeListener<Number>(){
                    @Override
                    public void changed (ObservableValue ov, Number value, Number new_value){
                        colorPicker.setValue(colorComp.get(new_value.intValue()));
                        colorPicker.fireEvent(new ActionEvent());
                    }
                });
        colorPicker.setOnAction(new EventHandler(){
            @Override
            public void handle(Event t){
                if (! colorComp.get(colorComponantSelector.getSelectionModel()
                .getSelectedIndex()).equals(colorPicker.getValue())){
                    colorComp.set(colorComponantSelector.getSelectionModel()
                            .getSelectedIndex(), colorPicker.getValue());
                    saveProject();
                    //colorComponants[colorComponantSelector.getSelectionModel().getSelectedIndex()] = colorPicker.getValue();
                    if (colorComponantSelector.getSelectionModel()
                            .getSelectedIndex() == Colors.fill.value){
                        dragSelectRectangle.setFill(colorPicker.getValue());
                    }else if (colorComponantSelector.getSelectionModel()
                            .getSelectedIndex() == Colors.line.value){
                        dragSelectRectangle.setStroke(colorPicker.getValue());
                    }else if (colorComponantSelector.getSelectionModel()
                            .getSelectedIndex() == Colors.saveLine.value){
                        for (Rectangle r: savedRegionsDisplay){
                            r.setStroke(colorPicker.getValue());
                        }
                    }else if (colorComponantSelector.getSelectionModel()
                            .getSelectedIndex() == Colors.saveFill.value){
                        for (Rectangle r: savedRegionsDisplay){
                            r.setFill(colorPicker.getValue());
                        }
                    }else{
                        removeSavedChromosomeImages();
                        if (redrawCheckBox.isSelected()){
                            refreshView(null, true);
                        }
                    }
                }
            }
        }); 
       
       
        /*perform appropriate action when user selects a chromosome
         * from the chromosome choice box
         */
        chromosomeSelector.getSelectionModel().selectedIndexProperty().addListener
                (new ChangeListener<Number>() {
                    @Override
                    public void changed (ObservableValue ov, Number value, Number new_value){
                        chromosomeBoxList = chromosomeSelector.getItems().toArray();
                        
                        if (new_value.intValue() > -1){
                            chromosomeSelected((String) chromosomeBoxList[new_value.intValue()]);
                        }
                    }
                });
        
        chromosomeSelector.addEventFilter(KeyEvent.ANY, new EventHandler<KeyEvent>(){
            @Override
            public void handle (KeyEvent ke){
                if (ke.getCode() == KeyCode.UP){
                    ke.consume();
                    chromosomeSelector.show();
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
       
        
        
        /*upon addition of a new affected file adjust components accordingly
         * i.e. ensure appropriate chromosomes are in the choice box
         * adjust the split panes to fit all files and redisplay
         */
        
        affObserve.addListener(new ListChangeListener(){
            @Override
            public void onChanged(ListChangeListener.Change change){
                change.next();/*from the javadoc 
                 * 'Go to the next change. In initial state is invalid a require 
                 * a call to next() before calling other methods. The first 
                 * next() call will make this object represent the first change.
                 */
                if (change.getRemovedSize() > 0){
                    List<SnpFile> both = new ArrayList<>(unFiles);
                    both.addAll(affFiles);
                    recheckChromosomeSelector(both);//need to check all files again, not just affFiles
                }else if (change.getAddedSize() > 0){
                    addToChromosomeSelector(affFiles);
                }
            }
        });
        
        /*as above 
         * but for unaffected files
         */
        unObserve.addListener(new ListChangeListener(){
            @Override
            public void onChanged(ListChangeListener.Change change){
                change.next();
                if (change.getRemovedSize() > 0){
                    List<SnpFile> both = new ArrayList<>(unFiles);
                    both.addAll(affFiles);
                    recheckChromosomeSelector(both);//need to check all files again, not just unFiles
                }else if (change.getAddedSize() > 0){
                    addToChromosomeSelector(unFiles);
                }

            }
        });
        
        selectionOverlayPane.addEventHandler(MouseEvent.MOUSE_MOVED, 
            new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent e) {
                   if (!genomeVersion.equals("") && chromosomeSelector.getSelectionModel().getSelectedIndex() > -1){
                       try{
                           ChromosomeLength chromLength = new ChromosomeLength(genomeVersion);
                           String currentChrom = (String) chromosomeBoxList[
                                   chromosomeSelector.getSelectionModel().getSelectedIndex()];
                           double coordinate = chromLength.getLength(currentChrom)/chromSplitPane.getWidth() * e.getX();
                           positionIndicator.setText(nf.format(coordinate));

                       }catch (Exception ex){
                           positionIndicator.setText("Build Error!");
                       }

                   }
                }
       });    
       /*handle mouse dragging and effect on dragSelectRectangle
        * 
        */ 
       dragSelectRectangle.widthProperty().bind(dragSelectRectX.subtract(dragSelectRectInitX));
       dragSelectRectangle.heightProperty().bind(selectionOverlayPane.heightProperty());
       //dragSelectRectangle.strokeProperty().set(colorComponants[Colors.line.value]);
       dragSelectRectangle.setStrokeWidth(4.0);
       
       //dragSelectRectangle.setBlendMode(BlendMode.SCREEN);
       dragSelectRectangle.setOpacity(0.45);
       dragSelectRectangle.setVisible(false);
       selectionOverlayPane.getChildren().add(dragSelectRectangle);
       
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
                           //check we're not overlapping saved regions
                           for (Rectangle r: savedRegionsDisplay){
                               if (r.isVisible() && e.getX() >= r.getX() && 
                                       e.getX() <= r.getX() + r.getWidth()){
                                   return;
                               }
                           }
                           if (chromosomeSelector.getSelectionModel().isEmpty()){
                               ocmItem1.setDisable(true);
                           }else{
                               ocmItem1.setDisable(false);
                           }
                           ocm.show(selectionOverlayPane, e.getScreenX(), e.getScreenY());
                           
                           return;
                       }
                   }
                   if (ocm.isShowing()){
                       ocm.hide();
                   }

                   dragSelectRectangle.strokeProperty().set(colorComp.get(Colors.line.value));
                   dragSelectRectangle.fillProperty().set(colorComp.get(Colors.fill.value));
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
       
       dragSelectRectangle.widthProperty().addListener(new ChangeListener() {
            @Override 
            public void changed(ObservableValue observableValue, 
                Object oldValue, Object newRectWidth) {
                    if (!genomeVersion.equals("") &&
                        chromosomeSelector.getSelectionModel().getSelectedIndex() > -1
                        && dragSelectRectangle.getWidth() > 0){
                        try{
                           ChromosomeLength chromLength = new ChromosomeLength(genomeVersion);
                           String currentChrom = (String) chromosomeBoxList[
                                   chromosomeSelector.getSelectionModel().getSelectedIndex()];
                           double startCoordinate = chromLength.getLength(currentChrom)
                                   /selectionOverlayPane.getWidth() * dragSelectRectangle.getX();
                           double selectionWidth = chromLength.getLength(currentChrom)
                                   /selectionOverlayPane.getWidth() * dragSelectRectangle.getWidth();
                           if (dragSelectRectangle.getX() ==0){
                               startCoordinate = 1;
                           }
                           selectionIndicator.setText("chr" + currentChrom + ":" 
                                   +nf.format(startCoordinate) + "-" + 
                                   nf.format(startCoordinate + selectionWidth));
                       }catch (Exception ex){
                           selectionIndicator.setText("Build Error!");
                       }
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
                    if (chromosomeSelector.getSelectionModel().isEmpty()){
                        scmItem1.setDisable(true);
                        scmItem2.setDisable(true);
                    }else{
                        scmItem1.setDisable(false);
                        scmItem2.setDisable(false);
                    }
                    if (ocm.isShowing()){
                        ocm.hide();
                    }

                    scm.show(selectionOverlayPane, e.getScreenX(), e.getScreenY());
               }
           }
       });          
       
    }//end initialize
    
    /* value for quality filter in all SnpFiles - score is liklihood genotype is
     * wrong, hence lower is better.  Set to null for no filtering
     */
    private void setQualityFilter(Double d){
        if (qualityFilter == null && d == null){
            setQualityLabel();
            return;
        }
        if (qualityFilter != null && d != null){
            if (qualityFilter.equals(d)){
                return;
            }
        }
        
        qualityFilter = d;
        ArrayList<SnpFile> both = new ArrayList<>();
        both.addAll(affObserve);
        both.addAll(unObserve);
        ArrayList<String> noQualityField = new ArrayList<>();
        for (SnpFile s : both){
            if (s.hasQualityField){
                s.setQualityFilter(qualityFilter);
            }else{
                noQualityField.add(s.getInputFileName());
            }
        }
        if (!noQualityField.isEmpty()){
            StringBuilder noQual = new StringBuilder();
            for (String s: noQualityField){
                noQual.append(s).append("\n");
            }
            Alert warning = new Alert(AlertType.WARNING);
            warning.setTitle("SnpViewer");
            warning.setContentText("The following input file(s) do not "
                    + "have call confidence information:\n\n" + noQual.toString()
                    + "\nChanging call quality filters will have no effect on these"
                    + " files.  To get quality fields ensure you include them "
                    + "when producing birdseed files with the Affymetrix Genotyping"
                    + " Console software.");
            warning.setHeaderText("File(s) without call confidences");
            warning.getDialogPane().setPrefSize(420, 200);
            warning.setResizable(true);
            warning.showAndWait();
        }
        setQualityLabel();
        saveProject();
        refreshView(null, redrawCheckBox.isSelected());
    }
    
    private void setQualityLabel(){
        if (qualityFilter != null){
            Double percent = 100 - (100 * qualityFilter);
            qualityLabel.setText(percent.toString() + " % confidence or greater");
        }else{
            qualityLabel.setText("None");
        }
    }
    
    public ArrayList<Color> getColors(){
        return colorComp;
    }
    
    public void resetColours(ActionEvent e){
        colorComp.clear();
        colorComp.addAll(Arrays.asList(colorComponants));
        for (int ci = 0; ci < colorComp.size(); ci++){
            colorComponantSelector.getSelectionModel().clearAndSelect(ci);
            colorPicker.setValue(colorComp.get(ci));
            colorPicker.fireEvent(new ActionEvent());
        }
        colorComponantSelector.getSelectionModel().selectFirst();
        removeSavedChromosomeImages();
        refreshView((String) chromosomeSelector.
                        getSelectionModel().getSelectedItem(), true);
    }
    
    public void saveColours(ActionEvent e){
        FileChooser fileChooser = new FileChooser();
        if (projectFile != null){
            fileChooser.setInitialDirectory(projectFile.getParentFile());
        }else{
            fileChooser.setInitialDirectory(new File(getProperty("user.home")));
        }
        fileChooser.setInitialDirectory(new File(getProperty("user.home")));
        fileChooser.setTitle("Save Colour Scheme (.svcols) As...");
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("SNP Viewer Colour Scheme", "*.svcols");
        fileChooser.getExtensionFilters().add(extFilter);
        File colorFile = fileChooser.showSaveDialog(mainWindow);
        if (colorFile != null){
            if (!colorFile.getName().endsWith(".svcols")){
                colorFile = new File(colorFile.getAbsolutePath() + ".svcols");
            }
    
            try{
                FileOutputStream fos = new FileOutputStream(colorFile);
                try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(fos))) {
                    for (Color c: colorComp){
                        out.writeObject(c.toString());
                    }
                    out.close();
                    projectLabel.setText("Project: " +projectFile.getName());
                }
            }catch (IOException ex){
                Alert error = new Alert(AlertType.ERROR);
                error.getDialogPane().setPrefSize(420, 200);
                error.setResizable(true);
                error.setTitle("SnpViewer");
                error.setHeaderText("Could not save colour scheme");
                error.setContentText(ex.getLocalizedMessage());
                ex.printStackTrace();
                error.showAndWait();
            }
        }
    }
    
    public void loadColourScheme(ActionEvent e){
        FileChooser fileChooser = new FileChooser();
        if (projectFile != null){
            fileChooser.setInitialDirectory(projectFile.getParentFile());
        }else{
            fileChooser.setInitialDirectory(new File(getProperty("user.home")));
        }
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SNP Viewer Colour Scheme", "*.svcols"));
        fileChooser.setTitle("Open SNP Viewer Colour Scheme (.svcols) file");
        File loadFile = fileChooser.showOpenDialog(mainWindow);
        if (loadFile != null){
            try{
                ObjectInputStream is = new ObjectInputStream(new BufferedInputStream
                        (new FileInputStream(loadFile)));
                ArrayList<Color> loadedColors = new ArrayList<>();
                for (Color c: colorComp){
                        String colorString = (String) is.readObject();
                        loadedColors.add(Color.valueOf(colorString));
                }
                for (int ci = 0; ci < loadedColors.size(); ci++){
                        colorComponantSelector.getSelectionModel().clearAndSelect(ci);
                        colorPicker.setValue(loadedColors.get(ci));
                        colorPicker.fireEvent(new ActionEvent());
                }
                colorComponantSelector.getSelectionModel().selectFirst();
                colorComp.clear();
                colorComp.addAll(loadedColors);
                is.close();
                saveProject();
                removeSavedChromosomeImages();
                refreshView((String) chromosomeSelector.
                        getSelectionModel().getSelectedItem(), true);
            }catch (IOException | ClassNotFoundException ex){
                
            }
        }
    }
    
    private void zoomRegion(Rectangle rectangle){
        try{
            FXMLLoader loader = new FXMLLoader(getClass().
                   getResource("ZoomRegionView.fxml"));
           ChromosomeLength chromLength = new ChromosomeLength(genomeVersion);
           String currentChrom = (String) chromosomeBoxList[
                   chromosomeSelector.getSelectionModel().getSelectedIndex()];
           double startCoordinate = chromLength.getLength(currentChrom)
                   /selectionOverlayPane.getWidth() * rectangle.getX();
           double selectionWidth = chromLength.getLength(currentChrom)
                   /selectionOverlayPane.getWidth() * rectangle.getWidth();
           if (rectangle.getX() ==0){
               startCoordinate = 1;
           }
           
           Pane page = (Pane) loader.load();
           Scene scene = new Scene(page);
           Stage stage = new Stage();
           stage.setScene(scene);
           stage.setTitle("chr" + currentChrom + ":" + nf.format(startCoordinate) 
                   + "-" + nf.format(startCoordinate + selectionWidth) );
           //scene.getStylesheets().add(SnpViewer.class
           //             .getResource("SnpViewerStyleSheet.css").toExternalForm());
           String subPath = "zoom"; 
           stage.initModality(Modality.NONE);
           stage.getIcons().add(new Image(this.getClass().
                    getResourceAsStream("icon.png")));
           stage.show();
           ZoomRegionViewController zoomController = (ZoomRegionViewController) loader.getController();
           ArrayList<SnpFile> bothFiles = new ArrayList<>();
           bothFiles.addAll(affFiles);
           bothFiles.addAll(unFiles);
           ArrayList<Pane> zoomPanes = zoomController.setPanes(bothFiles);
           zoomController.setParentController(this);
           zoomController.setLoadingRectangle(rectangle);
           zoomController.setRegionLength(selectionWidth);
           zoomController.setRegionStart(startCoordinate);
           zoomController.setChromosome(currentChrom);
           SplitPane zoomSplit = zoomController.getSplitPane();
           Iterator<SnpFile> sIter = bothFiles.iterator();
           Iterator<Pane> pIter = zoomPanes.iterator();
           if (!sIter.hasNext() || !pIter.hasNext()){
               return;
           }
           SnpFile firstFile = sIter.next();
           Pane firstPane = pIter.next();
           drawCoordinatesWithIterator(firstFile, firstPane, subPath, sIter, pIter, 
                   1, bothFiles.size(), currentChrom, startCoordinate, 
                   startCoordinate + selectionWidth, true, zoomSplit);
           zoomController.tidyPanes();
           
        }catch(ChromosomeLength.ChromosomeLengthException | IOException ex){
            Alert error = new Alert(AlertType.ERROR);
            ex.printStackTrace();
            error.getDialogPane().setPrefSize(420, 200);
            error.setResizable(true);
            error.setTitle("SnpViewer");
            error.setHeaderText("Error displaying zoomed region");
            error.setContentText(ex.getMessage());
            error.showAndWait();
        }
    }
    
    private void clearDragSelectRectangle(){
        dragSelectRectangle.setX(0);
        dragSelectRectX.set(0);
        dragSelectRectInitX.set(0);
        dragSelectRectangle.setVisible(false);
    }
    
    public void selectNextChromosome(boolean next){
        if (!cancelButton.isDisabled()){
            cancelButton.fire();
        }
        if (next){
            chromosomeSelector.getSelectionModel().selectNext();
        }else{
            chromosomeSelector.getSelectionModel().selectPrevious();
        }
    }
    
    private void removeSavedChromosomeImages(){
        ArrayList<SnpFile> bothFiles = new ArrayList<>(affFiles);
        bothFiles.addAll(unFiles);
        for (SnpFile f: bothFiles){
            for (Iterator it = chromosomeSelector.getItems().iterator(); 
                    it.hasNext();) {
                String chrom = (String) it.next();
                 /*below is a horrible cludgy fix to remove the sub folders - 
                 * need to set up an enum or global variable for the call values 
                 * to be used in different contexts at some point 
                 */
                ArrayList<String> quals = new ArrayList<>(Arrays.asList(null, "85", 
                "90", "95", "99"));
                for (String sub: quals){
                    File pngFile = new File(f.getOutputDirectoryName() + "/" + chrom + ".png");
                    if (sub != null){
                        pngFile = new File(f.getOutputDirectoryName() + "/" + sub 
                                + "/" + chrom + ".png");
                    }
                    if (pngFile.exists()){
                        boolean deleted = pngFile.delete();
                        if (! deleted){
                            Alert error = new Alert(AlertType.ERROR);
                            error.setTitle("SnpViewer");
                            error.setHeaderText("Error deleting old chromosome images");
                            error.setContentText("Unable to delete old chromosome "
                                    + "image " + pngFile.getPath() +". Please check"
                                    + " permissions.");
                            error.showAndWait();
                            return;
                        }
                    }
                }
               
                
            }
        }
    }
    
    public void chromosomeSelected (String chrom){
        clearDragSelectRectangle();
        selectionOverlayPane.getChildren().clear();
        savedRegionsDisplay.clear();
        savedRegionsReference.clear();
        refreshView(chrom, redrawCheckBox.isSelected());
        drawSavedRegions(chrom);
        showHideSavedRegions();
        
    }
    
    public void showSavedRegionsTable(){
        if (savedRegions.size() > 0){
           FXMLLoader tableLoader = new FXMLLoader(getClass().
                   getResource("MultiRegionReporter.fxml"));
           try{
                Pane tablePane = (Pane) tableLoader.load();
                MultiRegionReporterController multiReg = 
                        (MultiRegionReporterController) tableLoader.getController();
                multiReg.setParentController(this);
                Scene tableScene = new Scene(tablePane);
                Stage tableStage = new Stage();
                tableStage.setScene(tableScene);
                //tableScene.getStylesheets().add(SnpViewer.class
                //        .getResource("SnpViewerStyleSheet.css").toExternalForm());
                tableStage.getIcons().add(new Image(this.getClass().
                       getResourceAsStream("icon.png")));
                multiReg.displayData(savedRegions);
                tableStage.setTitle("Saved Regions");
                tableStage.initModality(Modality.NONE);
                
                tableStage.show();
           }catch (Exception ex){
                Alert error = new Alert(AlertType.ERROR);
                error.getDialogPane().setPrefSize(420, 200);
                error.setResizable(true);
                error.setTitle("SnpViewer");
                error.setHeaderText("Error displaying Saved Regions - "
                        + "see Details for stack trace.");
                error.setContentText(ex.getLocalizedMessage());
                ex.printStackTrace();
                error.showAndWait();
           }
       }else{
            Alert error = new Alert(AlertType.WARNING);
            error.setTitle("SnpViewer");
            error.setHeaderText("No regions Found");
            error.setContentText("No regions found matching parameters.");
            error.showAndWait();
        }
    }
    
    private void clearSavedRegions(){
        savedRegions.clear();
        savedRegionsDisplay.clear();
        savedRegionsReference.clear();
        selectionOverlayPane.getChildren().clear();
        selectionOverlayPane.getChildren().add(dragSelectRectangle);
        saveProject();
    }
    
    private void showHideSavedRegions(){
        for (Rectangle rect: savedRegionsDisplay){
            rect.setVisible(!hideSavedRegionsMenu.isSelected());
        }
        
    }
    
    private void drawSavedRegions(String chrom){
        selectionOverlayPane.getChildren().clear();
        if (savedRegions.isEmpty()){
            selectionOverlayPane.getChildren().add(dragSelectRectangle);
            return;
        }
        for (RegionSummary r: savedRegions){
            if (r.getChromosome() == null){
                selectionOverlayPane.getChildren().add(dragSelectRectangle);
                return;
            }
            if (r.getChromosome().equalsIgnoreCase(chrom)){
                drawRegionSummary(r, chrom);
            }
        }
        int rectCounter = 0;
        for (final Rectangle rect: savedRegionsDisplay){
            final int counter = rectCounter;
            final ContextMenu scm = new ContextMenu();
            final MenuItem scmItem1 = new MenuItem("Display Flanking SNP IDs");
            scmItem1.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    /* get coordinates of selection and report back
                     * flanking snp ids and coordinates
                     */
                    displayFlankingSnpIDs(rect);

                }
            });
            final MenuItem scmItem2 = new MenuItem("Write Region to File");
            scmItem2.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    Platform.runLater(new Runnable(){
                        @Override
                        public void run(){
                            /* get coordinates of selection and 
                             * write SNPs in region to file
                             */
                            writeRegionToFile(rect);
                        }
                    });
                }
            });
            final MenuItem scmItem3 = new MenuItem("Remove this Saved Region");
            scmItem3.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    /* get coordinates of selection and 
                     * write SNPs in region to file
                     */
                    removeSavedRegion(counter);

                }
            });
            final MenuItem scmItem4 = new MenuItem("Show/Hide Saved Regions");
            scmItem4.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    /* get coordinates of selection and report back
                     * write SNPs in region to file
                     */
                    hideSavedRegionsMenu.selectedProperty().setValue(!hideSavedRegionsMenu.isSelected());
                    hideSavedRegionsMenu.fire();
                }
            });
            final MenuItem scmItem5 = new MenuItem("Zoom Region");
            scmItem5.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    /* get coordinates of selection and report back
                     * write SNPs in region to file
                     */
                    zoomRegion(rect);
                }
            });
            final MenuItem scmItem6 = new MenuItem("Write Saved Regions to File");
            scmItem6.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent e) {
                    Platform.runLater(new Runnable(){
                        @Override
                        public void run(){
                        /* get coordinates of selection and report back
                         * write SNPs in region to file
                         */
                            writeSavedRegionsToFile();
                        }
                    });
                }
            });

            scm.getItems().add(scmItem1);
            scm.getItems().add(scmItem2);
            scm.getItems().add(scmItem3);
            scm.getItems().add(scmItem4);
            scm.getItems().add(scmItem5);
            scm.getItems().add(scmItem6);
            rect.addEventHandler(MouseEvent.MOUSE_CLICKED, 
               new EventHandler<MouseEvent>(){
               @Override
               public void handle (MouseEvent e){
                   ocm.hide();
                   
                   if (scm.isShowing()){
                       scm.hide();
                   }
                   if (e.getButton() == MouseButton.SECONDARY){
                        if (chromosomeSelector.getSelectionModel().isEmpty()){
                            for (MenuItem mi: scm.getItems()){
                                mi.setDisable(true);
                            }
                        }else{
                            for (MenuItem mi: scm.getItems()){
                                mi.setDisable(false);
                            }
                        }
                        
                        scm.show(selectionOverlayPane, e.getScreenX(), e.getScreenY());
                  }
               }
               
           });   
            rect.setVisible(true);
            selectionOverlayPane.getChildren().add(rect);
            rectCounter++;
        }
        selectionOverlayPane.getChildren().add(dragSelectRectangle);
    }
    
    private void removeSavedRegion(int index){
        RegionSummary regionToRemove = savedRegionsReference.get(index);
        if (savedRegions.contains(regionToRemove)){
            savedRegions.remove(regionToRemove);
            saveProject();
            savedRegionsDisplay.clear();
            savedRegionsReference.clear();
            selectionOverlayPane.getChildren().clear();
            selectionOverlayPane.getChildren().add(dragSelectRectangle);
            drawSavedRegions((String) chromosomeBoxList[chromosomeSelector
                    .getSelectionModel().getSelectedIndex()]);
        }else{
            Alert error = new Alert(AlertType.ERROR);
            error.setTitle("SnpViewer");
            error.setHeaderText("ERROR");
            error.setContentText("Can't find item for removal! Please "
                + "report this error.");
            error.showAndWait();
        }
    }
    
    private void drawRegionSummary(RegionSummary reg, String currentChrom){
        if (currentChrom == null){
            if (reg.getChromosome() != null){
                currentChrom = reg.getChromosome();
            }else{
                return;
            }
        }
        ChromosomeLength chromLength;
        try{
            chromLength = new ChromosomeLength(genomeVersion);
        }catch (Exception ex){
            chromLength = new ChromosomeLength();
        }
        double x;
        double width;
        double cLength;
        try{
            cLength =  chromLength.getLength(currentChrom);
        }catch (Exception ex){
            ex.printStackTrace();
            return;
        }
        int startPos = reg.getStartPos();
        int rLength = reg.getLength();
        x = chromSplitPane.getWidth()/cLength * startPos;
        width = chromSplitPane.getWidth()/cLength * rLength; 
        
        Rectangle regionRectangle = new Rectangle();
        regionRectangle.setX(x);
        regionRectangle.setWidth(width);
        regionRectangle.setY(0);
        regionRectangle.xProperty().bind(selectionOverlayPane.widthProperty().divide(cLength).multiply(startPos));
        regionRectangle.heightProperty().bind(selectionOverlayPane.heightProperty());
        regionRectangle.widthProperty().bind(selectionOverlayPane.widthProperty().divide(cLength).multiply(rLength));
        regionRectangle.strokeProperty().set(colorComp.get(Colors.saveLine.value));
        regionRectangle.fillProperty().set(colorComp.get(Colors.saveFill.value));
        regionRectangle.setOpacity(0.40);
        regionRectangle.setStrokeWidth(2);
        savedRegionsDisplay.add(regionRectangle);
        savedRegionsReference.add(reg);
    }
    
    public void cacheChromsFired(){
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.getDialogPane().setPrefSize(420, 250);
        alert.setResizable(true);
        ButtonType cButton = ButtonType.CANCEL;
        ButtonType okButton = ButtonType.OK;
        alert.getButtonTypes().setAll(okButton, cButton);
        alert.setTitle("SnpViewer");
        alert.setHeaderText("Continue caching chromosome images?");
        alert.setContentText("Caching chromsome images can take a long time. "
                + "Once finished you will have quick access to each chromosome "
                + "image.\n\nClick 'OK' to continue.");
        Optional<ButtonType> response = alert.showAndWait();
        if (response.get() != okButton){
            return;
        }
        List chroms = chromosomeSelector.getItems();
        setProgressMode(true);
        Iterator chromIter = chroms.iterator();
        ArrayList<SnpFile> bothFiles = new ArrayList<>(affFiles);
        bothFiles.addAll(unFiles);
        Pane tempPane =  new Pane();
        tempPane.setMinWidth(chromSplitPane.getWidth());
        tempPane.setMinHeight(chromSplitPane.getHeight());
        //tempPane.setCache(true);
        chromSplitPane.getItems().clear();
        chromSplitPane.getItems().add(tempPane);
        final Iterator fileIter = bothFiles.iterator();
        String firstChrom = (String) chromIter.next();
        SnpFile firstFile = (SnpFile) fileIter.next();
        Stage stage = (Stage) chromSplitPane.getScene().getWindow();
        //stage.setResizable(false);
        fixStageSize(stage, true);
        progressBar.progressProperty().unbind();
        progressBar.progressProperty().setValue(0);
        String pngPath = null;
        if (qualityFilter != null){
            Integer percent = new Integer(100 - (int) (qualityFilter * 100));
            pngPath = percent.toString();
        }
        cacheChromsWithIterator(chromIter, firstFile, tempPane, 
            pngPath, fileIter, 1, bothFiles.size() * chroms.size(), firstChrom);
        
    }
       
    public void cacheChromsWithIterator(final Iterator chromIter, final SnpFile sfile, 
            final Pane pane, final String pngPath, final Iterator sIter,
            final int currentFile, final int totalFiles, final String chrom){
        final DrawSnpsToPane draw = new DrawSnpsToPane(pane, sfile, chrom, 
                   Colors.aa.value, Colors.bb.value, Colors.ab.value);

           progressBar.progressProperty().unbind();
           //progressBar.progressProperty().bind(draw.progressProperty());
           progressTitle.setText("Processing chr" + chrom );
           progressMessage.setText("File " + sfile.inputFile.getName());
           cancelButton.setOnAction(new EventHandler<ActionEvent>(){
               @Override
               public void handle(ActionEvent actionEvent){
                    draw.cancel();

                }
           });

           draw.setOnCancelled(new EventHandler<WorkerStateEvent>(){
               @Override
               public void handle (WorkerStateEvent t){
                   progressBar.progressProperty().unbind();
                   progressBar.setProgress(0);
                   progressTitle.setText("Drawing Cancelled");
                   progressMessage.textProperty().unbind();
                   progressMessage.setText("Drawing Cancelled");
                   setProgressMode(false);
                   Stage stage = (Stage) chromSplitPane.getScene().getWindow();
                   fixStageSize(stage, false);        
                  // stage.setResizable(true);
               }
           });
           draw.setOnSucceeded(new EventHandler<WorkerStateEvent>(){
               @Override
               public void handle (WorkerStateEvent t){
                   ArrayList<HashMap<String, Double>> result = (ArrayList<HashMap<String, Double>>) t.getSource().getValue();
                   progressBar.progressProperty().unbind();
                   progressBar.setProgress((double) currentFile/((double) totalFiles * 2));
                   progressTitle.setText("");
                   progressMessage.textProperty().unbind();
                   progressMessage.setText("");
                   pane.getChildren().clear();
                   /*if (pane.getMinHeight() < 200){
                       pane.setMinHeight(200);
                   }
                   if (pane.getMinWidth() < 800){
                       pane.setMinWidth(800);
                   }*/
                   if (result != null){
                       List<Line> lines = drawLinesToPane(pane, result);
                       pane.getChildren().addAll(lines);
                       convertSampleViewToImage(sfile, pane, chrom, pngPath);
                       lines.clear();
                   }
                   progressBar.setProgress((double) currentFile/(double) totalFiles);
                   if (sIter.hasNext()){
                       cacheChromsWithIterator(chromIter, (SnpFile) sIter.next(), pane, 
                               pngPath, sIter, currentFile + 1, totalFiles, chrom);
                   }else{
                       if (chromIter.hasNext()){
                           ArrayList<SnpFile> bothFiles = new ArrayList<>(affFiles);
                           bothFiles.addAll(unFiles);
                           Iterator sIterNew = bothFiles.iterator();
                           cacheChromsWithIterator(chromIter, (SnpFile) sIterNew.next(), pane, 
                               pngPath, sIterNew, currentFile + 1, totalFiles, (String) chromIter.next());
                       }else{
                           progressBar.progressProperty().unbind();
                           progressBar.setProgress(0);
                           setProgressMode(false);
                           redrawCheckBox.setSelected(false);
                           chromosomeSelector.getSelectionModel().selectFirst();
                           refreshView((String) chromosomeSelector.
                                   getSelectionModel().getSelectedItem(), false);
                           Stage stage = (Stage) chromSplitPane.getScene().getWindow();
                           fixStageSize(stage, false);
                           stage.setResizable(true);
                       }
                   }
               }
           });
           draw.setOnFailed(new EventHandler<WorkerStateEvent>(){
                        @Override
                        public void handle(WorkerStateEvent t){
                            progressBar.progressProperty().unbind();
                            progressBar.setProgress(0);
                            progressTitle.setText("ERROR!");
                            progressMessage.textProperty().unbind();
                            progressMessage.setText("Drawing failed!");
                            setProgressMode(false);
                            Stage stage = (Stage) chromSplitPane.getScene().getWindow();
                            stage.setResizable(true);
                            fixStageSize(stage, false);
                        }

                    });
           draw.start();
    }
    
    private void clearSplitPanes(){
        for (Iterator it = chromSplitPane.getItems().iterator(); it.hasNext();){
            Object child = it.next();
            if (child instanceof Pane){
                Pane p = (Pane) child;
                for (Iterator pit = p.getChildren().iterator(); pit.hasNext();){
                    Object pChild = pit.next();
                    if (pChild instanceof ImageView){
                        ImageView i = (ImageView) pChild;
                        i.fitHeightProperty().unbind();
                        i.fitWidthProperty().unbind();
                    }else if (pChild instanceof Line){
                        Line l = (Line) pChild;
                        l.startXProperty().unbind();
                        l.endXProperty().unbind();
                        l.startYProperty().unbind();
                        l.endYProperty().unbind();
                    }
                }
                p.minWidthProperty().unbind();
                p.minHeightProperty().unbind();
            }
        }
        for (Iterator it = labelSplitPane.getItems().iterator(); it.hasNext();){
            Object child = it.next();
            if (child instanceof Pane){
                Pane p = (Pane) child;
                p.minWidthProperty().unbind();
                p.minHeightProperty().unbind();
            }
        }
        chromSplitPane.getItems().clear();
        labelSplitPane.getItems().clear();
    }
    
    public void refreshView(String chrom, boolean forceRedraw){
        //if forceRedraw is false look for existing png files for each snpFile
        if (chrom == null){
        /*if null is passed then select/reselect chromosome from 
         * chromosomeSelector, return and let chromosomeSelector's 
         * listener refire this method
         */
            if (chromosomeSelector.getSelectionModel().isEmpty()){
                chromosomeSelector.getSelectionModel().selectFirst();
            }else{
                   int sel = chromosomeSelector.getSelectionModel().getSelectedIndex();
                   chromosomeSelector.getSelectionModel().clearSelection();
                   chromosomeSelector.getSelectionModel().select(sel);
            }
            return;
        }
        int totalFiles = affFiles.size() + unFiles.size();
        if (totalFiles < 1){
            return;
        }
        ArrayList<Pane> panesToAdd = new ArrayList<>();
        ArrayList<ScrollPane> labelsToAdd = new ArrayList<>();
        clearSplitPanes();
        
        setProgressMode(true);
        nextChromMenu.setDisable(false);
        nextChromMenu.setDisable(false);
        for (final SnpFile f: affFiles ){
            Pane sPane = new Pane();
            sPane.setMinHeight(chromSplitPane.getHeight()/totalFiles);
            sPane.setMinWidth(chromSplitPane.getWidth());
            sPane.setVisible(true);
            panesToAdd.add(sPane);
            ScrollPane labelPane = new ScrollPane();
            Label fileLabel = new Label(f.inputFile.getName() + "\n(Affected)");
            labelPane.setMinHeight(labelSplitPane.getHeight()/totalFiles);
            labelPane.setPrefWidth(labelSplitPane.getWidth());
            labelPane.minHeightProperty().bind(labelSplitPane.heightProperty().divide(totalFiles));
            VBox vbox = new VBox();
            vbox.setSpacing(10);
            vbox.getChildren().add(fileLabel);
            final TextField textField = new TextField();
            textField.setPromptText("Sample Name");
            if (f.getSampleName() != null){
                textField.setText(f.getSampleName());
            }
            textField.setFocusTraversable(true);
            textField.setOnKeyPressed(new EventHandler<KeyEvent>(){
                @Override
                public void handle (KeyEvent ke){
                    if (ke.getCode().equals(KeyCode.ENTER)){
                        if (!textField.getText().isEmpty()){
                            String name = textField.getText().trim();
                            if (name.length() > 0){
                                f.setSampleName(name);
                            }
                            textField.getParent().requestFocus();
                            saveProject();
                        }
                    }
                }
            });
            textField.focusedProperty().addListener(new ChangeListener<Boolean>(){
                @Override
                public void changed(ObservableValue<? extends Boolean> observable,
                      Boolean oldValue, Boolean newValue ){
                    if (!textField.isFocused()){
                        if (!textField.getText().isEmpty()){
                            String name = textField.getText().trim();
                            if (name.length() > 0){
                                f.setSampleName(name);
                            }
                            saveProject();
                        }
                    }
                }
            });
            vbox.getChildren().add(textField);
            Label noCalls = new Label();
            if (f.getPercentNoCall() != null){
                noCalls.setText("No Calls: "+ DecimalFormat.getInstance().format(f.getPercentNoCall()) + " %");
            }else{
                noCalls.setText("No Calls: none");
            }
            Label meanQual = new Label();
            if (f.getMeanQuality() != null){
                meanQual.setText("Av. Call Conf: " + DecimalFormat.getInstance().format(100 - 
                        (f.getMeanQuality() * 100)) + " %");
            }else{
                meanQual.setText("No Call Confidence Data");
            }
            vbox.getChildren().add(noCalls);
            vbox.getChildren().add(meanQual);
            labelPane.setContent(vbox);
//            labelPane.getChildren().add(fileLabel);
//            labelPane.getChildren().add(new TextField());
            labelsToAdd.add(labelPane);
        }
        for (final SnpFile f: unFiles){
            Pane sPane = new Pane();
            sPane.setMinHeight(chromSplitPane.getHeight()/totalFiles);
            sPane.setMinWidth(chromSplitPane.getWidth());
            sPane.setVisible(true);
            panesToAdd.add(sPane);
            ScrollPane labelPane = new ScrollPane();
            Label fileLabel = new Label(f.inputFile.getName() + "\n(Unaffected)");
            labelPane.setMinHeight(labelSplitPane.getHeight()/totalFiles);
            labelPane.setPrefWidth(labelSplitPane.getWidth());
            labelPane.minHeightProperty().bind(labelSplitPane.heightProperty().divide(totalFiles));
            VBox vbox = new VBox();
            vbox.setSpacing(10);
            vbox.getChildren().add(fileLabel);
            final TextField textField = new TextField();
            textField.setPromptText("Sample Name");
            if (f.getSampleName() != null){
                textField.setText(f.getSampleName());
            }
            textField.setFocusTraversable(true);
            textField.setOnKeyPressed(new EventHandler<KeyEvent>(){
                @Override
                public void handle (KeyEvent ke){
                    if (ke.getCode().equals(KeyCode.ENTER)){
                        if (!textField.getText().isEmpty()){
                            f.setSampleName(textField.getText());
                            textField.getParent().requestFocus();
                            saveProject();
                        }
                    }
                }
            });
            textField.focusedProperty().addListener(new ChangeListener<Boolean>(){
                @Override
                public void changed(ObservableValue<? extends Boolean> observable,
              Boolean oldValue, Boolean newValue ){
                    if (!textField.isFocused()){
                        if (!textField.getText().isEmpty()){
                            f.setSampleName(textField.getText());
                            saveProject();
                        }
                    }
                }
            });
            vbox.getChildren().add(textField);  
            Label noCalls = new Label();
            if (f.getPercentNoCall() != null){
                noCalls.setText("No Calls: "+ DecimalFormat.getInstance().format
                        (f.getPercentNoCall()) + " %");
            }else{
                noCalls.setText("No Calls: none");
            }
            Label meanQual = new Label();
            if (f.getMeanQuality() != null){
                meanQual.setText("Av. Call Conf: " + DecimalFormat.getInstance().format(100 - 
                        (f.getMeanQuality() * 100)) + " %");
            }else{
                meanQual.setText("No Call Confidence Data");
            }
            vbox.getChildren().add(noCalls);
            vbox.getChildren().add(meanQual);
            labelPane.setContent(vbox);
//            labelPane.getChildren().add(fileLabel);
            labelsToAdd.add(labelPane);
        }
        if (panesToAdd.size() > 0){
            chromSplitPane.getItems().addAll(panesToAdd);
            labelSplitPane.getItems().addAll(labelsToAdd);
            
            ArrayList<SnpFile> bothFiles = new ArrayList<>(affFiles);
            bothFiles.addAll(unFiles);
            final Iterator<SnpFile> fileIter = bothFiles.iterator();
            final Iterator<Pane> paneIter = panesToAdd.iterator();
            SnpFile firstFileToProcess = fileIter.next();
            Pane firstPaneToProcess = paneIter.next();
            String pngPath = null;
            if (qualityFilter != null){
                Integer percent = new Integer(100 - (int) (qualityFilter * 100));
                pngPath = percent.toString();
            }
            drawWithIterator(firstFileToProcess, firstPaneToProcess, pngPath, fileIter, 
                    paneIter, 1, totalFiles, chrom, forceRedraw, chromSplitPane);
        }else{
            setProgressMode(false);
        }
    }//end of refreshView
    
    private void fixStageSize(Stage stage, boolean fix){
        /*provides a fix for annoying bug which means that 
         * setResizable(false) causing windows to grow in size in Windows
         */
        if (fix){
            stage.setMaxHeight(stage.getHeight());
            stage.setMinHeight(stage.getHeight());
            stage.setMaxWidth(stage.getWidth());
            stage.setMinWidth(stage.getWidth());
        }else{
            Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
            stage.setMaxHeight(primaryScreenBounds.getHeight());
            stage.setMinHeight(300);
            stage.setMaxWidth(primaryScreenBounds.getWidth());
            stage.setMinWidth(500);
        }
    }
    
    public void drawWithIterator(final SnpFile sfile, final Pane pane, 
            final String pngPath, final Iterator<SnpFile> sIter, final Iterator<Pane> pIter, 
            final int currentFile, final int totalFiles, final String chrom, 
            final boolean forceRedraw, final SplitPane splitPane){
        drawCoordinatesWithIterator(sfile, pane, pngPath, sIter, pIter, 
            currentFile, totalFiles, chrom, null, null, forceRedraw, splitPane);
    }
    public void drawCoordinatesWithIterator(final SnpFile sfile, final Pane pane, 
            final String pngPath, final Iterator<SnpFile> sIter, final Iterator<Pane> pIter, 
            final int currentFile, final int totalFiles, final String chrom, final Double start,
            final Double end, final boolean forceRedraw, final SplitPane splitPane){
        Stage stage = (Stage) splitPane.getScene().getWindow();
        fixStageSize(stage, true);
        
        //stage.setResizable(false);//we have to disable this when using windows due to a bug (in javafx?)
        File pngFile = new File(sfile.getOutputDirectoryName() + "/" + chrom + ".png");
        if (pngPath != null && pngPath.length() > 0){
            pngFile = new File(sfile.getOutputDirectoryName() + "/" + pngPath + "/" + chrom + ".png");
        }
        if (!forceRedraw && pngFile.exists()){
            try{
                progressBar.progressProperty().unbind();
                progressBar.setProgress((double) currentFile/(double) totalFiles);
                BufferedImage bufferedImage = ImageIO.read(pngFile);
                Image image = SwingFXUtils.toFXImage(bufferedImage, null);
                ImageView chromImage = new ImageView(image);
                //chromImage.setCache(true);
                pane.getChildren().clear();
                pane.getChildren().add(chromImage);
                //pane.setCache(true);
                chromImage.fitWidthProperty().bind(pane.widthProperty());
                chromImage.fitHeightProperty().bind(pane.heightProperty());
                pane.minHeightProperty().bind(splitPane.heightProperty().divide(totalFiles));
                pane.minWidthProperty().bind(splitPane.widthProperty());

                if (sIter.hasNext()){
                    SnpFile nextFile = sIter.next();
                    Pane nextPane = pIter.next();
                    drawCoordinatesWithIterator(nextFile, nextPane, pngPath,
                        sIter, pIter, currentFile + 1, totalFiles, chrom, start, end,
                        forceRedraw, splitPane);
                }else{
                    progressBar.progressProperty().unbind();
                    progressBar.setProgress(0);
                    setProgressMode(false);
                    fixStageSize(stage, false);//for windows only
                    stage.setResizable(true);
                }
            }catch (IOException ex){
                Alert error = new Alert(AlertType.ERROR);
                error.setTitle("SnpViewer");
                error.setHeaderText("Error displaying chromosome image");
                error.setContentText("IO error reading cached image");
                error.showAndWait();
                return;
            }
            
        }else{
        
            final DrawSnpsToPane draw = new DrawSnpsToPane(pane, sfile, chrom, 
                   Colors.aa.value, Colors.bb.value, Colors.ab.value, start, end);

           progressBar.progressProperty().unbind();
           //progressBar.setProgress(0);
           //progressBar.progressProperty().bind(draw.progressProperty());
           progressTitle.setText("Drawing " + currentFile + " of " + totalFiles);
           progressMessage.textProperty().bind(draw.messageProperty());
           cancelButton.setOnAction(new EventHandler<ActionEvent>(){
               @Override
               public void handle(ActionEvent actionEvent){
                    draw.cancel();
                }
           });

           draw.setOnCancelled(new EventHandler<WorkerStateEvent>(){
               @Override
               public void handle (WorkerStateEvent t){
                   
                   progressBar.progressProperty().unbind();
                   progressBar.setProgress(0);
                   progressTitle.setText("Drawing Cancelled");
                   progressMessage.textProperty().unbind();
                   progressMessage.setText("Drawing Cancelled");
                   setProgressMode(false);
                   selectionOverlayPane.getChildren().clear();
                   selectionOverlayPane.getChildren().add(dragSelectRectangle);
                   Stage stage = (Stage) splitPane.getScene().getWindow();
                   stage.setResizable(true);
                   fixStageSize(stage, false);//for windows only
               }
           });
           draw.setOnSucceeded(new EventHandler<WorkerStateEvent>(){
               @Override
               public void handle (WorkerStateEvent t){
                   ArrayList<HashMap<String, Double>> result = (ArrayList<HashMap<String, Double>>) t.getSource().getValue();
                   progressBar.progressProperty().unbind();
                   progressBar.setProgress((double) currentFile/ (2 * (double) totalFiles));
                   progressTitle.setText("");
                   progressMessage.textProperty().unbind();
                   progressMessage.setText("");
                   /*if (pane.getMinHeight() < 200){
                        pane.setMinHeight(200);
                   }
                   if (pane.getMinWidth() < 800){
                       pane.setMinWidth(800);
                   }*/
                   if (result != null){
                       List<Line> lines = drawLinesToPane(pane, result);
                       pane.getChildren().addAll(lines);
                       pane.setVisible(true);
                       convertSampleViewToImage(sfile, pane, chrom, pngPath);
                       /*for (Line l: lines){
                           l.startXProperty().unbind();
                           l.startYProperty().unbind();
                           l.endXProperty().unbind();
                           l.endYProperty().unbind();
                       }*/
                       lines.clear();
                   }
                   progressBar.setProgress((double) currentFile/ (double) totalFiles);
                   pane.minWidthProperty().bind(splitPane.widthProperty());
                   pane.minHeightProperty().bind(splitPane.heightProperty().divide(totalFiles));
                  // pane.setCache(true);
                   if (sIter.hasNext()){
                       SnpFile nextFile = sIter.next();
                       Pane nextPane = pIter.next();
                       drawCoordinatesWithIterator(nextFile, nextPane, pngPath,
                               sIter, pIter, currentFile + 1, totalFiles, chrom,
                               start, end, forceRedraw, splitPane);
                   }else{
                       setProgressMode(false);
                       progressBar.progressProperty().unbind();
                       progressBar.setProgress(0);
                

                       Stage stage = (Stage) splitPane.getScene().getWindow();
                       stage.setResizable(true);
                       fixStageSize(stage, false);//for windows only
                   }
               }
           });
           draw.setOnFailed(new EventHandler<WorkerStateEvent>(){
                @Override
                public void handle(WorkerStateEvent t){
                    draw.reset();
                    progressBar.progressProperty().unbind();
                    progressBar.setProgress(0);
                    progressTitle.setText("ERROR!");
                    progressMessage.textProperty().unbind();
                    progressMessage.setText("Drawing failed!");
                    setProgressMode(false);
                    selectionOverlayPane.getChildren().clear();
                    selectionOverlayPane.getChildren().add(dragSelectRectangle);
               //     Stage stage = (Stage) chromSplitPane.getScene().getWindow();
               //     stage.setResizable(true);
                }

            });
           draw.start();
        }     
    }
    
    public List<Line> drawLinesToPane(Pane pane, final ArrayList<HashMap<String, Double>> linemap){
        List<Line> lines = new ArrayList();
        for (HashMap<String, Double> l: linemap){
            double width = pane.getWidth();
            double coord = l.get("x");
            Line line = new Line(l.get("x"), 0, l.get("x"), pane.getMinHeight());
            line.setStroke(colorComp.get(l.get("color").intValue()));
            /*line.startXProperty().bind((pane.widthProperty().divide(width)).multiply(coord) );
            line.endXProperty().bind((pane.widthProperty().divide(width)).multiply(coord) );
            line.endYProperty().bind(pane.heightProperty().subtract(1));*/
            lines.add(line);
            //pane.getChildren().add(l);
        }
        return lines;
        
    }
    
    public void convertSampleViewToImage(final SnpFile s, final Pane pane, 
            final String chrom, final String path){
        WritableImage image;
        try{
            image = pane.snapshot(null, null);
        }catch(IllegalStateException ex){
            Alert error = new Alert(AlertType.ERROR);
            error.setTitle("SnpViewer");
            error.setHeaderText("PNG conversion failed");
            error.setContentText("Error while attempting to convert"
                    + " view to image file");
            error.showAndWait();
            return;
        }

        final DrawPaneToPng drawToPng = new DrawPaneToPng(image);
        drawToPng.setOnSucceeded(new EventHandler<WorkerStateEvent>(){
            @Override
            public void handle(WorkerStateEvent t){
                File pngFile;
                if (path != null && path.length() > 0){
                    pngFile = new File(s.getOutputDirectoryName() + "/" + path + "/" + chrom + ".png");
                }else{
                    pngFile = new File(s.getOutputDirectoryName() + "/" + chrom + ".png");
                }
                try{
                    if (!pngFile.getParentFile().exists()){
                        boolean madeDir = pngFile.getParentFile().mkdir();
                        if (madeDir == false){
                            Alert error = new Alert(AlertType.ERROR);
                            error.setTitle("SnpViewer");
                            error.setHeaderText("PNG conversion failed");
                            error.setContentText("Unable to make sub directory"
                            + " when converting dynamic view to image file. Please "
                                    + "check permissions.");
                            error.showAndWait();
                            return;
                        }
                    }
                    Files.copy(drawToPng.getImageFile().toPath(), pngFile.toPath(), REPLACE_EXISTING);
                    BufferedImage bufferedImage = ImageIO.read(pngFile);
                    Image image = SwingFXUtils.toFXImage(bufferedImage, null);
                    
                    ImageView chromImage = new ImageView(image);
                   // chromImage.setCache(true);
                    for (Iterator it = pane.getChildren().iterator(); it.hasNext();){
                        Object line = it.next();
                        if (line instanceof Line){
                            /*Line l = (Line) line;
                            l.startXProperty().unbind();
                            l.endXProperty().unbind();
                            l.endYProperty().unbind();*/
                        }else if (line instanceof ImageView){
                            ImageView l = (ImageView) line;
                            l.fitHeightProperty().unbind();
                            l.fitWidthProperty().unbind();
                        }
                    }
                    pane.getChildren().clear();
                    pane.getChildren().add(chromImage);
                    
                    chromImage.fitWidthProperty().bind(pane.widthProperty());
                    chromImage.fitHeightProperty().bind(pane.heightProperty());
               }catch (IOException ex){
                    Alert error = new Alert(AlertType.ERROR);
                    error.setTitle("SnpViewer");
                    error.setHeaderText("PNG conversion failed");
                    error.setContentText("IOException while attempting to convert"
                            + " dynamic view to image file");
                    error.showAndWait();
               }
            }
        });
        drawToPng.setOnFailed(new EventHandler<WorkerStateEvent>(){
            @Override
            public void handle(WorkerStateEvent t){
                Alert error = new Alert(AlertType.ERROR);
                error.setTitle("SnpViewer");
                error.setHeaderText("PNG conversion failed");
                error.setContentText("Error encountered while attempting to convert"
                        + " dynamic view to image file");
                error.showAndWait();
            }
        });
        drawToPng.start();
    }//end of convertSampleViewToImage
    
    public void drawPaneToPng(){
        if (chromosomeSelector.getSelectionModel().isEmpty()){
            return;
        }
        FileChooser fileChooser = new FileChooser();
        if (projectFile != null){
            fileChooser.setInitialDirectory(projectFile.getParentFile());
        }else{
            fileChooser.setInitialDirectory(new File(getProperty("user.home")));
        }
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "PNG Image Files (*.png)", "*.png");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setTitle("Save View as Image (.png) File...");
        File pngFile = fileChooser.showSaveDialog(mainWindow);
        if (pngFile == null){
            return;
        }else if (!pngFile.getName().endsWith(".png")){
            pngFile = new File(pngFile.getAbsolutePath() + ".png");
        }
        WritableImage image = chromSplitPane.snapshot(null, null);
        if (image == null){
            Alert error = new Alert(AlertType.ERROR);
            error.setTitle("SnpViewer");
            error.setHeaderText("PNG conversion failed");
            error.setContentText("Error encountered while attempting to convert"
                    + " dynamic view to image file");
            error.showAndWait();
            return;
        }
        try{
            ImageIO.write(SwingFXUtils.fromFXImage(image, null),
                    "png", pngFile);
            Alert info = new Alert(AlertType.INFORMATION);
            info.getDialogPane().setPrefSize(420, 200);
            info.setResizable(true);
            info.setTitle("SnpViewer");
            info.setHeaderText("Image Saved");
            info.setContentText("Sucessfully saved current view "
                    + "to " + pngFile.getName());
            info.showAndWait();
        } catch (IOException ex) {
            Alert error = new Alert(AlertType.ERROR);
            error.setTitle("SnpViewer");
            error.getDialogPane().setPrefSize(420, 200);
            error.setResizable(true);
            error.setHeaderText("PNG conversion failed");
            error.setContentText("Error encountered while attempting to convert"
                    + " dynamic view to image file.\n" 
                    + ex.getLocalizedMessage());
            error.showAndWait();
        }
    }
    
    public void newProjectFired(ActionEvent event){
        newProjectButton.setDisable(true);
        loadProjectButton.setDisable(true);
        if (projectRunning){
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.getDialogPane().setPrefSize(420, 200);
            alert.setResizable(true);
            ButtonType yButton = ButtonType.YES;
            ButtonType nButton = ButtonType.NO;
            alert.getButtonTypes().setAll(yButton, nButton);
            alert.setTitle("SnpViewer");
            alert.setHeaderText("Close Current Project?");
            alert.setContentText("Your current project will be closed. "
                    + "Its data will be saved.");
            Optional<ButtonType> response = alert.showAndWait();
            if (response.get() != yButton){
                newProjectButton.setDisable(false);
                loadProjectButton.setDisable(false);
                return;
            }
        }
        boolean success = startNewProject();
        newProjectButton.setDisable(false);
        loadProjectButton.setDisable(false);
        if (success){
            projectRunning = true;
            Alert info = new Alert (AlertType.INFORMATION);
            info.getDialogPane().setPrefSize(420, 250);
            info.setResizable(true);
            info.setTitle("SnpViewer");
            info.setHeaderText("Project " + projectFile.getName() + " Created");
            info.setContentText("Your project will automatically"
                    + " save itself as you make changes.");
            setProgressMode(false);
        }else{
            projectLabel.setText("Project: none");
            Alert error = new Alert(AlertType.ERROR);
            error.getDialogPane().setPrefSize(420, 200);
            error.setResizable(true);
            error.setTitle("SnpViewer");
            error.setHeaderText("Project Creation Failed");
            error.setContentText( "Could not create new project");
            error.showAndWait();
            projectRunning = false;
        }
       
    }
    
    public void closeButtonFired(ActionEvent ev){
        
        Stage stage = (Stage) mainMenu.getScene().getWindow();
        stage.close();


    }
    
    public boolean startNewProject(){
        affFiles.clear();
        unFiles.clear();
        resetObservables();
        genomeVersion = "";
        qualityFilter = null;
        noFilteringRadio.setSelected(true);
        chromSplitPane.getItems().clear();
        labelSplitPane.getItems().clear();
        chromosomeSelector.getItems().clear();
        snpViewSaveDirectory = null;
        clearDragSelectRectangle();
        savedRegions.clear();
        savedRegionsDisplay.clear();
        savedRegionsReference.clear();
        selectionOverlayPane.getChildren().clear();
        selectionOverlayPane.getChildren().add(dragSelectRectangle);

        if (projectRunning){
            setProgressMode(true);
        }
        FileChooser fileChooser = new FileChooser();
        if (projectFile != null){
            fileChooser.setInitialDirectory(projectFile.getParentFile());
        }else{
            fileChooser.setInitialDirectory(new File(getProperty("user.home")));
        }
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("SNP Viewer Projects (*.svproj)", "*.svproj");
        fileChooser.getExtensionFilters().add(extFilter);
        fileChooser.setTitle("Save New SNP Viewer (.svproj) Project As...");
        projectFile = fileChooser.showSaveDialog(mainWindow);
        if (projectFile != null){
            if (!projectFile.getName().endsWith(".svproj")){
                projectFile = new File(projectFile.getAbsolutePath() + ".svproj");
            }
            try{
               String projectName = projectFile.getName().replaceAll(".svproj", "");
               File snpViewDir = new File(projectFile.getParentFile() + "/" 
                       + projectName + " SNP Viewer files");
               if (snpViewDir.exists()){
                   FileUtils.deleteDirectory(snpViewDir);
               }
               boolean madeDir = snpViewDir.mkdir();
               if (madeDir){
                    snpViewSaveDirectory = snpViewDir;
               }else{
                    //display error?
                    return false;
                }   
           } catch(Exception ex){
               Alert error = new Alert(AlertType.ERROR);
               error.getDialogPane().setPrefSize(420, 200);
               error.setResizable(true);
               error.setTitle("SnpViewer");
               error.setContentText("Project Directory Creation Failed " + 
                       ex.getLocalizedMessage());
               error.setHeaderText("Error Creating New Project");
               ex.printStackTrace();
               error.showAndWait();
           }
           return saveProject(projectFile);
        }else{
            resetView();
            return false;
        }
        
    }
    
        
    public void windowResized (ActionEvent event){
        if (dragSelectRectangle.getWidth() > 0){
            dragSelectRectangle.setX(0);
            dragSelectRectX.set(0);
            dragSelectRectInitX.set(0);
            dragSelectRectangle.setVisible(false);
        }
    }
    
    public void saveButtonFired (ActionEvent event){
       boolean success = saveProject();
       if (success){
           Alert info = new Alert(AlertType.INFORMATION);
           info.setResizable(true);
           info.setTitle("SnpViewer");
           info.setHeaderText("Save Successful");
           info.setContentText(projectFile.getName() + " saved sucessfully");
           info.showAndWait();
        }
   }
    
    public void affButtonFired (ActionEvent event){
       addInputFiles(true);
   }
    
    public void unButtonFired (ActionEvent event){
        addInputFiles(false);
   }
    
    public void redrawButtonFired (ActionEvent event){
        refreshView((String) chromosomeSelector.getSelectionModel().getSelectedItem(),
                true);
    }
    
    public void clearSavedRegionsFired(ActionEvent event){
        clearSavedRegions();
    }
    
    public void removeSamples(ActionEvent event){
        FXMLLoader loader = new FXMLLoader(getClass().
               getResource("RemoveSamplesInterface.fxml"));
       try{
            Pane pane = (Pane) loader.load();
            RemoveSamplesInterfaceController removeController = 
                    (RemoveSamplesInterfaceController) loader.getController();
            Scene scene = new Scene(pane);
            Stage stage = new Stage();
            stage.setScene(scene);
            //scene.getStylesheets().add(SnpViewer.class
//.getResource("SnpViewerStyleSheet.css").toExternalForm());
            stage.getIcons().add(new Image(this.getClass().
                    getResourceAsStream("icon.png")));
            stage.setTitle("Remove Samples");
            removeController.setSamples(affObserve, unObserve);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            List<Integer> indicesToRemove = removeController.getSamplesToRemove();
            //System.out.println(indicesToRemove.toString());
            if (indicesToRemove.isEmpty()){
                return;
            }
            ArrayList<Integer> affsToRemove = new ArrayList<>();
            ArrayList<Integer> unsToRemove = new ArrayList<>();
            for (Integer r: indicesToRemove){
                if (r < affObserve.size()){//index corresponds to affFiles
                    affsToRemove.add(r);
                }else{//index corresponds to unFiles
                    r -= affObserve.size();
                    unsToRemove.add(r);
                }
            }
            ArrayList<File> dirsToDelete = new ArrayList<>();
            if (!affsToRemove.isEmpty()){
                Collections.sort(affsToRemove, Collections.reverseOrder());
                for (int i: affsToRemove){
                    dirsToDelete.add(affObserve.get(i).getOutputDirectory());
                    affObserve.remove(i);
                }
            }
            if (!unsToRemove.isEmpty()){
                Collections.sort(unsToRemove, Collections.reverseOrder());
                for (int i: unsToRemove){
                    dirsToDelete.add(unObserve.get(i).getOutputDirectory());
                    unObserve.remove(i);
                }
            }
            if(affObserve.isEmpty() && unObserve.isEmpty()){
                resetView();
            }else{
                refreshView(null, false);
            }
            saveProject();
            for (File dir: dirsToDelete){
                FileUtils.deleteDirectory(dir);                    
            }
       }catch(Exception ex){
            Alert error = new Alert(AlertType.ERROR);
            error.getDialogPane().setPrefSize(420, 200);
            error.setResizable(true);
            error.setTitle("SnpViewer");
            error.setHeaderText("Remove Samples Failed!");
            error.setContentText(ex.getLocalizedMessage());
            ex.printStackTrace();
            error.showAndWait();
       }
    }
    
    private void resetView(){
        chromSplitPane.getItems().clear();
        labelSplitPane.getItems().clear();
        newProjectMenu.setDisable(false);
        loadProjectMenu.setDisable(false);
        addAffected.setDisable(false);
        addUnaffected.setDisable(false);
        addAffSampleMenu.setDisable(false);
        addUnSampleMenu.setDisable(false);
        loadProjectButton.setDisable(false);
        newProjectButton.setDisable(false);
        saveToPngMenu.setDisable(true);
        chromosomeSelector.setDisable(true);
        redrawButton.setDisable(true);
        colorPicker.setDisable(true);
        colorComponantSelector.setDisable(true);
        cacheChromsButton.setDisable(true);
        
        cacheChromsMenu.setDisable(true);
        nextChromMenu.setDisable(true);
        prevChromMenu.setDisable(true);
        firstChromMenu.setDisable(true);
        lastChromMenu.setDisable(true);
        redrawMenu.setDisable(true);
        autoFindRegions.setDisable(true);
        displaySavedsRegionsMenu.setDisable(true);
        clearSavedRegionsMenu.setDisable(true);
        hideSavedRegionsMenu.setDisable(true);
        outputSavedRegionsMenu.setDisable(true);
        findRegionsButton.setDisable(true);
        removeSampleMenu.setDisable(true);
        noFilteringRadio.setDisable(true);
        filter99pt9.setDisable(true);
        filter99pt5.setDisable(true);
        filter99.setDisable(true);
        filter95.setDisable(true);
        filter90.setDisable(true);
        cancelButton.setDisable(true);
    }
    
    public void addInputFiles (final boolean isAffected){
        setProgressMode(true);
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(lastLoadedDir);
       fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt", "AutoSNPa Files", "*.xls"));
       fileChooser.setTitle("Select one or more input (birdseed) files");
       List<File> chosen = fileChooser.showOpenMultipleDialog(null);
       if (chosen == null){
           setProgressMode(false);
           return ;
       }
       cancelButton.setOnAction(new EventHandler<ActionEvent>(){
           @Override
           public void handle(ActionEvent actionEvent){
                setProgressMode(false);
           };
       });
       ArrayList<File> inputFiles = new ArrayList<>();
       inputFiles.addAll(chosen);
       ArrayList<File> duplicates =  new ArrayList<>();
       ArrayList<Integer> indicesToRemove =  new ArrayList<>();
       for (int i = 0; i < inputFiles.size(); i++){
           for (SnpFile s: affFiles){
               if (inputFiles.get(i).getName().equals(s.getInputFileName())){
                   duplicates.add(inputFiles.get(i));
                   indicesToRemove.add(i);
               }
           }
           for (SnpFile s: unFiles){
               if (inputFiles.get(i).getName().equals(s.getInputFileName())){
                   duplicates.add(inputFiles.get(i));
                   indicesToRemove.add(i);
               }
           }
       }
       if (!duplicates.isEmpty()){
           StringBuilder duplicateString = new StringBuilder();
           for (File d: duplicates){
               duplicateString.append(d.getName()).append("\n");
           }
           Collections.sort(indicesToRemove, Collections.reverseOrder());
           for (int i: indicesToRemove){
               inputFiles.remove(i);
           }
            Alert alert = new Alert(AlertType.CONFIRMATION);
            alert.getDialogPane().setPrefSize(420, 250);
            alert.setResizable(true);
            ButtonType okButton = ButtonType.OK;
            ButtonType cButton = ButtonType.CANCEL;
            alert.getButtonTypes().setAll(okButton, cButton);
            alert.setTitle("SnpViewer");
            alert.setHeaderText("Duplicate Input File");
            alert.setContentText("This project already includes the following file(s) with "
                   + "matching names to the file(s) you have just tried to add:"
                   + "\n\n" + duplicateString + "\nIf you want to change the affected "
                   + "status of a file please remove it first.  Any remaining "
                   + "(non-duplicate) files will be processed if you click 'OK'.");
            Optional<ButtonType> response = alert.showAndWait();
            if (response.get() != okButton){
               setProgressMode(false);
               return;
            }
       }
       if (inputFiles.isEmpty()){
           setProgressMode(false);
           return ;
       }
       final Iterator iter = inputFiles.iterator();
       File input = (File) iter.next();
       lastLoadedDir = input.getParentFile();
       int fileCounter = 1;
       if (snpViewSaveDirectory == null){
           Alert warn = new Alert(AlertType.WARNING);
           warn.setResizable(true);
           warn.setTitle("SnpViewer");
           warn.setHeaderText("Create Project");
           warn.setContentText("Before processing input files "
                    + "please create a project");
           warn.showAndWait();

           boolean success = startNewProject();

           if (!success){
                setProgressMode(false);
                return ;
           }
       }

       addInputFilesWithIterator(isAffected, input, iter, fileCounter, inputFiles.size());
    }
    
    private void addInputFilesWithIterator(final boolean isAffected, final File input, final Iterator it, final int fileCounter, final int totalFiles){
        try{
            String subDir = input.getName().replaceFirst("[.][^.]+$", "");
            File outputDirectory = new File(snpViewSaveDirectory +"/" + subDir);
            try{
                outputDirectory.mkdir();
            }catch(Exception ex){
                ex.printStackTrace();
                setProgressMode(false);
                return ;
            }
            final SnpFile snpFile = new SnpFile(input, outputDirectory);
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
            progressBar.progressProperty().bind(snpFile.progressProperty());
            progressTitle.setText("Processing " + fileCounter + " of " + totalFiles);
            progressMessage.textProperty().bind(snpFile.messageProperty());
            cancelButton.setOnAction(new EventHandler<ActionEvent>(){
                @Override
                public void handle(ActionEvent actionEvent){
                    snpFile.cancel();
                    progressBar.progressProperty().unbind();
                    progressBar.setProgress(0);
                     progressTitle.setText("Cancelled");
                     progressMessage.textProperty().unbind();
                     progressMessage.setText("Cancelled");
                     setProgressMode(false);

                }
            });
            snpFile.start();
            snpFile.setOnSucceeded (new EventHandler<WorkerStateEvent>(){
                @Override
                public void handle(WorkerStateEvent t){
                    if (snpFile.buildVersion == null){//need to manually identify snp build for autoSNPa files
                        BuildInferrer buildInferrer = new BuildInferrer();
                        String build = buildInferrer.inferBuild(snpFile);
                        if (build == null){
                            Alert error = new Alert(AlertType.ERROR);
                            error.getDialogPane().setPrefSize(420, 250);
                            error.setResizable(true);
                            error.setTitle("SnpViewer");
                            error.setHeaderText("Failure while adding input file.");
                            error.setContentText( "Failed to process file " 
                                    + input.getName() + ".  Could not determine "
                                    + "genome build. Only hg19 and hg18 builds "
                                    + "are supported and in the absence of a "
                                    + "header containing build information only "
                                    + "Affymetrix Genome-Wide Human SNP Array 5.0"
                                    + " or 6.0 chips are supported.");
                            error.showAndWait();
                            setProgressMode(false);
                            saveProject();
                            return;
                        }else{
                            snpFile.setBuildVersion(build);
                        }
                    }
                    progressBar.progressProperty().unbind();
                    progressBar.setProgress(0);
                    progressTitle.setText("");
                    progressMessage.textProperty().unbind();
                    progressMessage.setText("");
                    if (genomeVersion.equals("")){
                        genomeVersion = snpFile.getBuildVersion();
                        buildLabel.setText(genomeVersion);
                    }else{
                        if (! snpFile.getBuildVersion().equalsIgnoreCase(genomeVersion)){
                            progressTitle.setText("");
                            progressMessage.setText("");
                            Alert error = new Alert(AlertType.ERROR);
                            error.setResizable(true);
                            error.setTitle("SnpViewer");
                            error.setHeaderText("Genome Version Error");
                            error.setContentText( "Genome versions do "
                                    + "not match between input files. Error while"
                                    + " adding input file " + input.getName());
                            error.showAndWait();
                            setProgressMode(false);
                            saveProject();
                            return;
                        }
                    }
                    if (isAffected){
                        affObserve.add(snpFile);    
                    }else{
                        unObserve.add(snpFile);
                    }

                    if (it.hasNext()){
                        addInputFilesWithIterator(isAffected, (File) it.next(), it, fileCounter + 1, totalFiles);
                    }else{
                        setProgressMode(false);
                        saveProject();
                        refreshView(null, redrawCheckBox.isSelected());
                    }
                }
            });
            final String fileNameForFailure = input.getName() ;
            snpFile.setOnFailed(new EventHandler<WorkerStateEvent>(){
                @Override
                public void handle(WorkerStateEvent t){
                    progressBar.progressProperty().unbind();
                    progressBar.setProgress(0);
                    progressTitle.setText("");
                    progressMessage.textProperty().unbind();
                   // progressMessage.setText("");
                    Alert error = new Alert(AlertType.ERROR);
                    error.getDialogPane().setPrefSize(420, 220);
                    error.setResizable(true);
                    error.setTitle("SnpViewer");
                    error.setHeaderText("Failed to process file " 
                            + fileNameForFailure);
                    error.setContentText(t.getSource().getException().getLocalizedMessage() ); 
                    t.getSource().getException().printStackTrace();
                    error.showAndWait();
                    setProgressMode(false);
                    saveProject();
                }

            });
            snpFile.setOnCancelled(new EventHandler<WorkerStateEvent>(){
                @Override
                public void handle(WorkerStateEvent t){
                    progressBar.progressProperty().unbind();
                    progressBar.setProgress(0);
                    progressTitle.setText("ERROR!");
                    progressMessage.textProperty().unbind();
                    progressMessage.setText("Processing of " + fileNameForFailure + " failed!");
                    setProgressMode(false);
                    saveProject();
                }

            });

        }catch (Exception ex) {
            ex.printStackTrace();
            //display error here
        }      
    }
    
    private void setProgressMode(boolean running){
        /*simple method to put UI into one state when a process is running
         * and another when it is idle
         */
        if (! running){
            progressMode = false;
            //addAffected.setDefaultButton(true);
            //addAffected.requestFocus();
            addAffected.setDisable(false);
            addUnaffected.setDisable(false);
            chromosomeSelector.setDisable(false);
            loadProjectButton.setDisable(false);
            newProjectButton.setDisable(false);
            //saveProjectButton.setDisable(false);
            redrawButton.setDisable(false);
            colorPicker.setDisable(false);
            colorComponantSelector.setDisable(false);
            selectionIndicator.setDisable(false);
            cacheChromsButton.setDisable(false);
            newProjectMenu.setDisable(false);
            loadProjectMenu.setDisable(false);
            cacheChromsMenu.setDisable(false);
            saveToPngMenu.setDisable(false);
            addAffSampleMenu.setDisable(false);
            addUnSampleMenu.setDisable(false);
            nextChromMenu.setDisable(false);
            prevChromMenu.setDisable(false);
            firstChromMenu.setDisable(false);
            lastChromMenu.setDisable(false);
            redrawMenu.setDisable(false);
            autoFindRegions.setDisable(false);
            displaySavedsRegionsMenu.setDisable(false);
            clearSavedRegionsMenu.setDisable(false);
            hideSavedRegionsMenu.setDisable(false);
            outputSavedRegionsMenu.setDisable(false);
            findRegionsButton.setDisable(false);
            removeSampleMenu.setDisable(false);
            noFilteringRadio.setDisable(false);
            filter99pt9.setDisable(false);
            filter99pt5.setDisable(false);
            filter99.setDisable(false);
            filter95.setDisable(false);
            filter90.setDisable(false);
            cancelButton.setDisable(true);
        }else{
            progressMode = true;
            addAffected.setDisable(true);
            addUnaffected.setDisable(true);
            chromosomeSelector.setDisable(true);
            loadProjectButton.setDisable(true);
            newProjectButton.setDisable(true);
            saveToPngMenu.setDisable(true);
            //saveProjectButton.setDisable(true);
            redrawButton.setDisable(true);
            colorPicker.setDisable(true);
            colorComponantSelector.setDisable(true);
            cacheChromsButton.setDisable(true);
            newProjectMenu.setDisable(true);
            loadProjectMenu.setDisable(true);
            cacheChromsMenu.setDisable(true);
            addAffSampleMenu.setDisable(true);
            addUnSampleMenu.setDisable(true);
            nextChromMenu.setDisable(true);
            prevChromMenu.setDisable(true);
            firstChromMenu.setDisable(true);
            lastChromMenu.setDisable(true);
            redrawMenu.setDisable(true);
            autoFindRegions.setDisable(true);
            displaySavedsRegionsMenu.setDisable(true);
            clearSavedRegionsMenu.setDisable(true);
            hideSavedRegionsMenu.setDisable(true);
            outputSavedRegionsMenu.setDisable(true);
            findRegionsButton.setDisable(true);
            removeSampleMenu.setDisable(true);
            noFilteringRadio.setDisable(true);
            filter99pt9.setDisable(true);
            filter99pt5.setDisable(true);
            filter99.setDisable(true);
            filter95.setDisable(true);
            filter90.setDisable(true);
            cancelButton.setDisable(false);
        }
    } 
    
    public void addToChromosomeSelector(ArrayList<SnpFile> sFiles){

           for (SnpFile f: sFiles){
               List<String> chromsToAdd = new ArrayList<>();
               for (String c: f.chromFiles.keySet()){
                   if (! chromosomeSelector.getItems().contains(c)){
                       chromsToAdd.add(c);
                   }
               }
               ChromComparator chromCompare = new ChromComparator();
               java.util.Collections.sort(chromsToAdd, chromCompare);
               chromosomeSelector.getItems().addAll(chromsToAdd);
               
               if (chromosomeSelector.getSelectionModel().isEmpty()){
                   //chromosomeSelector.getSelectionModel().selectFirst();
               }else{
                   int sel = chromosomeSelector.getSelectionModel().getSelectedIndex();
                   //chromosomeSelector.getSelectionModel().clearSelection();
                   //chromosomeSelector.getSelectionModel().select(sel);
               }
               chromosomeSelector.requestFocus();
           }
    }
    public void recheckChromosomeSelector(List<SnpFile> sFiles){
        /*in case a SnpFile has been removed we clear the menu first
         */
                            
        chromosomeSelector.getItems().clear();
           for (SnpFile f: sFiles){
               List<String> chromsToAdd = new ArrayList<>();
               for (String c: f.chromFiles.keySet()){
                   if (! chromosomeSelector.getItems().contains(c)){
                       chromsToAdd.add(c);
                   }
               }
               ChromComparator chromCompare = new ChromComparator();
               java.util.Collections.sort(chromsToAdd, chromCompare);
               chromosomeSelector.getItems().addAll(chromsToAdd);
               
               if (chromosomeSelector.getSelectionModel().isEmpty()){
                   //chromosomeSelector.getSelectionModel().selectFirst();
               }else{
                   int sel = chromosomeSelector.getSelectionModel().getSelectedIndex();
                   //chromosomeSelector.getSelectionModel().clearSelection();
                   //chromosomeSelector.getSelectionModel().select(sel);
               }
               chromosomeSelector.requestFocus();
           }
    }
    
    public boolean saveProject(){
        if (projectFile != null){
            return saveProject(projectFile);
        }else{
            FileChooser fileChooser = new FileChooser();
            if (projectFile != null){
                fileChooser.setInitialDirectory(projectFile.getParentFile());
            }else{
                fileChooser.setInitialDirectory(new File(getProperty("user.home")));
            }
            fileChooser.setTitle("Save Snp Viewer Project (.svproj) As...");
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("SNP Viewer Projects", "*.svproj");
            fileChooser.getExtensionFilters().add(extFilter);
            projectFile = fileChooser.showSaveDialog(mainWindow);
            if (projectFile != null){
                if (!projectFile.getName().endsWith(".svproj")){
                    projectFile = new File(projectFile.getAbsolutePath() + ".svproj");
                }
                return saveProject(projectFile);
                
            }else{
                return false;
            }
        }
    }
    
    
    public boolean saveProject(File saveFile){
        try{
            FileOutputStream fos = new FileOutputStream(saveFile);
            try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(fos))) {
                out.writeObject(projectFile);
                out.writeObject(affFiles);
                out.writeObject(unFiles);
                out.writeObject(genomeVersion);
                out.writeObject(qualityFilter);
                out.writeObject(snpViewSaveDirectory);
                out.writeObject(savedRegions);
                for (Color c: colorComp){
                    out.writeObject(c.toString());
                }
                
                out.close();
                projectLabel.setText("Project: " +projectFile.getName());
            }
            return true;
        }catch (IOException ex){
            //ex.printStackTrace();
            Alert error = new Alert(AlertType.ERROR);
            error.getDialogPane().setPrefSize(420, 200);
            error.setResizable(true);
            error.setTitle("SnpViewer");
            error.setHeaderText("Save Failed");
            error.setContentText("Could not save project file - IO error.\n" + 
                    ex.getLocalizedMessage()); 
            error.showAndWait();
            return false;
        }
    }
    
    public void loadProject(){
        if (projectRunning){
            /*setProgressMode(true);
            DialogResponse response = Dialogs.showConfirmDialog(null,
            "Do you want to save your current project before starting a new one?", 
                    "Save Current Project?", "SNP View");
            if (DialogResponse.YES.equals(response)){
                boolean saved = saveProject();
                if (! saved){
                    setProgressMode(false);
                    return;
                }else{
                    Dialogs.showInformationDialog(null, projectFile.getName() + " saved sucessfully", 
                            "Save Successful", "SNP View");
                }
            }else if (DialogResponse.CANCEL.equals(response)){
                setProgressMode(false);
                return;
            }*/
        }
        loadProjectButton.setDisable(true);
        newProjectButton.setDisable(true);
        FileChooser fileChooser = new FileChooser();
        if (projectFile != null){
            fileChooser.setInitialDirectory(projectFile.getParentFile());
        }else{
            fileChooser.setInitialDirectory(new File(getProperty("user.home")));
        }
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SNP Viewer Projects", "*.svproj"));
        fileChooser.setTitle("Open SNP Viewer Project (.svproj) file");
        //setProgressMode(false);
        File loadFile = fileChooser.showOpenDialog(mainWindow);
        loadProjectButton.setDisable(false);
        newProjectButton.setDisable(false);
        if (loadFile != null){
            try{
                ObjectInputStream is = new ObjectInputStream(new BufferedInputStream
                        (new FileInputStream(loadFile)));
                try{
                    projectFile = null;
                    projectLabel.setText("Project: none");
                    chromSplitPane.getItems().clear();
                    labelSplitPane.getItems().clear();
                    clearDragSelectRectangle();
                    savedRegions.clear();
                    savedRegionsDisplay.clear();
                    savedRegionsReference.clear();
                    selectionOverlayPane.getChildren().clear();
                    selectionOverlayPane.getChildren().add(dragSelectRectangle);
                    
                    resetObservables();
                    genomeVersion = "";
                    qualityFilter = null;
                    noFilteringRadio.setSelected(true);
                    chromosomeSelector.getItems().clear();
                    projectFile = (File) is.readObject();//get rid of this?
                    projectFile = loadFile;//allow relative referencing
                    
                    ArrayList<Color> loadedColors = new ArrayList<>();
                    ArrayList<SnpFile> tempAff = new ArrayList<>();
                    ArrayList<SnpFile> tempUn = new ArrayList<>();
                    tempAff.addAll((ArrayList<SnpFile>) is.readObject());
                    tempUn.addAll((ArrayList<SnpFile>) is.readObject());
                    ArrayList<SnpFile> tempBoth = new ArrayList<>();
                    tempBoth.addAll(tempAff);
                    tempBoth.addAll(tempUn);
                    genomeVersion = (String) is.readObject();
                    qualityFilter = (Double) is.readObject();
                    buildLabel.setText(genomeVersion);
                    snpViewSaveDirectory = (File) is.readObject();
                    savedRegions =  (ArrayList<RegionSummary>) is.readObject();
                    String projectName = projectFile.getName().replaceAll(".svproj", "");
                    if (!snpViewSaveDirectory.exists()){
                        snpViewSaveDirectory = new File(projectFile.getParentFile() + "/" + projectName + " SNP Viewer files");
                        if (!snpViewSaveDirectory.exists()){
                            Alert alert = new Alert(AlertType.CONFIRMATION);
                            alert.getDialogPane().setPrefSize(420, 220);
                            alert.setResizable(true);
                            ButtonType yButton = ButtonType.YES;
                            ButtonType nButton = ButtonType.NO;
                            alert.getButtonTypes().setAll(yButton, nButton);
                            alert.setTitle("SnpViewer");
                            alert.setHeaderText("Project directory not found");
                            alert.setContentText("Can't find project directory (" + 
                                    snpViewSaveDirectory.getName() + ") - do you "
                                    + "want to try to find it?");
                            Optional<ButtonType> response = alert.showAndWait();
                            if (response.get() == yButton){
                                DirectoryChooser dirChooser = new DirectoryChooser();
                                dirChooser.setTitle("Locate Project Folder");
                                snpViewSaveDirectory = dirChooser.showDialog(mainWindow);
                                if (snpViewSaveDirectory == null){
                                    returnToInitialState();
                                    return;
                                }
                            }else{
                                returnToInitialState();
                                return;
                            }
                        }
                    }
                    boolean check = checkProjectFolder(snpViewSaveDirectory, tempBoth);
                    if (!check){
                        Alert error = new Alert(AlertType.ERROR);
                        error.setResizable(true);
                        error.setTitle("SnpViewer");
                        error.setHeaderText("Corrupt project");
                        error.setContentText("Corrupt project"
                                + " folder - missing files."); 
                        error.showAndWait();
                        returnToInitialState();
                        return;
                    }
                    for (Color c: colorComp){
                        String colorString = (String) is.readObject();
                        loadedColors.add(Color.valueOf(colorString));
                    }
                    for (int ci = 0; ci < loadedColors.size(); ci++){
                        colorComponantSelector.getSelectionModel().clearAndSelect(ci);
                        colorPicker.setValue(loadedColors.get(ci));
                        colorPicker.fireEvent(new ActionEvent());
                    }
                    affObserve.addAll(tempAff);
                    unObserve.addAll(tempUn);
                    colorComponantSelector.getSelectionModel().selectFirst();
                    colorComp.clear();
                    colorComp.addAll(loadedColors);
                    is.close();
                    setQualityLabel();
                    checkQualitySelection();
                    saveProject();
                    projectLabel.setText("Project: " + projectFile.getName());
                    //addToChromosomeSelector(affFiles);
                    //addToChromosomeSelector(unFiles);
                    if (!chromosomeSelector.getItems().isEmpty()){
                        chromosomeSelector.getSelectionModel().selectFirst();
                    }
                    if (chromosomeSelector.isDisabled()){
                        chromosomeSelector.setDisable(false);
                    }
                    //setProgressMode(false);
                    if (affObserve.isEmpty() && unObserve.isEmpty()){
                        resetView();
                    }
                    projectRunning = true; 

                }catch(IOException | ClassNotFoundException ex){
                    resetView();
                    projectLabel.setText("Project: none");
                    savedRegions.clear();
                    savedRegionsDisplay.clear();
                    savedRegionsReference.clear();
                    resetObservables();
                    genomeVersion = "";
                    qualityFilter = null;
                    noFilteringRadio.setSelected(true);
                    chromosomeSelector.getItems().clear();
                    ex.printStackTrace();
                    Alert error = new Alert(AlertType.ERROR);
                    error.getDialogPane().setPrefSize(420, 200);
                    error.setResizable(true);
                    error.setTitle("SnpViewer");
                    error.setHeaderText("Load Failed");
                    error.setContentText("Could not load project file.\n" + 
                            ex.getLocalizedMessage());
                    error.showAndWait();
                }
            }catch (IOException ex){
                resetView();
                projectLabel.setText("Project: none");
                savedRegions.clear();
                savedRegionsDisplay.clear();
                savedRegionsReference.clear();
                resetObservables();
                genomeVersion = "";
                qualityFilter = null;
                noFilteringRadio.setSelected(true);
                chromosomeSelector.getItems().clear();
                ex.printStackTrace();
                Alert error = new Alert(AlertType.ERROR);
                error.getDialogPane().setPrefSize(420, 200);
                error.setResizable(true);
                error.setTitle("SnpViewer");
                error.setHeaderText("Load Failed");
                error.setContentText("Could not load project file  - IO error.\n" + 
                        ex.getLocalizedMessage());
                error.showAndWait();
            }
        }       
    }
    
    private void resetObservables(){
        affObserve.clear();
        unObserve.clear();
        affObserve = FXCollections.observableList(affFiles);
        unObserve = FXCollections.observableList(unFiles);
        affObserve.addListener(new ListChangeListener(){
            @Override
            public void onChanged(ListChangeListener.Change change){
                change.next();/*from the javadoc 
                 * 'Go to the next change. In initial state is invalid a require 
                 * a call to next() before calling other methods. The first 
                 * next() call will make this object represent the first change.
                 */
                if (change.getRemovedSize() > 0){
                    List<SnpFile> both = new ArrayList<>(unFiles);
                    both.addAll(affFiles);
                    recheckChromosomeSelector(both);
                }else if (change.getAddedSize() > 0){
                    addToChromosomeSelector(affFiles);
                }
            }
        });
        
        /*as above 
         * but for unaffected files
         */
        unObserve.addListener(new ListChangeListener(){
            @Override
            public void onChanged(ListChangeListener.Change change){
                change.next();
                if (change.getRemovedSize() > 0){
                    List<SnpFile> both = new ArrayList<>(unFiles);
                    both.addAll(affFiles);
                    recheckChromosomeSelector(both);
                }else if (change.getAddedSize() > 0){
                    addToChromosomeSelector(unFiles);
                }

            }
        });
        
    }
    
    public void saveRegion(final String chromosome, final double startCoordinate, 
            final double endCoordinate){
        final Task<RegionSummary> saveSelectionTask = new Task<RegionSummary>() {
            @Override
            protected RegionSummary call() throws Exception {
                try{
                    updateProgress(-1, -1);
                    updateTitle("Finding flanking SNPs");
                    updateMessage("Searching for nearest SNP in all files...");
                   
                   /* read SnpFiles to find closest SNPs - use binary search
                    * to find nearby SNP and refine to closest
                    */
                   List<SnpFile.SnpLine> startAndEndSnps = 
                           searchCoordinate(chromosome, (int) startCoordinate , 
                           (int) endCoordinate);
                   if (startAndEndSnps == null){
                       System.out.println("Start and End SNPS ARE NULL!");
                       //DISPLAY ERROR HERE?
                       return null;
                   }
                   RegionSummary region = new RegionSummary(chromosome, 
                           startAndEndSnps.get(0).getPosition(),
                           startAndEndSnps.get(1).getPosition(), 0, 0, 
                           startAndEndSnps.get(0).getId(), 
                           startAndEndSnps.get(1).getId());
                   
                   return region;
                   
                }catch (NumberFormatException ex){
                    Alert error = new Alert(AlertType.ERROR);
                    error.getDialogPane().setPrefSize(420, 200);
                    error.setResizable(true);
            error.setTitle("SnpViewer");
                    error.setHeaderText("Error!");
                    error.setContentText("Can't display flanking SNP IDs"
                            + " - missing required component!\n\nPlease "
                            + "report this error.");
                    error.showAndWait();
                }
                return null;
                }
        };
        setProgressMode(true);
        progressBar.progressProperty().bind(saveSelectionTask.progressProperty());
        progressMessage.textProperty().unbind();
        progressMessage.textProperty().bind(saveSelectionTask.messageProperty());
        progressTitle.textProperty().unbind();
        progressTitle.textProperty().bind(saveSelectionTask.titleProperty());
        saveSelectionTask.setOnSucceeded(new EventHandler<WorkerStateEvent>(){
            @Override
            public void handle (WorkerStateEvent e){
                setProgressMode(false);
                RegionSummary result = (RegionSummary) e.getSource().getValue();
                savedRegions.add(result);
                RegionSummary sorter = new RegionSummary();
                sorter.mergeRegionsByPosition(savedRegions);
                saveProject();
                clearDragSelectRectangle();
                savedRegionsDisplay.clear();
                savedRegionsReference.clear();
                drawSavedRegions((String) chromosomeBoxList[chromosomeSelector
                        .getSelectionModel().getSelectedIndex()]);
                progressBar.progressProperty().unbind();
                progressBar.progressProperty().set(0);   
                progressTitle.textProperty().unbind();
                progressMessage.textProperty().unbind();
                progressTitle.setText("");
                progressMessage.setText("");
             }

        });
        saveSelectionTask.setOnFailed(new EventHandler<WorkerStateEvent>(){
            @Override
            public void handle (WorkerStateEvent e){
                setProgressMode(false);
                progressBar.progressProperty().unbind();
                progressBar.progressProperty().set(0);
                progressTitle.textProperty().unbind();
                progressMessage.textProperty().unbind();
                progressTitle.setText("");
                progressMessage.setText("");
                Alert error = new Alert(AlertType.ERROR);
                error.getDialogPane().setPrefSize(420, 200);
                error.setResizable(true);
                error.setTitle("SnpViewer");
                error.setHeaderText("Save Region Error");
                error.setContentText("Error finding flanking SNPs\n" + 
                        saveSelectionTask.getException().getLocalizedMessage());
                saveSelectionTask.getException().printStackTrace();
                error.showAndWait();
            }

        });
        saveSelectionTask.setOnCancelled(new EventHandler<WorkerStateEvent>(){
            @Override
            public void handle (WorkerStateEvent e){
                progressMessage.setText("Region write cancelled");
                setProgressMode(false);
                progressBar.progressProperty().unbind();
                progressBar.progressProperty().set(0);
                progressTitle.textProperty().unbind();
                progressMessage.textProperty().unbind();
                progressTitle.setText("");
                progressMessage.setText("");
                Alert error = new Alert(AlertType.ERROR);
                error.setResizable(true);
                error.setTitle("SnpViewer");
                error.setHeaderText("Save Region Cancelled");
                error.setContentText("User cancelled region save.");
                error.showAndWait();
            }

        });
        cancelButton.setOnAction(new EventHandler<ActionEvent>(){
               @Override
               public void handle(ActionEvent actionEvent){
                    saveSelectionTask.cancel();

                }
           });
        new Thread(saveSelectionTask).start();
    }
    
    private void saveSelection(){
        /* get coordinates of selection and create a RegionSummary object.
         * Add RegionSummary to saved regions and merge all saved regions
         * in case of overlaps.  Refresh view to reflect new saved region
         */
        
        try{
            if (!genomeVersion.equals("") && chromosomeSelector.
                    getSelectionModel().getSelectedIndex() > -1
                    && dragSelectRectangle.getWidth() > 0){

                //work out coordinates based on chromosome and pane sizes
               ChromosomeLength chromLength = new ChromosomeLength(genomeVersion);
               String currentChrom = (String) chromosomeBoxList[
                       chromosomeSelector.getSelectionModel().getSelectedIndex()];
               double startCoordinate = chromLength.getLength(currentChrom)
                       /selectionOverlayPane.getWidth() * dragSelectRectangle.getX();
               double selectionWidth = chromLength.getLength(currentChrom)
                       /selectionOverlayPane.getWidth() * dragSelectRectangle.getWidth();
               if (dragSelectRectangle.getX() ==0){
                   startCoordinate = 1;
               }
               saveRegion(currentChrom, startCoordinate, startCoordinate + selectionWidth);
            }
        }catch (Exception ex){
            Alert error = new Alert(AlertType.ERROR);
            error.getDialogPane().setPrefSize(420, 200);
            error.setResizable(true);
            error.setTitle("SnpViewer");
            error.setHeaderText("Save Region Error");
            error.setContentText("Error determing selection coordinates\n" + 
                    ex.getLocalizedMessage());
            ex.printStackTrace();
            error.showAndWait();
        }
                      
    }
    
    private void displayFlankingSnpIDs(final Rectangle rectangle){
        /* get coordinates of selection and report back
         * flanking snp ids and coordinates
         */
        try{
            if (!genomeVersion.equals("") && chromosomeSelector.
                    getSelectionModel().getSelectedIndex() > -1
                    && rectangle.getWidth() > 0){
                
                //work out coordinates based on chromosome and pane sizes
               ChromosomeLength chromLength = new ChromosomeLength(genomeVersion);
               String currentChrom = (String) chromosomeBoxList[
                       chromosomeSelector.getSelectionModel().getSelectedIndex()];
               double startCoordinate = chromLength.getLength(currentChrom)
                       /selectionOverlayPane.getWidth() * rectangle.getX();
               double selectionWidth = chromLength.getLength(currentChrom)
                       /selectionOverlayPane.getWidth() * rectangle.getWidth();
               if (rectangle.getX() ==0){
                   startCoordinate = 1;
               }
               displayFlankingSnpIDs(currentChrom, startCoordinate, 
                       startCoordinate + selectionWidth);
            }
        }catch(Exception ex){
            Alert error = new Alert(AlertType.ERROR);
            error.getDialogPane().setPrefSize(420, 200);
            error.setResizable(true);
            error.setTitle("SnpViewer");
            error.setHeaderText("Error Displaying flanking SNPs");
            error.setContentText("Couldn't display flanking SNP IDs - "
                    + "build error?\n" + 
                    ex.getLocalizedMessage());
            ex.printStackTrace();
            error.showAndWait();
        }
    }
    public void displayFlankingSnpIDs(final String chrom, final double start, 
            final double end){
        final Task<List<String>> displayTask = new Task<List<String>>() {
            @Override
            protected List<String> call() {
                updateProgress(-1, -1);
                updateTitle("Finding flanking SNPs");
                updateMessage("Searching for nearest SNP in all files...");
                //work out coordinates based on chromosome and pane sizes
                   /* read SnpFiles to find closest SNPs - use binary search
                    * to find nearby SNP and refine to closest
                    */
                   List<SnpFile.SnpLine> startAndEndSnps = searchCoordinate(chrom, 
                           (int) start, (int) end);
                   if (startAndEndSnps == null){
                       //DISPLAY ERROR HERE?
                       return null;
                   }
                   String coordResult = "chr" + chrom + ":" + 
                       nf.format(startAndEndSnps.get(0).getPosition()) + "-" 
                           + nf.format(startAndEndSnps.get(1).getPosition());
                   String idResult = startAndEndSnps.get(0).getId() + 
                           ";" + startAndEndSnps.get(1).getId();
                   List<String> result = new ArrayList();
                   result.add(coordResult);
                   result.add(idResult);
                   return result;
            }           
        };

        setProgressMode(true);
        progressBar.progressProperty().bind(displayTask.progressProperty());
        progressMessage.textProperty().unbind();
        progressMessage.textProperty().bind(displayTask.messageProperty());
        progressTitle.textProperty().unbind();
        progressTitle.textProperty().bind(displayTask.titleProperty());
        displayTask.setOnSucceeded(new EventHandler<WorkerStateEvent>(){
            @Override
            public void handle (WorkerStateEvent e){
                setProgressMode(false);
                progressBar.progressProperty().unbind();
                progressBar.progressProperty().set(0);   
                progressTitle.textProperty().unbind();
                progressMessage.textProperty().unbind();
                progressTitle.setText("");
                progressMessage.setText("");
             }

        });
        displayTask.setOnFailed(new EventHandler<WorkerStateEvent>(){
            @Override
            public void handle (WorkerStateEvent e){
                setProgressMode(false);
                progressBar.progressProperty().unbind();
                progressBar.progressProperty().set(0);
                progressTitle.textProperty().unbind();
                progressMessage.textProperty().unbind();
                progressTitle.setText("");
                progressMessage.setText("");
                Alert error = new Alert(AlertType.ERROR);
                error.getDialogPane().setPrefSize(420, 200);
                error.setResizable(true);
                error.setTitle("SnpViewer");
                error.setHeaderText("Display error");
                error.setContentText("Error displaying flanking SNPs\n" + 
                        displayTask.getException().getLocalizedMessage());
                displayTask.getException().printStackTrace();
                error.showAndWait();
                
            }

        });
        displayTask.setOnCancelled(new EventHandler<WorkerStateEvent>(){
            @Override
            public void handle (WorkerStateEvent e){
                progressMessage.setText("Display flanking SNPs cancelled");
                setProgressMode(false);
                progressBar.progressProperty().unbind();
                progressBar.progressProperty().set(0);
                progressTitle.textProperty().unbind();
                progressMessage.textProperty().unbind();
                progressTitle.setText("");
                progressMessage.setText("");
                Alert error = new Alert(AlertType.ERROR);
                error.setResizable(true);
                error.setTitle("SnpViewer");
                error.setHeaderText("Display Cancelled");
                error.setContentText("User cancelled display.");
                error.showAndWait();
            }

        });
        cancelButton.setOnAction(new EventHandler<ActionEvent>(){
               @Override
               public void handle(ActionEvent actionEvent){
                    displayTask.cancel();

                }
           });
        new Thread(displayTask).start();
        try{
           List<String> result = displayTask.get();
           FXMLLoader loader = new FXMLLoader(getClass().getResource("RegionReporter.fxml"));
           Stage stage =  new Stage();
           Pane page = (Pane) loader.load();
           Scene scene = new Scene(page);
           stage.setScene(scene);
           stage.setTitle("SNP Viewer Region Summary");
           stage.getIcons().add(new Image(this.getClass().
                    getResourceAsStream("icon.png")));
           RegionReporterController regionReporter =
                   loader.<RegionReporterController>getController();
           if (result == null){
               final Hyperlink errorLink = new Hyperlink();
               errorLink.setText("Error!");
               regionReporter.setCoordinates(errorLink);
               regionReporter.setIds("Error!");
           }else{
               String db = getUcscDb();
               final String regionUrl = "http://genome.ucsc.edu/cgi-bin/hgTracks?db=" + db + 
                "&position=" + result.get(0);
               final Hyperlink coordLink = new Hyperlink();
               coordLink.setText(result.get(0));
               coordLink.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent e) {
                        getHostServices().showDocument(regionUrl);
                        coordLink.setVisited(true);
                        coordLink.setUnderline(false);
                    }
                });
               regionReporter.setCoordinates(coordLink);
               regionReporter.setIds(result.get(1));
           }
           //scene.getStylesheets().add(SnpViewer.class
           //             .getResource("SnpViewerStyleSheet.css").toExternalForm());
           stage.setResizable(true);
           stage.initModality(Modality.NONE);
           
           stage.show();
        }catch (InterruptedException | ExecutionException | IOException ex){
            Alert error = new Alert(AlertType.ERROR);
            error.getDialogPane().setPrefSize(420, 200);
            error.setResizable(true);
            error.setTitle("SnpViewer");
            error.setHeaderText("Display Error");
            error.setContentText("Can't display flanking SNP IDs"
                    + " - exception caught!\n\nPlease report this error.");
            error.showAndWait();
        }
        
    }
    
    public String getUcscDb(){
        if (genomeVersion.equals("37")){
            return "hg19";
        }
        else if (genomeVersion.equals("36")){
            return "hg18";
        }else{
            return genomeVersion;
        }
    }
    
    public void writeSavedRegionsToFile(){
        if (savedRegions.size() < 1){
            Alert error = new Alert(AlertType.ERROR);
            error.setResizable(true);
            error.setTitle("SnpViewer");
            error.setHeaderText("No Saved Regions");
            error.setContentText("No Saved Regions exist to write!");
            error.showAndWait();
            return; 
        }
       final int flanks = 10;
       FileChooser fileChooser = new FileChooser();
       if (projectFile != null){
            fileChooser.setInitialDirectory(projectFile.getParentFile());
        }else{
            fileChooser.setInitialDirectory(new File(getProperty("user.home")));
        }
       FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx");
       fileChooser.getExtensionFilters().add(extFilter);
       fileChooser.setTitle("Write regions to Excel file (.xlsx)...");
       File rFile = fileChooser.showSaveDialog(mainWindow);
       if (rFile == null){
           return;
       }else if (!rFile.getName().endsWith(".xlsx")){
           rFile = new File (rFile.getAbsolutePath() + ".xlsx");
       }
       final File regionFile = rFile;
       final Task<Boolean> writeTask = new Task() {
            @Override
            protected Boolean call() throws Exception {
                try{
                   updateProgress(-1, -1);
                   BufferedOutputStream out = new BufferedOutputStream(new 
                           FileOutputStream(regionFile));
                   Workbook wb = new XSSFWorkbook();
                   //first create a summary sheet of all regions
                   Sheet sheet = wb.createSheet();
                   Row row = null;
                   int rowNo = 0;
                   int sheetNo = 0;
                   wb.setSheetName(sheetNo++, "Summary");
                   row = sheet.createRow(rowNo++);
                   String header[] = {"Coordinates", "rsIDs", "Size (Mb)"};
                   for (int col = 0; col < header.length; col ++){
                       Cell cell = row.createCell(col);
                       cell.setCellValue(header[col]);
                   }
                   for (int i = 0; i < savedRegions.size(); i++){
                       row = sheet.createRow(rowNo++);
                       int col = 0;
                       Cell cell = row.createCell(col++);
                       cell.setCellValue("chr" + savedRegions.get(i).getCoordinateString());
                       cell = row.createCell(col++);
                       cell.setCellValue(savedRegions.get(i).getIdLine());
                       cell = row.createCell(col++);
                       double mB = (double) savedRegions.get(i).getLength()/1000000;
                       cell.setCellValue(mB);
                   }
                   
                   ArrayList<SnpFile> bothFiles = new ArrayList<>();
                   bothFiles.addAll(affFiles);
                   bothFiles.addAll(unFiles);
                   String prevChrom = new String();
                   double prog = 0;
                   double total = savedRegions.size() * bothFiles.size() * 2;
                   updateProgress(prog, total);
                   int regCounter = 0;
                   for (RegionSummary reg: savedRegions){
                       updateMessage("Writing region " + ++regCounter + " of "  
                               + savedRegions.size());
                       //create a sheet for each chromosome
                       if (!reg.getChromosome().equalsIgnoreCase(prevChrom)){
                           if (!prevChrom.isEmpty()){
                               
                               CellRangeAddress[] regions = {new CellRangeAddress
                                    (0, rowNo, 2, 2 + bothFiles.size())};
                               SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();
                               
                               ConditionalFormattingRule rule1 = sheetCF.
                                       createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"AA\"");
                               PatternFormatting fill1 = rule1.createPatternFormatting();
                               fill1.setFillBackgroundColor(IndexedColors.LIGHT_GREEN.index);
                               fill1.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
                               ConditionalFormattingRule rule2 = sheetCF.
                                       createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"BB\"");
                               PatternFormatting fill2 = rule2.createPatternFormatting();
                               fill2.setFillBackgroundColor(IndexedColors.PALE_BLUE.index);
                               fill2.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
                               ConditionalFormattingRule rule3 = sheetCF.
                                       createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"AB\"");
                               PatternFormatting fill3 = rule3.createPatternFormatting();
                               fill3.setFillBackgroundColor(IndexedColors.ROSE.index);
                               fill3.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
                               sheetCF.addConditionalFormatting(regions, rule3, rule2);
                               sheetCF.addConditionalFormatting(regions, rule1);
                           }
                           rowNo = 0;
                           sheet = wb.createSheet();
                           wb.setSheetName(sheetNo++, reg.getChromosome());
                           prevChrom = reg.getChromosome();
                           
                       }else{//pad regions with an empty line
                           rowNo++;
                       }
                       TreeMap<Integer, HashMap<String, String>> coordMap = new TreeMap();
                       /*coordmap - key is position, key of hashmap 
                        * is input filename and value call
                        */
                       HashMap<Integer, String> coordToId = new HashMap<>();
                       //coordinate to rs ID

                        try  {
                            for (SnpFile f: bothFiles){
                                updateProgress(prog++, total);
                                if (isCancelled()){
                                    return false;
                                }
                                List<SnpFile.SnpLine> lines = f.getSnpsInRegion(
                                        reg.getChromosome(), reg.getStartPos(), 
                                        reg.getEndPos(), flanks); 
                                for (SnpFile.SnpLine snpLine: lines){
                                    if (isCancelled()){
                                        return false;
                                    }
                                    Integer coord = snpLine.getPosition();
                                    if (!coordMap.containsKey(coord)){
                                        coordMap.put(coord, new HashMap<String, String>());
                                    }
                                    String filename = f.inputFile.getName();
                                    String rsId = snpLine.getId();
                                    String call = snpLine.getCall();
                                    coordMap.get(coord).put(filename, call);
                                    coordToId.put(coord, rsId);
                                }
                            }
                            row = sheet.createRow(rowNo++);
                            Cell cell = row.createCell(0);
                            cell.setCellValue(reg.getCoordinateString());
                            row = sheet.createRow(rowNo++);
                            cell = row.createCell(0);
                            cell.setCellValue(reg.getIdLine());
                            
                           int col = 0;
                           row = sheet.createRow(rowNo++);
                           cell = row.createCell(col++);
                           cell.setCellValue("Position");
                           cell = row.createCell(col++);
                           cell.setCellValue("rsID");
                           for (SnpFile f: bothFiles){
                               updateProgress(prog++, total);
                               cell = row.createCell(col++);
                               if (f.getSampleName() != null && !f.getSampleName().isEmpty()){
                                   cell.setCellValue(f.getSampleName());
                               }else{
                                   cell.setCellValue(f.inputFile.getName());                               
                               }
                           }
                            for (Entry current : coordMap.entrySet()) {
                                if (isCancelled()){
                                    return false;
                                }
                                col = 0;
                                Integer coord = (Integer) current.getKey();
                                row = sheet.createRow(rowNo++);
                                cell = row.createCell(col++);
                                cell.setCellValue(coord);
                                cell = row.createCell(col++);
                                cell.setCellValue(coordToId.get(coord));
                                HashMap<String, String> fileToCall = 
                                        (HashMap<String, String>) current.getValue();
                                for (SnpFile f: bothFiles){
                                    cell = row.createCell(col++);
                                    if (fileToCall.containsKey(f.inputFile.getName())){
                                        cell.setCellValue(fileToCall.get(f.inputFile.getName()));
                                    }else{
                                        cell.setCellValue("-");
                                    }
                                }
                             }
                        }catch(Exception ex){
                            return false;
                        }
                       
                   }
                   CellRangeAddress[] regions = {new CellRangeAddress
                           (0, rowNo, 2, 2 + bothFiles.size())};
                   SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();

                   ConditionalFormattingRule rule1 = sheetCF.
                           createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"AA\"");
                   PatternFormatting fill1 = rule1.createPatternFormatting();
                   fill1.setFillBackgroundColor(IndexedColors.LIGHT_GREEN.index);
                   fill1.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
                   ConditionalFormattingRule rule2 = sheetCF.
                           createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"BB\"");
                   PatternFormatting fill2 = rule2.createPatternFormatting();
                   fill2.setFillBackgroundColor(IndexedColors.PALE_BLUE.index);
                   fill2.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
                   ConditionalFormattingRule rule3 = sheetCF.
                           createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"AB\"");
                   PatternFormatting fill3 = rule3.createPatternFormatting();
                   fill3.setFillBackgroundColor(IndexedColors.ROSE.index);
                   fill3.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
                   sheetCF.addConditionalFormatting(regions, rule3, rule2);
                   sheetCF.addConditionalFormatting(regions, rule1);
                   wb.write(out);
                   updateProgress(total, total);
                   out.close();
                }catch (IOException
                        | NumberFormatException ex){
                       ex.printStackTrace();
                       return false;
                }
                return true;
            }
        };//end of task
        
        setProgressMode(true);
        progressBar.progressProperty().bind(writeTask.progressProperty());
        progressMessage.textProperty().bind(writeTask.messageProperty());
        writeTask.setOnSucceeded(new EventHandler<WorkerStateEvent>(){
            @Override
            public void handle (WorkerStateEvent e){
                if ((boolean) e.getSource().getValue() == true){
                    Alert info = new Alert(AlertType.INFORMATION);
                    info.getDialogPane().setPrefSize(420, 200);
                    info.setResizable(true);
                    info.setTitle("SnpViewer");
                    info.setHeaderText("Regions Written");
                    info.setContentText("Saved regions written "
                            + "to file " + "(" + regionFile.getName() +
                            ")successfully");
                    info.showAndWait();
                }else{
                    Alert error = new Alert(AlertType.ERROR);
                    error.getDialogPane().setPrefSize(420, 200);
                    error.setResizable(true);
                    error.setTitle("SnpViewer");
                    error.setHeaderText("Write Failed");
                    error.setContentText("Region write failed.\n" + 
                            e.getSource().getException().getLocalizedMessage());
                    e.getSource().getException().printStackTrace();
                    error.showAndWait();
                }
                //setProgressMode(false);
                progressBar.progressProperty().unbind();
                progressBar.progressProperty().set(0);
                progressMessage.textProperty().unbind();
                progressMessage.setText("");
                progressTitle.setText("");
                
            }

        });
        writeTask.setOnFailed(new EventHandler<WorkerStateEvent>(){
            @Override
            public void handle (WorkerStateEvent e){
                setProgressMode(false);
                progressBar.progressProperty().unbind();
                progressBar.progressProperty().set(0);
                progressMessage.textProperty().unbind();
                progressMessage.setText("");
                progressTitle.setText("Region write failed!");
                Alert error = new Alert(AlertType.ERROR);
                error.getDialogPane().setPrefSize(420, 200);
                error.setResizable(true);
                error.setTitle("SnpViewer");
                error.setHeaderText("Region write error");
                error.setContentText("Error writing region to file\n" + 
                        e.getSource().getException().getLocalizedMessage());
                e.getSource().getException().printStackTrace();
                error.showAndWait();
            }

        });
        writeTask.setOnCancelled(new EventHandler<WorkerStateEvent>(){
            @Override
            public void handle (WorkerStateEvent e){
                progressMessage.setText("Region write cancelled");
                progressTitle.setText("Cancelled");
                setProgressMode(false);
                progressBar.progressProperty().unbind();
                progressBar.progressProperty().set(0);
            }

        });
        cancelButton.setOnAction(new EventHandler<ActionEvent>(){
               @Override
               public void handle(ActionEvent actionEvent){
                    writeTask.cancel();

                }
           });
        progressTitle.setText("Writing regions to .xlsx file");
        new Thread(writeTask).start();
    }
    public void writeRegionToFile(final Rectangle rectangle){
        try{
            ChromosomeLength chromLength = new ChromosomeLength(genomeVersion);
           String currentChrom = (String) chromosomeBoxList[
                   chromosomeSelector.getSelectionModel().getSelectedIndex()];
           double startCoordinate = chromLength.getLength(currentChrom)
                   /selectionOverlayPane.getWidth() * rectangle.getX();
           double selectionWidth = chromLength.getLength(currentChrom)
                   /selectionOverlayPane.getWidth() * rectangle.getWidth();
           if (rectangle.getX() ==0){
               startCoordinate = 1;
           }
           writeRegionToFile(currentChrom, startCoordinate, startCoordinate + selectionWidth);
        }catch(Exception ex){
            Alert error = new Alert(AlertType.ERROR);
            error.getDialogPane().setPrefSize(420, 200);
            error.setResizable(true);
            error.setTitle("SnpViewer");
            error.setHeaderText("Region write error");
            error.setContentText("Region write failed while assessing "
                    + "region properties.\n" + ex.getLocalizedMessage());
            ex.printStackTrace();
            error.showAndWait();
        }
    }
    public void writeRegionToFile(final String chromosome, final double start, 
            final double end){
        /* get coordinates of selection and report back
         * write SNPs in region to file
         */
       FileChooser fileChooser = new FileChooser();
       if (projectFile != null){
            fileChooser.setInitialDirectory(projectFile.getParentFile());
        }else{
            fileChooser.setInitialDirectory(new File(getProperty("user.home")));
        }
       FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Excel  (*.xlsx)", "*.xlsx");
       fileChooser.getExtensionFilters().add(extFilter);
       fileChooser.setTitle("Write region to Excel file (.xlsx)...");
       File rFile = fileChooser.showSaveDialog(mainWindow);
       if (rFile == null){
           return;
       }else if (!rFile.getName().endsWith(".xlsx")){
           rFile = new File (rFile.getAbsolutePath() + ".xlsx");
       }
       final File regionFile = rFile;
       final Task<Boolean> writeTask = new Task() {
            @Override
            protected Boolean call() throws Exception {
                try{
                   
                   updateProgress(-1, -1);
                   ArrayList<SnpFile> bothFiles = new ArrayList<>();
                   bothFiles.addAll(affFiles);
                   bothFiles.addAll(unFiles);
                   TreeMap<Integer, HashMap<String, String>> coordMap = new TreeMap();
                   /*coordmap - key is position, key of hashmap 
                    * is input filename and value call
                    */
                   HashMap<Integer, String> coordToId = new HashMap<>();
                   double progress = 0;
                   double total = bothFiles.size() * 5;
                   try  {
                        BufferedOutputStream out = new BufferedOutputStream(
                                new FileOutputStream(regionFile));                    
                        Workbook wb = new XSSFWorkbook();
                        Sheet sheet = wb.createSheet();
                       int rowNo = 0;
                       Row row = sheet.createRow(rowNo++);
                        for (SnpFile f: bothFiles){
                            if (isCancelled()){
                                return false;
                            }
                            updateProgress(++progress, total);
                            updateMessage("Reading region in " + f.inputFile.getName());
                            List<SnpFile.SnpLine> lines = f.getSnpsInRegion(chromosome, 
                                    (int) start, (int) end); 
                            for (SnpFile.SnpLine snpLine: lines){
                                if (isCancelled()){
                                    return false;
                                }
                                Integer coord = snpLine.getPosition();
                                if (!coordMap.containsKey(coord)){
                                    coordMap.put(coord, new HashMap<String, String>());
                                }
                                String filename = f.inputFile.getName();
                                String rsId = snpLine.getId();
                                String call = snpLine.getCall();
                                coordMap.get(coord).put(filename, call);
                                coordToId.put(coord, rsId);
                            }
                        }
                        Cell cell = row.createCell(0);
                        cell.setCellValue("chr" + chromosome + ":" + 
                                coordMap.firstKey() + "-" + coordMap.lastKey());
                        row = sheet.createRow(rowNo++);
                        cell = row.createCell(0);
                        cell.setCellValue(coordToId.get(coordMap.firstKey()) + 
                                ";" + coordToId.get(coordMap.lastKey()));
                        row = sheet.createRow(rowNo++);
                        int colNo = 0;
                        cell = row.createCell(colNo++);
                        cell.setCellValue("Position");
                        cell = row.createCell(colNo++);
                        cell.setCellValue("rsID");
                        for (SnpFile f: bothFiles){
                            cell = row.createCell(colNo++);
                            if (f.getSampleName() != null && f.getSampleName()
                                    .length() > 0){
                                cell.setCellValue(f.getSampleName());
                            }else{
                                cell.setCellValue(f.getInputFileName());
                            }
                        }
                        progress = coordMap.size();
                        total = 5 * coordMap.size();
                        updateMessage("Writing region to file...");
                        for (Entry current : coordMap.entrySet()) {
                            if (isCancelled()){
                                return false;
                            }
                            progress += 4;
                            updateProgress(progress, total);
                            row = sheet.createRow(rowNo++);
                            colNo = 0;
                            Integer coord = (Integer) current.getKey();
                            cell = row.createCell(colNo++);
                            cell.setCellValue(coord);
                            String rsId = coordToId.get(coord);
                            cell = row.createCell(colNo++);
                            cell.setCellValue(rsId);
                            HashMap<String, String> fileToCall = 
                                    (HashMap<String, String>) current.getValue();
                            for (SnpFile f: bothFiles){
                                cell = row.createCell(colNo++);
                                if (fileToCall.containsKey(f.inputFile.getName())){
                                    cell.setCellValue(fileToCall.get(f.inputFile.getName()));
                                }else{
                                    cell.setCellValue("-");
                                }
                            }
                         }
                        CellRangeAddress[] regions = {new CellRangeAddress
                           (0, rowNo, 2, 2 + bothFiles.size())};
                       SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();

                       ConditionalFormattingRule rule1 = sheetCF.
                               createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"AA\"");
                       PatternFormatting fill1 = rule1.createPatternFormatting();
                       fill1.setFillBackgroundColor(IndexedColors.LIGHT_GREEN.index);
                       fill1.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
                       ConditionalFormattingRule rule2 = sheetCF.
                               createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"BB\"");
                       PatternFormatting fill2 = rule2.createPatternFormatting();
                       fill2.setFillBackgroundColor(IndexedColors.PALE_BLUE.index);
                       fill2.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
                       ConditionalFormattingRule rule3 = sheetCF.
                               createConditionalFormattingRule(ComparisonOperator.EQUAL, "\"AB\"");
                       PatternFormatting fill3 = rule3.createPatternFormatting();
                       fill3.setFillBackgroundColor(IndexedColors.ROSE.index);
                       fill3.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
                       sheetCF.addConditionalFormatting(regions, rule3, rule2);
                       sheetCF.addConditionalFormatting(regions, rule1);
                       wb.write(out);
                       out.close();
                       return true;
                    }catch(IOException ex){
                        return false;
                    }
                }catch (Exception ex){
                       return false;
                }
            }
        };//end of task
        
        setProgressMode(true);
        progressBar.progressProperty().bind(writeTask.progressProperty());
        progressMessage.textProperty().bind(writeTask.messageProperty());
        writeTask.setOnSucceeded(new EventHandler<WorkerStateEvent>(){
            @Override
            public void handle (WorkerStateEvent e){
                if ((boolean) e.getSource().getValue() == true){
                    Alert info = new Alert(AlertType.INFORMATION);
                    info.getDialogPane().setPrefSize(420, 200);
                    info.setResizable(true);
                    info.setTitle("SnpViewer");
                    info.setHeaderText("Region Written");
                    info.setContentText("Region written to file "
                            + "(" + regionFile.getName() + ") successfully");
                    info.showAndWait();
                }else{
                    Alert error = new Alert(AlertType.ERROR);
                    error.getDialogPane().setPrefSize(420, 200);
                    error.setResizable(true);
                    error.setTitle("SnpViewer");
                    error.setHeaderText("Region write failed");
                    error.setContentText("Region write failed.\n" + 
                            e.getSource().getException().getLocalizedMessage());
                    e.getSource().getException().printStackTrace();
                    error.showAndWait();
                }
                setProgressMode(false);
                progressBar.progressProperty().unbind();
                progressBar.progressProperty().set(0);
                progressMessage.textProperty().unbind();
                progressMessage.setText("");
                progressTitle.setText("");
                
            }

        });
        writeTask.setOnFailed(new EventHandler<WorkerStateEvent>(){
            @Override
            public void handle (WorkerStateEvent e){
                setProgressMode(false);
                progressBar.progressProperty().unbind();
                progressBar.progressProperty().set(0);
                progressMessage.textProperty().unbind();
                progressMessage.setText("");
                progressTitle.setText("Region write failed!");
                Alert error = new Alert(AlertType.ERROR);
                error.getDialogPane().setPrefSize(420, 200);
                error.setResizable(true);
                error.setTitle("SnpViewer");
                error.setHeaderText("Region write failed");
                error.setContentText("Region write failed.\n" + 
                        e.getSource().getException().getLocalizedMessage());
                e.getSource().getException().printStackTrace();
                error.showAndWait();
            }

        });
        writeTask.setOnCancelled(new EventHandler<WorkerStateEvent>(){
            @Override
            public void handle (WorkerStateEvent e){
                progressMessage.setText("Region write cancelled");
                progressTitle.setText("Cancelled");
                setProgressMode(false);
                progressBar.progressProperty().unbind();
                progressBar.progressProperty().set(0);
                Alert error = new Alert(AlertType.ERROR);
                error.setResizable(true);
                error.setTitle("SnpViewer");
                error.setHeaderText("Region write cancelled");
                error.setContentText("Region write cancelled by user.\n");
                error.showAndWait();
            }

        });
        cancelButton.setOnAction(new EventHandler<ActionEvent>(){
               @Override
               public void handle(ActionEvent actionEvent){
                    writeTask.cancel();

                }
           });
        progressTitle.setText("Writing region to .xlsx file");
        new Thread(writeTask).start();
    }
    
    private List<SnpFile.SnpLine> searchCoordinate(String chrom, int start, int end){
        List<SnpFile.SnpLine> foundSnps = new ArrayList<>();
        List<SnpFile> bothFiles = new ArrayList<>();
        bothFiles.addAll(affFiles);
        bothFiles.addAll(unFiles);
        for (SnpFile f: bothFiles){
            List<SnpFile.SnpLine> snps = 
                    f.getFlankingSnps(chrom, start, end);
            if (foundSnps.isEmpty()){
                foundSnps = snps;
            }else if (snps != null){
                
                if (foundSnps.get(0).getPosition() > start
                        && snps.get(0).getPosition() <= start){
                    foundSnps.set(0, snps.get(0));
                }else{
                    if (Math.abs(snps.get(0).getPosition())
                        < Math.abs(foundSnps.get(0).getPosition())){
                        foundSnps.set(0, snps.get(0));
                    }  
                }
                if (
                foundSnps.get(1).getPosition() < end
                            && snps.get(0).getPosition() >= end){
                        foundSnps.set(1, snps.get(1));
                    }else{
                        if (Math.abs(snps.get(1).getPosition())
                            < Math.abs(foundSnps.get(1).getPosition())){
                            foundSnps.set(0, snps.get(1));
                    }
                }
            }
        }
        return foundSnps;
    }
    
    public void autoFindRegions(){
        if (affObserve.isEmpty()){
            Alert error = new Alert(AlertType.ERROR);
            error.getDialogPane().setPrefSize(420, 200);
            error.setResizable(true);
            error.setTitle("SnpViewer");
            error.setHeaderText("No Affected samples to analyze!");
            error.setContentText("Find Regions can only be run when there "
                    + "is at least one Affected sample in the project.  Use the "
                    + "'Add Affected' button/menu item to add  Affected samples.");
            error.showAndWait();
            return;
        }
        Stage stage =  new Stage();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("FindRegionsInterface.fxml"));
        try{
            Pane page = (Pane) loader.load();
            FindRegionsInterfaceController findReg = 
                    (FindRegionsInterfaceController) loader.getController();
            Scene scene = new Scene(page);
            stage.setScene(scene);
            //scene.getStylesheets().add(SnpViewer.class
            //            .getResource("SnpViewerStyleSheet.css").toExternalForm());
            setProgressMode(true);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.getIcons().add(new Image(this.getClass().
                    getResourceAsStream("icon.png")));
            stage.showAndWait();
            if (findReg.getCancelled()){
                setProgressMode(false);
                return;
            }
            
            //int w, double r, boolean con, int refWind, double refCut,
            //int minReportSize, int minReportRun, double het, double dischord)
            int window = Integer.parseInt(findReg.getWindow());
            double regionLength = Double.parseDouble(findReg.getRegionLength());
            boolean checkConcordant = findReg.getCheckConcordance();
            int refineWindow = Integer.parseInt(findReg.getRefineSize());
            double refineTolerance = Double.parseDouble(findReg.getRefineTolerance());
            int minReport = Integer.parseInt(findReg.getReportLength());
            int minReportRun = Integer.parseInt(findReg.getReportMinSnps());
            double hetTolerance = Double.parseDouble(findReg.getHetTolerance());
            double dischordTolerance = Double.parseDouble(findReg.getDischordTolerance());
            boolean autosomesOnly = findReg.getAutosomesOnly();
            LinkedHashSet<String> c = new LinkedHashSet();
            for (Object item: chromosomeSelector.getItems()){
                if (item instanceof String){
                    String chrom = (String) item;
                    if (autosomesOnly){
                        if (chrom.matches("\\d+")){//only add autosomes
                            c.add(chrom);
                        }
                    }else{
                        c.add(chrom);
                    }
                }
            }
            
            final RegionFinder regionFinder = new RegionFinder(c, affFiles, unFiles, 
                    window, regionLength, checkConcordant, refineWindow, 
                    refineTolerance, minReport, minReportRun, hetTolerance,
                    dischordTolerance);
            final SnpViewer thisthis = this;
            regionFinder.setOnSucceeded(new EventHandler<WorkerStateEvent>(){
                   @Override
                   public void handle (WorkerStateEvent t){
                       progressBar.progressProperty().unbind();
                       progressMessage.textProperty().unbind();
                       progressMessage.setText("Done");
                       progressTitle.textProperty().unbind();
                       setProgressMode(false);
                       Object result = t.getSource().getValue();
                       ArrayList<RegionSummary> foundRegions = new ArrayList<>();
                       if (result instanceof ArrayList){
                           for (Object r: (ArrayList) result){
                               if (r instanceof RegionSummary){
                                   RegionSummary reg = (RegionSummary) r;
                                   foundRegions.add(reg);
                               }
                           }
                           if (foundRegions.size() > 0){
                               FXMLLoader tableLoader = new FXMLLoader(getClass().
                                       getResource("MultiRegionReporter.fxml"));
                               try{
                                    Pane tablePane = (Pane) tableLoader.load();
                                    MultiRegionReporterController multiReg = 
                                            (MultiRegionReporterController) tableLoader.getController();
                                    multiReg.setParentController(thisthis);
                                    Scene tableScene = new Scene(tablePane);
                                    Stage tableStage = new Stage();
                                    tableStage.setScene(tableScene);
                                    //tableScene.getStylesheets().add(SnpViewer.class
                        //.getResource("SnpViewerStyleSheet.css").toExternalForm());
                                    multiReg.displayData(foundRegions);
                                    tableStage.setTitle("Find Regions Results");
                                    tableStage.getIcons().add(new Image(this.getClass().
                    getResourceAsStream("icon.png")));
                                    tableStage.initModality(Modality.NONE);
                                    tableStage.show();
                               }catch (Exception ex){
                                    Alert error = new Alert(AlertType.ERROR);
                                    error.getDialogPane().setPrefSize(420, 200);
                                    error.setResizable(true);
                                    error.setTitle("SnpViewer");
                                    error.setHeaderText("Find Regions Error!");
                                    error.setContentText("Error displaying"
                                           + " results from Find Regions Method.\n" 
                                            + ex.getLocalizedMessage());
                                    ex.printStackTrace();
                                    error.showAndWait();
                               }
                           }else{
                                Alert info = new Alert(AlertType.INFORMATION);
                                info.setResizable(true);
                                info.setTitle("SnpViewer");
                                info.setHeaderText("Find Regions");
                                info.setContentText("No regions found.");
                                info.showAndWait();
                           }
                           savedRegions.addAll(foundRegions);
                           RegionSummary sorter = new RegionSummary();
                           sorter.mergeRegionsByPosition(savedRegions);
                       }
                       saveProject();
                       int c = chromosomeSelector.getSelectionModel().getSelectedIndex();
                       if (c > -1){
                           selectionOverlayPane.getChildren().clear();
                           selectionOverlayPane.getChildren().add(dragSelectRectangle);
                           drawSavedRegions((String) chromosomeBoxList[c]);
                       }
                       progressMessage.setText("");
                       progressTitle.setText("");
                       progressBar.progressProperty().set(0);
                   }
            });
            regionFinder.setOnFailed(new EventHandler<WorkerStateEvent>(){
                   @Override
                   public void handle (WorkerStateEvent t){
                       progressBar.progressProperty().unbind();
                       progressMessage.textProperty().unbind();
                       progressMessage.setText("Failed!");
                       progressTitle.textProperty().unbind();
                       Alert error = new Alert(AlertType.ERROR);
                       error.getDialogPane().setPrefSize(420, 200);
                       error.setResizable(true);
                       error.setTitle("SnpViewer");
                       error.setHeaderText("Find Regions Error!");
                       error.setContentText("Find Regions method failed.\n" + 
                               t.getSource().getException().getLocalizedMessage());
                       t.getSource().getException().printStackTrace();
                       error.showAndWait();
                       setProgressMode(false);
                       progressMessage.setText("");
                       progressTitle.setText("");
                       progressBar.progressProperty().set(0);
                   }
            });
            regionFinder.setOnCancelled(new EventHandler<WorkerStateEvent>(){
                   @Override
                   public void handle (WorkerStateEvent t){
                        progressBar.progressProperty().unbind();
                        progressMessage.textProperty().unbind();
                        progressMessage.setText("Cancelled");
                        progressTitle.textProperty().unbind();
                        Alert error = new Alert(AlertType.ERROR);
                        error.setResizable(true);
                        error.setTitle("SnpViewer");
                        error.setHeaderText("Cancelled");
                        error.setContentText("Find Regions method cancelled by "
                                + "user.\n");
                        error.showAndWait();
                        setProgressMode(false);
                        progressMessage.setText("");
                        progressTitle.setText("");
                        progressBar.progressProperty().set(0);
                        
                   }
            });
            cancelButton.setOnAction(new EventHandler<ActionEvent>(){
               @Override
               public void handle(ActionEvent actionEvent){
                    regionFinder.cancel();

                }
           });
            progressBar.progressProperty().unbind();
            progressMessage.textProperty().unbind();
            progressTitle.textProperty().unbind();
            progressBar.progressProperty().bind(regionFinder.progressProperty());
            progressMessage.textProperty().bind(regionFinder.messageProperty());
            progressTitle.textProperty().bind(regionFinder.titleProperty());
            regionFinder.start();
            
        }catch(IOException ex){
            Alert error = new Alert(AlertType.ERROR);
            error.getDialogPane().setPrefSize(420, 200);
            error.setResizable(true);
            error.setTitle("SnpViewer");
            error.setHeaderText("Find Regions Error");
            error.setContentText("Error starting Find Regions method.\n"
                    + ex.getLocalizedMessage());
            ex.printStackTrace();
            error.showAndWait();
            progressBar.progressProperty().unbind();
            progressMessage.textProperty().unbind();
            progressTitle.textProperty().unbind();
            setProgressMode(false);
        }
        
    }
    
    public void returnToInitialState(){
        setProgressMode(false);
        projectRunning = false; 
        chromSplitPane.getItems().clear();
        labelSplitPane.getItems().clear();
        clearDragSelectRectangle();
        savedRegions.clear();
        savedRegionsDisplay.clear();
        savedRegionsReference.clear();
        selectionOverlayPane.getChildren().clear();
        selectionOverlayPane.getChildren().add(dragSelectRectangle);
        resetObservables();
        newProjectMenu.setDisable(false);
        loadProjectMenu.setDisable(false);
        addAffected.setDisable(true);
        addUnaffected.setDisable(true);
        loadProjectButton.setDisable(false);
        newProjectButton.setDisable(false);
        saveToPngMenu.setDisable(true);
        chromosomeSelector.setDisable(true);
        redrawButton.setDisable(true);
        colorPicker.setDisable(true);
        colorComponantSelector.setDisable(true);
        cacheChromsButton.setDisable(true);      
        cacheChromsMenu.setDisable(true);
        addAffSampleMenu.setDisable(true);
        addUnSampleMenu.setDisable(true);
        nextChromMenu.setDisable(true);
        prevChromMenu.setDisable(true);
        firstChromMenu.setDisable(true);
        lastChromMenu.setDisable(true);
        redrawMenu.setDisable(true);
        autoFindRegions.setDisable(true);
        displaySavedsRegionsMenu.setDisable(true);
        clearSavedRegionsMenu.setDisable(true);
        hideSavedRegionsMenu.setDisable(true);
        outputSavedRegionsMenu.setDisable(true);
        findRegionsButton.setDisable(true);
        removeSampleMenu.setDisable(true);
        cancelButton.setDisable(true);
        projectLabel.setText("");
    }
    
    public boolean checkProjectFolder(File directory, ArrayList<SnpFile> snpFiles){
        for (SnpFile f: snpFiles){
            String fileDir = f.outputDirectory.getName();
            File dirCheck = new File (directory.getAbsolutePath() + "/" + fileDir);
            if (! dirCheck.exists()){
                return false;
            }else{
                f.setOutputDirectory(dirCheck);
            }
            HashMap<String, File> chromFileReplace = new HashMap<>();
            for (String c: f.chromFiles.keySet()){
                File snpViewFile = new File (dirCheck.getAbsolutePath() + "/chr"
                        + c + ".snpview");
                if (! snpViewFile.exists()){
                    return false;
                }else{
                    chromFileReplace.put(c, snpViewFile);
                }
            }
            f.clearChromFiles();
            for (String c: chromFileReplace.keySet()){
                f.addChromFile(c, chromFileReplace.get(c));
            }
        }
        return true;
    }
    
    private void checkQualitySelection(){
        ArrayList<RadioMenuItem> callQualityRadios = new ArrayList<>(Arrays.asList
                (noFilteringRadio, filter99pt9, filter99pt5, filter99, filter95, filter90));
        ArrayList<Double> callQuals = new ArrayList<>(Arrays.asList(null, 0.01, 
                0.05, 0.10, 0.15));
        for (int i = 0; i < callQuals.size(); i++){
            if (callQuals.get(i) != null && qualityFilter != null){
                if(callQuals.get(i).equals(qualityFilter)){
                    callQualityRadios.get(i).setSelected(true);
                    break;
                }
            }else{
                if (callQuals.get(i) == null && qualityFilter == null){
                    callQualityRadios.get(i).setSelected(true);
                    break;
                }
            }
        }
    }
    public void showAbout(ActionEvent ev){
        try{
            FXMLLoader loader = new FXMLLoader(getClass().getResource("about.fxml"));
            Pane page = (Pane) loader.load();
            Scene scene = new Scene(page);
            Stage stage = new Stage();
            stage.setScene(scene);
            //scene.getStylesheets().add(SnpViewer.class
            //            .getResource("SnpViewerStyleSheet.css").toExternalForm());
            AboutController controller = loader.getController();
            controller.setVersion(VERSION);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.getIcons().add(new Image(this.getClass().
                    getResourceAsStream("icon.png")));
            stage.setTitle("About SnpViewer");
            
            stage.show();
        }catch(Exception ex){
            Alert error = new Alert(AlertType.ERROR);
            error.getDialogPane().setPrefSize(420, 200);
            error.setResizable(true);
            error.setTitle("SnpViewer");
            error.setHeaderText("Error");
            error.setContentText("Error showing about information - see"
                    + " details for stack trace.\n" + ex.getLocalizedMessage());
            ex.printStackTrace();
            error.showAndWait();
        }
    }
    
    public enum Colors{
        aa(0), bb(1), ab(2), line(3), fill(4), saveLine(5), saveFill(6);
    
        public int value;
        private Colors(int value) {
            this.value = value;
        }
    }


    
}//end of class
    
