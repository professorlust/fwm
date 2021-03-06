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
import com.forj.fwm.entity.Npc;
import com.forj.fwm.entity.Region;
import com.forj.fwm.entity.Searchable;
import com.forj.fwm.entity.Statblock;
import com.forj.fwm.entity.Template;
import com.forj.fwm.gui.MainController;
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
import javafx.scene.layout.VBox;

public class GodTabController implements MainEntityTab {
	private static Logger log = Logger.getLogger(GodTabController.class);
	private God god;
    private ListController interactionController;
	private AddableImage image;
	@FXML private AddableSound sound;
	@FXML private HBox soundHbox;
	private TextInputControl[] thingsThatCanChange; 
	private RelationalList npcRelation, godRelation, eventRelation, regionRelation, templateRelation;
	private List<God> panth = new ArrayList<God>();
	private SearchList.EntitiesToSearch tabType = SearchList.EntitiesToSearch.GOD;
	
	@FXML private TextField name, pantheon, gender, race, godClass; 
	@FXML private TextArea attributes, description, history;
	@FXML private VBox interactionContainer, rhsVbox;
	@FXML private Tab tabHead;
	@FXML private Button statBlockButton, playButton;
	@FXML private Accordion accordion;
	
	private ChangeListener<String> nameListener = new ChangeListener<String>(){
		public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
			tabHead.setText(newValue);
		}
	};
	
	public RelationalList getTemplateRelation(){
		return templateRelation;
	}
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

	private EventHandler<Event> saveEvent = new EventHandler<Event>(){
		public void handle(Event event){
			log.debug("Save event firing!");
			if(!AppConfig.getManualSaveOnly()){	
				simpleSave();
			}
		}
	};
	private EventHandler<Event> pantheonEvent = new EventHandler<Event>(){
		public void handle(Event event){
			updatePantheon();
		}
	};

	public void startRelationalList() throws Exception {
		log.debug("god.getNpcs.size: " + god.getNpcs().size());
		
		panth.clear();
		panth.addAll(Backend.getGodDao().getPantheon(god));
		
		accordion.getPanes().clear();
		npcRelation = RelationalList.createRelationalList(this, App.toListSearchable(god.getNpcs()), "Worshippers", true, true, tabType, SearchList.EntitiesToSearch.NPC);
		accordion.getPanes().add((TitledPane) npcRelation.getOurRoot());
		
		godRelation = RelationalList.createRelationalList(this, App.toListSearchable(panth), "Pantheon", false, false, tabType, SearchList.EntitiesToSearch.GOD);
		accordion.getPanes().add((TitledPane) godRelation.getOurRoot());
		
		eventRelation = RelationalList.createRelationalList(this, App.toListSearchable(god.getEvents()), com.forj.fwm.entity.Event.WHAT_IT_DO + "s", true, true, tabType, SearchList.EntitiesToSearch.EVENT);
		accordion.getPanes().add((TitledPane) eventRelation.getOurRoot());
		
		regionRelation = RelationalList.createRelationalList(this, App.toListSearchable(god.getRegions()), "Regions", true, true, tabType, SearchList.EntitiesToSearch.REGION);
		accordion.getPanes().add((TitledPane) regionRelation.getOurRoot());
		
		templateRelation = RelationalList.createRelationalList(this, App.toListSearchable(god.getTemplates()), "Templates", true, true, tabType, SearchList.EntitiesToSearch.TEMPLATE);
		accordion.getPanes().add((TitledPane) templateRelation.getOurRoot());

	}
	
	private void updatePantheon(){
		if(tabHead.getText()!=null)
		{
			if(!AppConfig.getManualSaveOnly()){
				fullSave();
			}
		}
		panth.clear();
		panth.addAll(Backend.getGodDao().getPantheon(god));
		godRelation.clearList();
		godRelation.populateList(App.toListSearchable(panth));

	}
	
	// when F5 get's hit or smth. 
	public void manualUpdateTab(){
		log.debug("update tab on god tab ID: " + god.getID() + " was called.");
		try {
			god = Backend.getGodDao().getFullGod(god.getID());
			setAllTexts(god);
			try{
				startRelationalList();
			}catch(Exception e){
				log.error(e);
				e.printStackTrace();
			}
			Backend.getGodDao().update(god);
			Backend.getGodDao().refresh(god);
			
		} catch (SQLException e) {
			log.error(e);
			e.printStackTrace();
		}
	}
	
	// NOTE: Probably much more important than just using for pantheon. Updates objects in other tabs
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
		
		god.setNpcs(new ArrayList<Npc>((List<Npc>)(List<?>)npcRelation.getList()));
		
		god.setInteractions(new ArrayList<Interaction>((List<Interaction>)(List<?>)interactionController.getAllInteractions()));
		god.setRegions(new ArrayList<Region>((List<Region>)(List<?>)regionRelation.getList()));	
		god.setEvents(new ArrayList<com.forj.fwm.entity.Event>((List<com.forj.fwm.entity.Event>)(List<?>)eventRelation.getList()));	
		god.setTemplates(new ArrayList<Template>((List<Template>)(List<?>)templateRelation.getList()));
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
			Backend.getGodDao().saveFullGod(god);
			log.debug("Save successfull!");
			log.debug("god id: " + god.getID());
			App.getMainController().addStatus("Successfully saved full God " + god.getName() + " ID: " + god.getID());
		}catch(SQLException e){
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
			Backend.SaveSimpleSearchable(god);
			log.debug("Save successfull!");
			log.debug("god id: " + god.getID());
			App.getMainController().addStatus("Successfully saved base God " + god.getName() + " ID: " + god.getID());
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
			Backend.getGodDao().saveRelationalGod(god);
			log.debug("Save successfull!");
			log.debug("god id: " + god.getID());
			App.getMainController().addStatus("Successfully saved God relations" + god.getName() + " ID: " + god.getID());
		}catch(SQLException e){
			log.error(e);
			e.printStackTrace();
		}
	}
	
	
	public void start(Tab rootLayout, God god) throws Exception {
		this.god = god;
				
		interactionController = ListController.startInteractionList(god.getInteractions(), this);
		interactionContainer.getChildren().add(interactionController.getInteractionListContainer());
		
		if(App.worldFileUtil.findMultimedia(god.getSoundFileName()) != null)
		{
			log.debug(App.worldFileUtil.findMultimedia(god.getSoundFileName()));
			sound = new AddableSound(this, App.worldFileUtil.findMultimedia(god.getSoundFileName()));
		}
		else
		{
			sound = new AddableSound(this);
		}
		soundHbox.getChildren().add(sound);
		
		if(App.worldFileUtil.findMultimedia(god.getImageFileName()) != null)
		{
			image = new AddableImage(App.worldFileUtil.findMultimedia(god.getImageFileName()));
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
		thingsThatCanChange = new TextInputControl[] {history, description, attributes, godClass, race, gender, pantheon, name};
		name.textProperty().addListener(nameListener);
		log.debug("start god tab controller called");
		setAllTexts(god);
		
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
		
		thingsThatCanChange[6].setOnKeyReleased(pantheonEvent);
		updatePantheon();
		
//		App.getMainController().getTabPane().getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
//		    public void changed(ObservableValue<? extends Tab> observable, Tab oldTab, Tab newTab) {
//		        if(newTab.equals(getTab())) {
//		        	autoUpdateTab();
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

	private void setAllTexts(God god){
		if(god.getStatblock() != null)
		{
			try {
				god.setStatblock(Backend.getStatblockDao().queryForSameId(god.getStatblock()));
			} catch (SQLException e) {
				log.error(e);
			}
		}
		tabHead.setText(god.getName());
		history.setText(god.getHistory());
		description.setText(god.getDescription());
		attributes.setText(god.getAttributes());
		godClass.setText(god.getClass_());
		race.setText(god.getRace());
		gender.setText(god.getGender());
		name.setText(god.getName());
		pantheon.setText(god.getPantheon());
	}
	
	private void getAllTexts()
	{
		god.setImageFileName(image.getFilename());
		log.debug("saving:" + sound.getFilename());
		god.setSoundFileName(sound.getFilename());
		god.setHistory(history.getText());
		god.setDescription(description.getText());
		god.setGender(gender.getText());
		god.setClass_(godClass.getText());
		god.setRace(race.getText());
		god.setAttributes(attributes.getText());
		god.setName(name.getText());
		god.setPantheon(pantheon.getText());
	}
	
	private static boolean started = false;

	
	public static boolean getStarted() {
		return started;
	}

	public static GodTabController startGodTab(God god) throws Exception {
		log.debug("static startGodTab called.");
		God ourG = god;
		if(god != null){
			log.debug("ourG got filled from backend");
			ourG = Backend.getGodDao().getFullGod(god.getID());
		}else
		{
			ourG = new God();
		}
		FXMLLoader loader = new FXMLLoader();
		loader.setLocation(GodTabController.class.getResource("godTab.fxml"));
		Tab rootLayout = (Tab)loader.load();
		GodTabController cr = (GodTabController)loader.getController();
		cr.start(rootLayout, ourG);
		started = true;
		return cr;
	}
	
	@FXML
	public void showStatBlock() throws Exception{
		if(god.getID() != -1){
			log.debug("statblock is being brought up.");
			if (god.getStatblock() == null)
			{
				log.debug("statblock is null.");
				god.setStatblock(new Statblock());
				god.getStatblock().setDescription(DefaultStatblockBackend.getGodStat().getDescription());
			}	
		App.getStatBlockController().show(god.getStatblock(), this);
		}
		else{
			App.getMainController().addStatus("Error: Cannot open statblock of unsaved object.");
		}
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
		return god;
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
