package application;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


public class Main extends Application {
	
	public static final String SEPARATOR = System.getProperty("file.separator");
	
	public static final String WPL_EXT = ".wpla";
	public static final String WPLPJ_FILE = "project.wpj";
	
	public static final String PJ_DATE_KEY = "#PJDATE#";
	public static final String PJ_NAME_KEY = "#PJNAME#";
	
	public static final String PJ_PATH_KEY = "#PJPATH#";
	public static final String PJ_TABS_KEY = "#PJTABS#";
	public static final String PJ_SELTAB_KEY = "#PJSELTAB#";
	
	public static final String FOLDER_WPL = "wpl";
	public static final String FOLDER_MEDIA = "media";
	public static final String FOLDER_STYLES = "styles";
	public static final String FOLDER_SCRIPTS = "scripts";
	public static final String FOLDER_OTHER = "other";
	public static final String FOLDER_OUTPUT = "output";
	
	private static Stage stage;
	private static String projectPath;
	private static Hierarchy hierarchy;
	private static RichTextCode editor;
	private static String pjname = "";
	private static String date = "";
	
	
	@Override
	public void start(Stage primaryStage) {
		//standardIO2File("");
		Platform.setImplicitExit(false);
		stage = primaryStage;
		stage.getIcons().add(new Image(getClass().getResourceAsStream("logo16x16.png")));
		stage.getIcons().add(new Image(getClass().getResourceAsStream("logo32x32.png")));
		stage.getIcons().add(new Image(getClass().getResourceAsStream("logo48x48.png")));
		stage.getIcons().add(new Image(getClass().getResourceAsStream("logo64x64.png")));
		primaryStage.setFullScreen(true);
		
		// Consumimos el evento de salir para que se ejecute nuestro codigo por defecto
		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
		    @Override
		    public void handle(WindowEvent event) {
		        event.consume();
		        beforeExit();
		    }
		});
		
		try {
			BorderPane root = new BorderPane();
			Scene scene = new Scene(root,800,600);
			
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.setTitle("IDE WPL");
			primaryStage.show();
			
			MenuPane menu = new MenuPane();
			root.setTop(menu.getMenu());
			
			BorderPane toolMidPane = new BorderPane();
			Toolbar tb = new Toolbar();
			toolMidPane.setTop(tb.getToolbar());
			
			SplitPane hsp = new SplitPane();
			hsp.setId("middlepane");
			hsp.setOrientation(Orientation.HORIZONTAL);
			hsp.setDividerPositions(0.25);
			
			
			SplitPane codeConsole = new SplitPane();
			
			editor = new RichTextCode();
			
			codeConsole.getItems().addAll(editor.getCodeEditor(), loadConsole());
			codeConsole.setOrientation(Orientation.VERTICAL);
			codeConsole.setDividerPositions(0.7);
			
			hierarchy = new Hierarchy();
			
			hsp.getItems().addAll(hierarchy.getHierarchy(), codeConsole);
			
			toolMidPane.setCenter(hsp);	
			
			root.setCenter(toolMidPane);
			root.setBottom(loadFooter());
						
		} catch(Exception e) {
			e.printStackTrace();
		}

	}
	
	public static void main(String[] args) {
		launch(args);
	}
	

	/** Carga la consola de errores */
	public static Pane loadConsole() {
		Pane p = new Pane();
		p.setId("consolepane");
		SplitPane.setResizableWithParent(p, false);
		
		return p;
	}
	
	/** Carga la barra inferior que contiene la barra de progreso */
	public static BorderPane loadFooter() {
		BorderPane footer = new BorderPane();
		
		Label lversion = new Label("Version 1.0");
		lversion.setId("lblversion");
		ProgressBar pbar = new ProgressBar();
		
		footer.setLeft(lversion);
		footer.setRight(pbar);
		
		footer.setId("footer");
		
		return footer;
	}
	
	public static void toggleFullScreen() {		
		stage.setFullScreen(!stage.isFullScreen());
	}
	
	public static Stage getStage() {
		return stage;
	}
	
	public static void setProjectPath(String path) {
		projectPath = path;
	}
	
	public static String getProjectPath() {
		return projectPath;
	}
	
	public static void openTab(String path) {
		editor.addTab(path);
	}
	
	public static void saveCurrentTab() {
		editor.saveCurrentTab();
	}
	
	public static void saveAllTabs() {
		editor.saveAllTabs();
	}
	
	public static void loadWorkspace() {
		IOManager reader = new IOManager();
		try {
			reader.open(getProjectPath()+WPLPJ_FILE, true, false);
			String line;
			while((line = reader.getLine()) != null) {
				if(line.startsWith(PJ_TABS_KEY)) {
					line = line.replaceAll(PJ_TABS_KEY, "");
					// Si existe el fichero
					if(new File(line).exists()) {
						openTab(line);
					}
				}
				if(line.startsWith(PJ_SELTAB_KEY)) {
					line = line.replaceAll(PJ_SELTAB_KEY, "");
					// Si existe el fichero
					if(new File(line).exists()) {
						editor.selectTab(line);
					}
				}
				if(line.startsWith(PJ_NAME_KEY)) {
					line = line.replaceAll(PJ_NAME_KEY, "");
					pjname = line;			
				}
				if(line.startsWith(PJ_DATE_KEY)) {
					line = line.replaceAll(PJ_DATE_KEY, "");
					date = line;			
				}
			}
			// Y refrescamos
			hierarchy.reload();
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void saveWorkspace() {
		IOManager writer = new IOManager();
		try {
			writer.open(getProjectPath()+WPLPJ_FILE, false, false);
			if(pjname.length() == 0) {
				if(projectPath != null) {
					String[] pathdiv = projectPath.split(SEPARATOR);
					if(pathdiv != null) {
						pjname = pathdiv[pathdiv.length-1];
					}
				}
			}
			if(date.length() == 0) {
				date = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
			}
			writer.putLine(PJ_NAME_KEY+pjname);
			writer.putLine(PJ_DATE_KEY+date);
			ArrayList<CodeTab> tabs = editor.getCodeTabs();
			for(int i=0; i<tabs.size(); i++) {
				writer.putLine(PJ_TABS_KEY+tabs.get(i).getPath());
			}
			String selTab = editor.getSelectedTabPath();
			if(selTab != null) {
				writer.putLine(PJ_SELTAB_KEY+selTab);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void reload() {
		saveWorkspace();
		editor.closeAllTabs();
		loadWorkspace();
	}
	
	public static void importResources() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Import files");
        List<File> list =
                fileChooser.showOpenMultipleDialog(Main.getStage());
        if (list != null) {
            for (File file : list) {
            	String folder;
                if(file.getAbsolutePath().endsWith(WPL_EXT)) {
                	folder = "wpl";
                }else if(file.getAbsolutePath().endsWith(".css")) {
                	folder = "styles";
                }else if(file.getAbsolutePath().endsWith(".js")) {
                	folder = "scripts";
                }else if(file.getAbsolutePath().toLowerCase().endsWith(".png") ||
                		file.getAbsolutePath().toLowerCase().endsWith(".jpg") ||
                		file.getAbsolutePath().toLowerCase().endsWith(".jpeg") ||
                		file.getAbsolutePath().toLowerCase().endsWith(".bmp") ||
                		file.getAbsolutePath().toLowerCase().endsWith(".gif") ||
                		file.getAbsolutePath().toLowerCase().endsWith(".svg") ||
                		file.getAbsolutePath().toLowerCase().endsWith(".wave") ||
                		file.getAbsolutePath().toLowerCase().endsWith(".pcm") ||
                		file.getAbsolutePath().toLowerCase().endsWith(".webm") ||
                		file.getAbsolutePath().toLowerCase().endsWith(".vorbis") ||
                		file.getAbsolutePath().toLowerCase().endsWith(".vp8") ||
                		file.getAbsolutePath().toLowerCase().endsWith(".theora") ||
                		file.getAbsolutePath().toLowerCase().endsWith(".ogg") ||
                		file.getAbsolutePath().toLowerCase().endsWith(".mp3") ||
                		file.getAbsolutePath().toLowerCase().endsWith(".mp4") ||
                		file.getAbsolutePath().toLowerCase().endsWith(".aac") ||
                		file.getAbsolutePath().toLowerCase().endsWith(".h.264")) {
                	folder = "media";
                }else {
                	folder = "other";
                }
                try {
					Files.copy(Paths.get(file.getAbsolutePath()), Paths.get(projectPath+folder+SEPARATOR+file.getName()),
							REPLACE_EXISTING);
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
        }
        hierarchy.reload();
	}
	
	
	public static void editorCopy() {
		editor.copy();
	}
	
	public static void editorSelectAll() {
		editor.selectAll();
	}
	
	public static void editorPaste() {
		editor.paste();
	}
	
	public static void editorCut() {
		editor.cut();
	}
	
	public static void editorUndo() {
		editor.undo();
	}
	
	public static void editorRedo() {
		editor.redo();
	}
	
	public static void editorReplace(String val, String nval) {
		editor.replaceAll(val, nval);
	}
	
	public static void editorFind(String val, int index) {
		editor.find(val, index);
	}
	
	public static void beforeExit() {
		if(!editor.isEverythingSaved()) {
			Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Exit action");
			alert.setHeaderText("Some files are not saved.");
			alert.setContentText("Do you want to save your progress before exit?");
			
			ButtonType btYes = new ButtonType("Save and exit");
			ButtonType btNo = new ButtonType("Exit without saving");
			ButtonType btCancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);

			alert.getButtonTypes().setAll(btYes, btNo, btCancel);
			alert.getButtonTypes().stream()
				.map(alert.getDialogPane()::lookupButton)
				.forEach(n -> ButtonBar.setButtonUniformSize(n, false));
	
			Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == btYes){
				saveAllTabs();
			} else if (result.get() == btCancel) {
				return;
			}
		}
		saveWorkspace();
		Platform.exit();
		System.exit(0);
	}
	
	
	public static void closeCurrentProject() {
		// TODO
	}
	
	
	/** Permite redireccionar la salida de consola a un fichero */
	public static void standardIO2File(String fileName){
        if(fileName.equals("")){//Si viene vacío usamos este por defecto
            fileName="/home/carlos/Escritorio/log.txt";
        }

        try {
            //Creamos un printstream sobre el archivo permitiendo añadir al
            //final para no sobreescribir.
            PrintStream ps = new PrintStream(new BufferedOutputStream(
                    new FileOutputStream(new File(fileName),true)),true);
            //Redirigimos entrada y salida estandar
            System.setOut(ps);
            System.setErr(ps);
        } catch (FileNotFoundException ex) {
            System.err.println("Se ha producido una excepción FileNotFoundException");
        }
    }
	
	
}