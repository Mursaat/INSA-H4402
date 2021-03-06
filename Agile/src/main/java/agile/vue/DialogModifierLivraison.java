package agile.vue;

import java.time.LocalTime;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXDatePicker;
import com.jfoenix.controls.JFXDialog;

import agile.modele.Livraison;
import agile.modele.Temps;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Boite de dialogue permettant de modifier une livraison
 */
public final class DialogModifierLivraison {
    private DialogModifierLivraison() {
    }

    /**
     * Méthode à appeler pour afficher la boite de dialogue de modfication d'une
     * livraison
     * 
     * @param controlleur
     *            Le controlleur permettant d'accéder à la liste de livraisons,
     *            et de mettre à jour l'affichage
     * @param stackPane
     *            Où la boite de dialogue doit prendre effet
     * @param livraison
     *            La livraison à modifier
     */
    public static void show(ContentController controlleur, StackPane stackPane, Livraison livraison) {
	// Liste des composants à afficher
	Label labelPlageDebut = new Label("Plage horaire début :");
	Label labelPlageFin = new Label("Plage horaire fin :");
	JFXDatePicker datePickerPlageDebut = new JFXDatePicker();
	JFXDatePicker datePickerPlageFin = new JFXDatePicker();
	JFXButton boutonValider = new JFXButton("Valider");
	JFXButton boutonAnnuler = new JFXButton("Annuler");
	Text title = new Text("Modifier une livraison");
	title.setFont(Font.font(null, FontWeight.BOLD, 16));

	// Option des DatePickers
	datePickerPlageDebut.setPromptText("Début de la plage");
	datePickerPlageFin.setPromptText("Fin de la plage");
	datePickerPlageDebut.setDefaultColor(Color.web("#3f51b5"));
	datePickerPlageFin.setDefaultColor(Color.web("#3f51b5"));
	datePickerPlageDebut.setShowTime(true);
	datePickerPlageFin.setShowTime(true);

	// Affectation des heures déjà existantes (s'il y en a)
	if (livraison.getDebutPlage() != null) {
	    datePickerPlageDebut
		    .setTime(LocalTime.of(livraison.getDebutPlage().getHeure(), livraison.getDebutPlage().getMinute()));
	}
	if (livraison.getFinPlage() != null) {
	    datePickerPlageFin
		    .setTime(LocalTime.of(livraison.getFinPlage().getHeure(), livraison.getFinPlage().getMinute()));
	}

	// Placement sur une grille
	GridPane grid = new GridPane();
	grid.add(labelPlageDebut, 1, 1);
	grid.add(labelPlageFin, 2, 1);
	grid.add(datePickerPlageDebut, 1, 2);
	grid.add(datePickerPlageFin, 2, 2);

	grid.setHgap(10);
	grid.setVgap(10);
	grid.setPadding(new Insets(5, 10, 10, 10));

	boutonValider.getStyleClass().add("red-A400");
	boutonAnnuler.getStyleClass().add("red-A400");

	HBox hbox = new HBox();
	hbox.setPadding(new Insets(0, 10, 10, 10));
	hbox.setSpacing(10);
	boutonValider.setMinWidth(100);
	boutonAnnuler.setMinWidth(100);
	hbox.getChildren().add(boutonValider);
	hbox.getChildren().add(boutonAnnuler);
	hbox.setAlignment(Pos.CENTER);

	VBox vbox = new VBox();
	vbox.setPadding(new Insets(5, 10, 0, 10));
	vbox.getChildren().add(title);
	vbox.getChildren().add(grid);
	vbox.getChildren().add(hbox);

	JFXDialog dialog = new JFXDialog(stackPane, vbox, JFXDialog.DialogTransition.CENTER);

	// Binding bouton annuler : quitte la fenêtre
	boutonAnnuler.setOnAction(new EventHandler<ActionEvent>() {
	    @Override
	    public void handle(ActionEvent event) {
		dialog.close();
	    }
	});

	// Binding bouton valider : vérifie les heures et les enregistre
	boutonValider.setOnAction(new EventHandler<ActionEvent>() {
	    @Override
	    public void handle(ActionEvent event) {
		// Champs non vides
		if (datePickerPlageDebut.getTime() == null) {
		    controlleur.afficherMessage("La plage de début est vide.");
		    return;
		}
		if (datePickerPlageFin.getTime() == null) {
		    controlleur.afficherMessage("La plage de fin est vide.");
		    return;
		}

		Temps tempsPlageDebut = new Temps(datePickerPlageDebut.getTime().getHour(),
			datePickerPlageDebut.getTime().getMinute(), 0);
		Temps tempsPlageFin = new Temps(datePickerPlageFin.getTime().getHour(),
			datePickerPlageFin.getTime().getMinute(), 0);

		// Le temps de fin avant le temps de début
		if (tempsPlageDebut.compareTo(tempsPlageFin) > 0) {
		    controlleur.afficherMessage("La plage de fin est inférieure à la plage de départ.");
		    return;
		}

		// Les plages horraires ne contiennent pas l'heure d'arrivée
		if (tempsPlageDebut.compareTo(new Temps(
			livraison.getHeureArrivee().getTotalSecondes() + (int) livraison.getTempsAttente())) > 0) {
		    controlleur.afficherMessage(
			    "La plage de début est supérieure à l'heure d'arrivée + le temps d'attente.");
		    return;
		}
		if (tempsPlageFin.compareTo(new Temps(
			livraison.getHeureArrivee().getTotalSecondes() + (int) livraison.getTempsAttente())) < 0) {
		    controlleur.afficherMessage(
			    "La plage de fin est inférieure à l'heure d'arrivée + le temps d'attente.");
		    return;
		}

		// Enregistrement des modifications
		ContentController.controlleur.modifierLivraison(livraison, tempsPlageDebut, tempsPlageFin);
		controlleur.miseAJourLivraison(ContentController.controlleur.getTournee().getLivraisonsTSP());

		// Si tout est correct, fermeture de la boite de dialogue
		dialog.close();
	    }
	});

	// Afficher la boite de dialogue
	dialog.show();
    }
}
