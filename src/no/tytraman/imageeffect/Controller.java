package no.tytraman.imageeffect;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;

public class Controller {

    enum ImageMode {
        BLACK_AND_WHITE, GRAY_LEVEL
    }


    @FXML
    private MenuItem menuVersion;
    @FXML
    private ImageView leftImage;
    @FXML
    private ImageView rightImage;
    @FXML
    private Label toolLabel;
    @FXML
    private Menu filesMenu;
    @FXML
    private Menu transformMenu;

    private boolean isFolder;
    private File folder;
    private File fileImg;
    private static int filesNumber;

    public void initialize() {
        menuVersion.setText("Version: " + Main.VERSION);
    }

    public void openFile() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Ouvrir un fichier...");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tout", "*.*"),
                new FileChooser.ExtensionFilter("PNG", "*.png"),
                new FileChooser.ExtensionFilter("JPG", "*.jpg"),
                new FileChooser.ExtensionFilter("JPEG", "*.jpeg"),
                new FileChooser.ExtensionFilter("GIF", "*.gif")
        );
        fc.setInitialDirectory(new File(System.getProperty("user.home")));
        fileImg = fc.showOpenDialog(Main.STAGE);
        isFolder = false;
        if(fileImg != null) {
            Image image = new Image(fileImg.toURI().toString());
            if(image.getWidth() == 0 || image.getHeight() == 0) {
                toolLabel.setText("Fichier invalide !");
                transformMenu.setDisable(true);
                leftImage.setImage(null);
                rightImage.setImage(null);
            }else {
                toolLabel.setText(fileImg.getAbsolutePath());
                transformMenu.setDisable(false);
                leftImage.setImage(image);
                rightImage.setImage(null);
            }
        }else {
            toolLabel.setText("Aucun fichier sélectionné...");
            transformMenu.setDisable(true);
            leftImage.setImage(null);
            rightImage.setImage(null);
        }
    }

    public void openFolder() {
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Ouvrir un dossier...");
        dc.setInitialDirectory(new File(System.getProperty("user.home")));
        folder = dc.showDialog(Main.STAGE);
        isFolder = true;
        if(folder != null) {
            transformMenu.setDisable(false);
            toolLabel.setText(folder.getAbsolutePath());
            leftImage.setImage(null);
            rightImage.setImage(null);
            showFiles(null, folder);
            System.out.println("Dossier chargé");
            toolLabel.setText("Dossier chargé");
        }else {
            toolLabel.setText("Aucun dossier sélectionné...");
            transformMenu.setDisable(true);
            leftImage.setImage(null);
            rightImage.setImage(null);
        }
    }

    public void setBlackAndWhite() {
        if(isFolder) {
            doAction(ImageMode.BLACK_AND_WHITE);
        }else {
            filesMenu.setDisable(true);
            if(transformToBlackAndWhite(fileImg, "temp-black_and_white.png")) {
                System.out.println("Image transformée en noir et blanc");
                rightImage.setImage(new Image(new File("temp-black_and_white.png").toURI().toString()));
            }else {
                System.out.println("L'image n'a pas pu être transformée en noir et blanc");
                rightImage.setImage(null);
            }
            filesMenu.setDisable(false);
        }
    }

    public void setGray() {
        if(isFolder) {
            doAction(ImageMode.GRAY_LEVEL);
        }else {
            if(transformToGray(fileImg, "temp-gray.png")) {
                System.out.println("Image transformée en niveau de gris");
                rightImage.setImage(new Image(new File("temp-gray.png").toURI().toString()));
            }else {
                System.out.println("L'image n'a pas pu être transformée en niveau de gris");
                rightImage.setImage(null);
            }
        }
    }

    private void doAction(ImageMode imageMode) {
        long startTime = new Date().getTime();
        filesNumber = 0;
        DirectoryChooser dc = new DirectoryChooser();
        dc.setTitle("Choisir un dossier de destination...");
        dc.setInitialDirectory(new File(System.getProperty("user.home")));
        File outputFolder = dc.showDialog(Main.STAGE);
        if(outputFolder != null) {
            filesMenu.setDisable(true);
            transformMenu.setDisable(true);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                toolLabel.setText("Copie des fichiers...");
                            }
                        });
                        FileUtils.copyDirectory(folder, outputFolder);
                        showFiles(imageMode, outputFolder);
                        long finishTime = new Date().getTime();
                        System.out.println("Opération terminée, " + filesNumber + " fichiers traités en " + (finishTime - startTime) + " ms");
                        Platform.runLater(new Runnable() {
                            @Override
                            public void run() {
                                toolLabel.setText("Opération terminée, " + filesNumber + " fichiers traités en " + (finishTime - startTime) + " ms");
                                filesMenu.setDisable(false);
                                transformMenu.setDisable(false);
                            }
                        });
                    }catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    private void showFiles(ImageMode imageMode, File folder) {
        File[] files = folder.listFiles();
        for(File file : files) {
            if(file.isDirectory()) {
                if(imageMode == null) {
                    System.out.println("Dossier: " + file.getPath());
                }
                showFiles(imageMode, file);
            }else {
                if(imageMode == ImageMode.BLACK_AND_WHITE) {
                    transformToBlackAndWhite(file, file.getAbsolutePath());
                    filesNumber++;
                    updateFilesNumberLabel();
                }else if(imageMode == ImageMode.GRAY_LEVEL) {
                    transformToGray(file, file.getAbsolutePath());
                    filesNumber++;
                    updateFilesNumberLabel();
                }else {
                    System.out.println("Fichier: " + file.getPath());
                }
            }
        }
    }

    private boolean transformToBlackAndWhite(File imageFile, String outputDirectory) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if(image != null) {
                BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
                Graphics2D graphic = newImage.createGraphics();
                graphic.drawImage(image, null, null);
                graphic.dispose();
                File output = new File(outputDirectory);
                ImageIO.write(newImage, "png", output);
                return true;
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean transformToGray(File imageFile, String outputDirectory) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            if(image != null) {
                BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
                Graphics2D graphic = newImage.createGraphics();
                graphic.drawImage(image, null, null);
                graphic.dispose();
                File output = new File(outputDirectory);
                ImageIO.write(newImage, "png", output);
                return true;
            }
        }catch(IOException e) {
            e.printStackTrace();
        }
        return false;
    }




    private void updateFilesNumberLabel() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                toolLabel.setText(filesNumber + " fichiers traités");
            }
        });
    }

}
