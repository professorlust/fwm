package com.forj.fwm.gui;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.forj.fwm.conf.AppConfig;
import com.forj.fwm.conf.WorldConfig;
import com.forj.fwm.entity.Npc;
import com.forj.fwm.entity.Searchable;
import com.forj.fwm.entity.Template;
import com.forj.fwm.gui.SearchList.EntitiesToSearch;
import com.forj.fwm.gui.component.Openable;
import com.forj.fwm.gui.component.Saveable;
import com.forj.fwm.startup.App;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

public class RelationalList implements Openable{
	private Node ourRoot;

	private static int maxImageSize = 16;
	
	private EntitiesToSearch tabType;
	private EntitiesToSearch relationType;
	
	private static Logger log = Logger.getLogger(RelationalList.class);
	
	private ObservableList<Searchable> items = FXCollections.observableArrayList();
	
	// @Jay how do you feel about @FXML on the same line for variables? 
	// I was getting eye cancer from full screens of FXML variables
	@FXML private ListView<Searchable> listView = new ListView<Searchable>();
	@FXML private Button addButton;
	@FXML private StackPane stackPane, listPane;
	@FXML private TitledPane titledPane;
	@FXML private VBox vbox;
	private boolean page1 = true;

	private ArrayList<Searchable> searchResults = new ArrayList<Searchable>();
	private SearchList sl;

	private Saveable caller;
	private Searchable tabObject;
	
	private boolean showButton;
	private boolean relationsRemovable;
	private boolean isTemplate = false;
	
	public static RelationalList createRelationalList(Saveable caller, List<Searchable> ourItems, String title, boolean useButton, boolean relationsRemovable, EntitiesToSearch tabType, EntitiesToSearch relationType) throws IOException {
		FXMLLoader fxmlLoader = new FXMLLoader();
		fxmlLoader.setLocation(RelationalList.class.getResource("relationalList.fxml"));
		Node n = (Node) fxmlLoader.load();
		RelationalList rl = fxmlLoader.getController();
		rl.start(tabType, relationType, n, caller, ourItems, useButton, relationsRemovable, title);
		return rl;
	}

	public void start(EntitiesToSearch t, EntitiesToSearch r, Node n, Saveable caller, List<Searchable> ourItems, boolean useButton, boolean removable, String title) {
		this.tabType = t;
		this.relationType = r;
		this.ourRoot = n;
		this.caller = caller;
		this.showButton = useButton;
		this.relationsRemovable = removable;
		this.titledPane = (TitledPane) n;
		addButton.setContentDisplay(ContentDisplay.RIGHT);
		listPane.setMouseTransparent(true);
		listPane.setVisible(false);
		titledPane.setAnimated(false);
		if (!showButton) {
			addButton.setMouseTransparent(true);
			addButton.setVisible(false);
		}
		if (r == EntitiesToSearch.TEMPLATE) {
			this.isTemplate = true;
		}
		
		// set reference to the object owned by the tab
		tabObject = caller.getThing();
		populateList(ourItems);
			
			
		updateList();
		setTitle(title);
		
		
	}
	
	public void updateList() {
		items = FXCollections.observableArrayList(searchResults);
		listView.setItems(items);
		
		listView.setOnKeyPressed(new EventHandler<KeyEvent>(){
			public void handle(KeyEvent event) {
				if(event.getCode().equals(KeyCode.ENTER)){
					try{
						App.getMainController().open(listView.getSelectionModel().getSelectedItem());
					}catch(Exception e){
						e.printStackTrace();
					}
				}
				if(event.getCode().equals(KeyCode.DELETE)){
					removeItem(listView.getSelectionModel().getSelectedItem());
				}
				if(event.getCode().equals(KeyCode.ESCAPE)){
					escape();
				}
			}
		});
		// JavaFX cancer
		listView.setCellFactory(new Callback<ListView<Searchable>, ListCell<Searchable>>() {
			public ListCell<Searchable> call(ListView<Searchable> param) {
				return new ListCell<Searchable>() {
					private void openObj(){
						try{
							App.getMainController().open(this.getItem());	
						}catch(Exception e)
						{
							e.printStackTrace();
						}
					}
					public void updateItem(final Searchable obj, boolean empty) {
						super.updateItem(obj, empty);
						
						final ContextMenu contextMenu = new ContextMenu();
				        MenuItem deleteMenuItem = new MenuItem("Remove");
				        
				        
				        deleteMenuItem.setOnAction(new EventHandler<ActionEvent>() {
				            public void handle(ActionEvent e) {
				                removeItem(obj);
				                log.debug("removed relation");
				            }
				        });
				        contextMenu.getItems().addAll(deleteMenuItem);
				        if (isTemplate) {
				        	MenuItem templateItem = new MenuItem("Create NPC from template");
					        templateItem.setOnAction(new EventHandler<ActionEvent>() {
					            public void handle(ActionEvent e) {
					                try {
										Npc n = ((Template) obj).newFromTemplate();
										App.getMainController().open(n);
										log.debug("trying to create new");
									} catch (SQLException e1) {
										log.debug(e1);
									} catch (Exception e1) {
										log.debug(e1);
									}
					            }
					        });
					        contextMenu.getItems().add(templateItem);					       
				        }			
				        
				        this.setContextMenu(contextMenu);
				        final ListCell<Searchable> thing = this;
				        
						this.setOnMouseClicked(new EventHandler<MouseEvent>(){
							public void handle(MouseEvent event) {
								if (event.getButton() ==  MouseButton.SECONDARY && relationsRemovable){
									contextMenu.show(thing, event.getScreenX(), event.getScreenY());
								} else {
									openObj();
								}
							}
						} );
						
						if(obj == null){
							setText(null);
							setGraphic(null);
							setHeight(0);
							setWidth(0);
							return;
						}
						ImageView imageView = new ImageView();
						String name = "";
						if (obj != null) {
							// TODO: Region should have hierarchy??
							name = obj.getShownName();
							try {
								
								imageView.setImage(new Image(
										App.worldFileUtil.findMultimedia(obj.getImageFileName()).toURI().toString(), true));
							} catch (NullPointerException e) {
								// log.info("No Image Set for object -> " +
								// name);
								imageView.setImage(
										new Image(App.retGlobalResource("/src/main/ui/no_image_icon.png").toString()));
							}
						}
						if (empty) {
							setText(null);
							setGraphic(null);
						} else {
							setText(name);
							imageView.setFitHeight(maxImageSize);
							imageView.setFitWidth(maxImageSize);
							imageView.setPreserveRatio(true);
							setGraphic(imageView);
						}
					}
				};
			}
		});
	}
	
	@FXML
	public void escape() {
		if (!page1){
			handleAddButton();
		}
	}
	
	public void clearList() {
		listView.getItems().clear();
		searchResults.clear();
	}
	
	private boolean checkUnique(Searchable item) {
		boolean unique = true;
		for (Searchable i : searchResults) {
			if (i.getID() == item.getID()) {
				if (i.getClass().equals(item.getClass())) {
					unique = false;
					break;
				}
				
			}
		}
		return unique;
	}

	public void addItem(Searchable item, boolean update) {
		if (checkUnique(item)) {
			listView.getItems().add(item);
			searchResults.add(item);
			if (update) {
				updateList();
			}
		}
		
	}
	
	public void removeItem(Searchable item) {
		listView.getItems().remove(item);
		searchResults.remove(item);
		log.debug("Removing an item, and attempting to save changes to tab.");
		if(!AppConfig.getManualSaveOnly()){
			caller.relationalSave();
		}
		updateList();
	}

	@FXML
	public void handleAddButton() {
		if (page1)
		{
			listPane.setMouseTransparent(false);
			listPane.setVisible(true);
			
			listView.setMouseTransparent(true);
			listView.setVisible(false);
			
			listPane.getChildren().clear();
			
			try {
				sl = SearchList.createSearchList(relationType, this);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			listPane.getChildren().add(sl.getOurRoot());
			sl.setMaxImageSize(maxImageSize);
			titledPane.setExpanded(true);
			Accordion ac = (Accordion)titledPane.getParent();
//			ac.getPanes().get(0).setExpanded(true);
			sl.getSearchField().requestFocus();
			
			sl.getSearchField().setOnKeyPressed(new EventHandler<KeyEvent>(){
				public void handle(KeyEvent event) {
					if(event.getCode().equals(KeyCode.ESCAPE)){
						escape();
					}
				}
			});
					
			sl.getSearchField().focusedProperty().addListener(new ChangeListener<Boolean>() {
			    public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) {
			        if (!newPropertyValue) {
			            if (!sl.getSearchField().isFocused() && !sl.getList().isFocused()) {
			            	escape();
			            }
			        }
			    }
			});
			
			sl.getList().focusedProperty().addListener(new ChangeListener<Boolean>() {
			    public void changed(ObservableValue<? extends Boolean> arg0, Boolean oldPropertyValue, Boolean newPropertyValue) {
			        if (!newPropertyValue){
			            if (!sl.getSearchField().isFocused() && !sl.getList().isFocused()) {
			            	escape();
			            }
			        }
			    }
			});

			page1 = false;
		}
		else {
			listPane.setMouseTransparent(true);
			listPane.setVisible(false);
			
			listView.setMouseTransparent(false);
			listView.setVisible(true);

			page1 = true;
		}		
	}
	
	public void populateList(List<Searchable> ourItems) {
		for(Searchable s: ourItems){
			addItem(s, false);
		}
		updateList();
	}
	
	public void open(Searchable o) throws Exception {
		log.debug("opening tab from search");
		
		
		searchResults.add(o);
		populateList(searchResults);
		updateList();
		handleAddButton();
		if(!AppConfig.getManualSaveOnly()){
			caller.relationalSave();
		}
		//TODO fuck whoever left this in? ... 
		// research on left hand side? ... 
		// App.getMainController().getSearchList().searchDB();
		
	}

	public ListView<Searchable> getListView(){
		return listView;
	}
	
	public void setTitle(String newText) {
		titledPane.setText(newText);
	}
	
	public List<Searchable> getList() {
		return listView.getItems();
	}
	
	public void setMaxImageSize(int newSize) {
		maxImageSize = newSize;
	}

	public void setEntitiesToSearch(EntitiesToSearch e) {
		tabType = e;
	}

	public Node getOurRoot() {
		return ourRoot;
	}
	
	public void setButtonText(String newText) {
		addButton.setText(newText);
	}
	
	public Searchable getTabObject() {
		return tabObject;
	}
}
