package com.forj.fwm.gui.tab;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.forj.fwm.backend.Backend;
import com.forj.fwm.backend.DefaultStatblockBackend;
import com.forj.fwm.conf.AppConfig;
import com.forj.fwm.conf.WorldConfig;
import com.forj.fwm.entity.God;
import com.forj.fwm.entity.Interaction;
import com.forj.fwm.entity.MMTemplateRegion;
import com.forj.fwm.entity.Npc;
import com.forj.fwm.entity.Region;
import com.forj.fwm.entity.Searchable;
import com.forj.fwm.entity.Statblock;
import com.forj.fwm.entity.Template;
import com.forj.fwm.gui.MainController;
import com.forj.fwm.gui.RelationalField;
import com.forj.fwm.gui.RelationalList;
import com.forj.fwm.gui.SearchList;
import com.forj.fwm.gui.InteractionList.ListController;
import com.forj.fwm.gui.component.AddableImage;
import com.forj.fwm.gui.component.AddableSound;
import com.forj.fwm.gui.component.MainEntityTab;
import com.forj.fwm.gui.component.TabControlled;
import com.forj.fwm.startup.App;
import com.sun.javafx.scene.control.skin.TextAreaSkin;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class RegionTabController implements MainEntityTab {
	private static Logger log = Logger.getLogger(RegionTabController.class);
	private Region region;
	private ListController interactionController;
	private AddableImage image;
    private AddableSound sound;
    private TextInputControl[] thingsThatCanChange;
	private RelationalList npcRelation, godRelation, eventRelation, regionRelation, templateRelation;
	private SearchList.EntitiesToSearch tabType = SearchList.EntitiesToSearch.REGION;
	private RelationalField superRelation;
	private List<Region> superList = new ArrayList<Region>();
	
    @FXML private TextField name;
	@FXML private TextArea attributes, description, history;
	@FXML private VBox interactionContainer, rhsVbox;
	@FXML private Tab tabHead;
	@FXML private Accordion accordion;
	@FXML private Button statBlockButton, playButton;
	@FXML private StackPane superRegionPane;
	@FXML private HBox soundHbox;
	
	public RelationalList getNpcRelation(){
		return npcRelation;
	}
	public RelationalList getGodRelation(){
		return godRelation;
	}
	public RelationalList getEventRelation(){
		return eventRelation;
	}
	public RelationalList getRegionRelation(){
		return regionRelation;
	}
	public RelationalList getTemplateRelation(){
		return templateRelation;
	}

	private ChangeListener<String> nameListener = new ChangeListener<String>(){
		public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
			tabHead.setText(newValue);
		}		
	};
	
	private EventHandler<Event> saveEvent = new EventHandler<Event>(){
		public void handle(Event event){
			log.debug("Save event firing!");
			if(!AppConfig.getManualSaveOnly()){
				simpleSave();
			}
		}
	};
	
	public void startRelationalList() throws Exception {
		
		superList.clear();
		if (region.getSuperRegion() != null){
			if (!Backend.getRegionDao().queryForEq("ID", region.getSuperRegion().getID()).isEmpty()) {
				region.getSuperRegion().setName(Backend.getRegionDao().queryForEq("ID", region.getSuperRegion().getID()).get(0).getName());
				superList.add(region.getSuperRegion());
			}
		}
		superRelation = RelationalField.createRelationalList(this, App.toListSearchable(superList), "Super Region", true, true, tabType, SearchList.EntitiesToSearch.REGION);
		superRegionPane.getChildren().add(superRelation.getOurRoot());
		
		accordion.getPanes().clear();
		regionRelation = RelationalList.createRelationalList(this, App.toListSearchable(region.getSubRegions()), "Sub Regions", true, true, tabType, SearchList.EntitiesToSearch.REGION);
		accordion.getPanes().add((TitledPane) regionRelation.getOurRoot());
		
		npcRelation = RelationalList.createRelationalList(this, App.toListSearchable(region.getNpcs()), "NPCs", true, true, tabType, SearchList.EntitiesToSearch.NPC);
		accordion.getPanes().add((TitledPane) npcRelation.getOurRoot());
		
		godRelation = RelationalList.createRelationalList(this, App.toListSearchable(region.getGods()), "Gods", true, true, tabType, SearchList.EntitiesToSearch.GOD);
		accordion.getPanes().add((TitledPane) godRelation.getOurRoot());
		
		eventRelation = RelationalList.createRelationalList(this, App.toListSearchable(region.getEvents()), com.forj.fwm.entity.Event.WHAT_IT_DO + "s", true, true, tabType, SearchList.EntitiesToSearch.EVENT);
		accordion.getPanes().add((TitledPane) eventRelation.getOurRoot());
		
		templateRelation = RelationalList.createRelationalList(this, App.toListSearchable(region.getTemplates()), "Templates", true, true, tabType, SearchList.EntitiesToSearch.TEMPLATE);
		accordion.getPanes().add((TitledPane) templateRelation.getOurRoot());
		
	}
	
	// when F5 get's hit or smth. 
	public void manualUpdateTab(){
		log.debug("update tab on god tab ID: " + region.getID() + " was called.");
		try {
			region = Backend.getRegionDao().getFullRegion(region.getID());
			setAllTexts(region);
			try{
				startRelationalList();
			}catch(Exception e){
				log.error(e);
				e.printStackTrace();
			}
			Backend.getRegionDao().update(region);
			Backend.getRegionDao().refresh(region);
		} catch (SQLException e) {
			log.error(e);
			e.printStackTrace();
		}
	}
				
	public void autoUpdateTab(){
		// this shouldn't occur if they have manual saving only on, because that will dump their data. 
		if(!AppConfig.getManualSaveOnly()){
			manualUpdateTab();
		}
		else{
			App.getMainController().addStatus(TabControlled.DID_NOT_AUTO_UPDATE);
			// pass, we just changed but there could be unsaved information. 
		}
	}
	
	public void getAllRelations(){
		log.debug("in get all relations.");
		for(Npc n: (List<Npc>)(List<?>)npcRelation.getList()){
			log.debug("\t" + n.getID());
		}
		region.setInteractions(new ArrayList<Interaction>((List<Interaction>)(List<?>)interactionController.getAllInteractions()));
		region.setGods(new ArrayList<God>((List<God>)(List<?>)godRelation.getList()));
		region.setNpcs(new ArrayList<Npc>((List<Npc>)(List<?>)npcRelation.getList()));
		region.setSubRegions(new ArrayList<Region>((List<Region>)(List<?>)regionRelation.getList()));
		region.setEvents(new ArrayList<com.forj.fwm.entity.Event>((List<com.forj.fwm.entity.Event>)(List<?>)eventRelation.getList()));
		region.setTemplates(new ArrayList<Template>((List<Template>)(List<?>)templateRelation.getList()));
		if (!superRelation.getList().isEmpty()){
			Region newRegion = (new ArrayList<Region>((List<Region>)(List<?>)superRelation.getList())).get(0);
			region.setSuperRegion(newRegion);
			log.debug("spitting out super region IDS.");
			log.debug(newRegion.getID());
		}
		log.debug("spitting out sub region IDS.");
		for(Region r: region.getSubRegions()){
			log.debug(r.getID());
		}
	}
	
	@FXML
	public void fullSave(){
		getAllTexts();
		getAllRelations();
		if(tabHead.getText()!=null && !tabHead.getText().equals(""))
		{
			// pass
		}
		else {
			log.debug("can't save, no name");
			App.getMainController().addStatus("Unable to save without a name.");
			return;
		}
		try{
			Backend.getRegionDao().saveFullRegion(region);
			Backend.getRegionDao().refresh(region);
			log.debug("Save successfull!");
			log.debug("region id: " + region.getID());
			App.getMainController().addStatus("Successfully saved full Region " + region.getName() + " ID: " + region.getID());
		}catch(SQLException e){
			e.printStackTrace();
			log.error(e);
		}
	}
	
	public void simpleSave() {
		getAllTexts();
		if(tabHead.getText()!=null && !tabHead.getText().equals(""))
		{
			// pass
		}
		else {
			log.debug("can't save, no name");
			App.getMainController().addStatus("Unable to save without a name.");
			return;
		}
		try{
			Backend.SaveSimpleSearchable(region);
			log.debug("Save successfull!");
			log.debug("region id: " + region.getID());
			App.getMainController().addStatus("Successfully saved base Region " + region.getName() + " ID: " + region.getID());
		}catch(SQLException e){
			log.error(e);
			e.printStackTrace();
		}
	}

	public void relationalSave() {
		getAllRelations();
		if(tabHead.getText()!=null && !tabHead.getText().equals(""))
		{
			// pass
		}
		else {
			log.debug("can't save, no name");
			App.getMainController().addStatus("Unable to save without a name.");
			return;
		}
		try{
			Backend.getRegionDao().saveRelationalRegion(region);
			log.debug("Save successfull!");
			log.debug("region id: " + region.getID());
			App.getMainController().addStatus("Successfully saved Region relations" + region.getName() + " ID: " + region.getID());
		}catch(SQLException e){
			log.error(e);
			e.printStackTrace();
		}
	}
	
	
	public void start(Tab rootLayout, Region region) throws Exception {
		this.region = region;

		interactionController = ListController.startInteractionList(region.getInteractions(), this);
		interactionContainer.getChildren().add(interactionController.getInteractionListContainer());
		
		
		if(App.worldFileUtil.findMultimedia(region.getSoundFileName()) != null)
		{
			sound = new AddableSound(this, App.worldFileUtil.findMultimedia(region.getSoundFileName()));
		}
		else
		{
			sound = new AddableSound(this);
		}
		soundHbox.getChildren().add(sound);
		
		if(App.worldFileUtil.findMultimedia(region.getImageFileName()) != null)
		{
			image = new AddableImage(App.worldFileUtil.findMultimedia(region.getImageFileName()));
		}
		else
		{
			image = new AddableImage();
		}
		image.setOnImageChanged(new EventHandler<Event>(){
			public void handle(Event event) {
				if(!AppConfig.getManualSaveOnly()){
					fullSave();
				}
			}
		});
		image.setVisible(true);
		rhsVbox.getChildren().add(0, image);
		
		
		thingsThatCanChange = new TextInputControl[] {history, description, attributes, name};
		name.textProperty().addListener(nameListener);
		log.debug("start region tab controller called");
		setAllTexts(region);
		
		for(TextInputControl c: thingsThatCanChange){
			if (c.getClass() == TextArea.class){
				c.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
					public void handle(KeyEvent event) {
					    if (event.getCode().equals(KeyCode.TAB)) {
					        Node node = (Node) event.getSource();
					        if (node instanceof TextField) {
					            TextAreaSkin skin = (TextAreaSkin) ((TextField)node).getSkin();
					            if (event.isShiftDown()) {
					                skin.getBehavior().traversePrevious();
					            }
					            else {
					                skin.getBehavior().traverseNext();
					            }               
					        }
					        else if (node instanceof TextArea) {
					            TextAreaSkin skin = (TextAreaSkin) ((TextArea)node).getSkin();
					            if (event.isShiftDown()) {
					                skin.getBehavior().traversePrevious();
					            }
					            else {
					                skin.getBehavior().traverseNext();
					            }
					        }

					        event.consume();
					    }
					}
			    });
			}
			c.setOnKeyReleased(saveEvent);
		}
		
		startRelationalList();
		
//		App.getMainController().getTabPane().getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
//		    public void changed(ObservableValue<? extends Tab> observable, Tab oldTab, Tab newTab) {
//		        if(newTab == getTab()) {
//		        	updateTab();
//		        }
//		    }
//		});
		
		
		Platform.runLater(new Runnable() {
			public void run() {
				name.requestFocus();
			}
		});
		
		tabHead.setOnSelectionChanged(new EventHandler(){
			public void handle(Event arg0) {
				sound.stop();
			}
		});
		
		started = true;
	}

	private void setAllTexts(Region region){
		if(region.getStatblock() != null)
		{
			try {
				region.setStatblock(Backend.getStatblockDao().queryForSameId(region.getStatblock()));
			} catch (SQLException e) {
				log.error(e);
			}
		}
		tabHead.setText(region.getName());
		history.setText(region.getHistory());
		description.setText(region.getDescription());
		attributes.setText(region.getAttributes());
		name.setText(region.getName());
		// works better when it has this, for some reason. 
		if(region.getSuperRegion() != null)
		{
			try {
				region.setSuperRegion(Backend.getRegionDao().queryForSameId(region.getSuperRegion()));
			} catch (SQLException e) {
				log.error(e);
			}
		}
	}
	
	private void getAllTexts()
	{
		region.setImageFileName(image.getFilename());
		region.setSoundFileName(sound.getFilename());
		region.setHistory(history.getText());
		region.setDescription(description.getText());
		region.setAttributes(attributes.getText());
		region.setName(name.getText());
		
	}
	
	private static boolean started = false;

	public static boolean getStarted() {
		return started;
	}


	public static RegionTabController startRegionTab(Region region) throws Exception {
		log.debug("static startRegionTab called.");

		Region ourR = region;
		if(region != null){
			log.debug("ourR got filled from backend");
			ourR = Backend.getRegionDao().getFullRegion(region.getID());
		}else
		{
			ourR = new Region();
			
		}
		
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(RegionTabController.class.getResource("regionTab.fxml"));
		Tab rootLayout = (Tab)loader.load();
		RegionTabController cr = (RegionTabController)loader.getController();
		cr.start(rootLayout, ourR);
		started = true;
		return cr;
		
	}
	
	
	
	@FXML
	public void changeSound() throws Exception{
		sound.changeSound();
		if(!AppConfig.getManualSaveOnly()){
			fullSave();
		}
		sound.play();
		
	}
	
	@FXML
	public void showStatBlock() throws Exception{
		if(region.getID() != -1){
			log.debug("statblock is being brought up.");
			if (region.getStatblock() == null)
			{
				log.debug("statblock is null.");
				region.setStatblock(new Statblock());
				region.getStatblock().setDescription(DefaultStatblockBackend.getRegionStat().getDescription());
			}	
			App.getStatBlockController().show(region.getStatblock(), this);
		}
		else{
			App.getMainController().addStatus("Error: Cannot open statblock of unsaved object.");
		}
	}
	
	
	@FXML 
	public void playSound() throws Exception{
		if(sound != null && sound.hasSound())
		{
			if (!sound.isPlaying()) {
				sound.play();
				log.debug("not playing. So play it.");
			} else {
				sound.stop();
				log.debug("playing. so stop it");
			}
			
		} else  {
			log.debug("nosound");
		}
	}
	
	public Tab getTab(){
		return tabHead;
	}

	public Searchable getThing() {
		return region;
	}
	
	public AddableImage getAddableImage() {
		return image;
	}
	
	public ListController getListController(){
		return interactionController;
	}
	public void nameFocus(){
		name.requestFocus();
	}
	public AddableSound getAddableSound(){
		return sound;
	}
	public Accordion getAccordion(){
		return accordion;
	}	
	public Button getPlayButton(){
		return playButton;
	}
}
