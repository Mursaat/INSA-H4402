package agile.controlleur;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import agile.modele.Chemin;
import agile.modele.Entrepot;
import agile.modele.Intersection;
import agile.modele.Livraison;
import agile.modele.Temps;
import agile.modele.Tournee;
import agile.modele.Troncon;
import javafx.stage.FileChooser;

public class EtatTourneeCalculee extends EtatDefaut {

    private static String CHEMIN_REPERTOIRE_INITIAL = "src/main/resources/";

    public EtatTourneeCalculee() {
    }

    /**
     * Génère la feuille de route au format txt pour le livreur. Demande à
     * l'utilisateur le fichier à utiliser pour l'export
     * 
     * @param controlleur
     *            Controlleur
     */
    @Override
    public void enregistrerFeuilleDeRoute(Controlleur controlleur) throws Exception {

	FileChooser fileChooserTXT = new FileChooser();
	fileChooserTXT.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Fichier texte", "*.txt"));
	fileChooserTXT.setInitialDirectory(new File(CHEMIN_REPERTOIRE_INITIAL));
	File fichier = fileChooserTXT.showSaveDialog(null);

	if (fichier.equals(null)) {
	    String messageAnnulation = "Erreur: Aucun fichier pour l'export n'a été séléctionné.";
	    System.out.println(messageAnnulation);
	    throw new Exception(messageAnnulation);
	} else {
	    if (fichier != null) {
		try {
		    fichier.createNewFile();
		} catch (IOException e) {
		    String messageErreur = "Impossible de créer le fichier d'export " + fichier.toString();
		    System.err.println(messageErreur);
		    System.err.println(e.getMessage());
		    e.printStackTrace();
		    throw new Exception(messageErreur);
		}

	    }

	    Tournee tournee = controlleur.getTournee();

	    try {
		// Création et ouverture du fichier texte
		PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fichier)));

		// En-tête
		out.println("\t\t\tTOURNEE");
		SimpleDateFormat formatDate = new SimpleDateFormat("dd MMMM yyyy 'à' hh:mm:ss");
		String date = formatDate.format(new Date());
		out.println("Editée le: " + date);
		out.println();

		// Contenu

		Entrepot entrepot = tournee.getDemandeInitiale().getEntrepot();
		int etapeActuelle = 1;
		String derniereRue = null;
		Intersection derniereDestination = null;
		Intersection derniereOrigine = entrepot.getIntersection();

		out.println("Départ de l'entrepôt[" + entrepot.getIntersection().getId() + "] à "
			+ entrepot.getHeureDepart().toString() + ".");

		Iterator<Livraison> iterator = tournee.getLivraisonsTSP().iterator();

		for (Chemin chemin : tournee.getCheminsTSP()) {
		    // Description du chemin
		    for (Troncon troncon : chemin.getTroncons()) {
			// Calcul de l'angle :
			// troncon.getOrigine() = derniereDestination d'ou o2d1
			Intersection o1 = derniereOrigine;
			Intersection o2d1 = troncon.getOrigine();
			Intersection d2 = troncon.getDestination();
			int pscalaire = (o2d1.getX() - o1.getX()) * (d2.getX() - o2d1.getX())
				+ (o2d1.getY() - o1.getY()) * (d2.getY() - o2d1.getY());
			// Affichage
			out.print(etapeActuelle + ". ");
			if (troncon.getNomRue().equals(derniereRue)) {
			    out.print("Continuer sur ");
			} else if (pscalaire > 0) {

			    out.print("Tourner à gauche sur ");
			} else if (pscalaire <= 0) {
			    out.print("Tourner à droite sur ");
			}
			out.println(troncon.getNomRue() + ".");
			derniereRue = troncon.getNomRue();
			derniereDestination = troncon.getDestination();
			derniereOrigine = troncon.getOrigine();
			etapeActuelle++;
		    }

		    // Description de la livraison
		    if (iterator.hasNext()) {
			Livraison livraison = iterator.next();
			out.print("\t====> Effectuer la livraison à " + livraison.getIntersection().getId());
			if (livraison.ContrainteDeTemps() == true) {
			    out.print(" entre " + livraison.getDebutPlage().toString() + " et "
				    + livraison.getFinPlage().toString());
			}
			out.println(". [Durée de la livraison: " + livraison.getDuree() + " secondes]");
			derniereRue = null;
		    }
		}

		out.println("\t====> Arrivée à l'entrepôt[" + entrepot.getIntersection().getId() + "].");
		out.println();
		out.println("Fin de la tournée.");

		// Fermeture du fichier
		out.close();
		System.out.println("La tournée a été exportée dans " + fichier.toString() + ".");
	    } catch (Exception e) {
		System.err.println("Erreur lors de l'export de la tournée");
		System.err.println(e.getMessage());
		e.printStackTrace();
	    }

	    System.out.println("Enregistrement de la feuille de route");
	}
    }

    @Override

    public boolean ajouterLivraison(Controlleur controlleur, Livraison livraison, boolean historisation) {
	CommandeAjouterLivraison cAjouterLivraison = new CommandeAjouterLivraison(controlleur.getTournee(), livraison);
	cAjouterLivraison.doCde(controlleur);

	if (!(cAjouterLivraison.isSuccess() && historisation)) {
	    controlleur.getHistorique().undo(controlleur);
	} else {
	    controlleur.getHistorique().ajoute(cAjouterLivraison, controlleur);
	}

	return cAjouterLivraison.isSuccess();
    }

    @Override
    public void supprimerLivraison(Controlleur controlleur, Livraison livraison) {

	CommandeSupprimerLivraison cSupprimerLivraison = new CommandeSupprimerLivraison(controlleur.getTournee(),
		livraison);
	controlleur.getHistorique().ajoute(cSupprimerLivraison, controlleur);
    }

    @Override
    public void modifierLivraison(Controlleur controlleur, Livraison livraison, Temps debutPlage, Temps finPlage) {

	CommandeModifierLivraison cModifierLivraison = new CommandeModifierLivraison(controlleur.getTournee(),
		livraison, debutPlage, finPlage);
	controlleur.getHistorique().ajoute(cModifierLivraison, controlleur);
    }

    @Override

    public void undo(Controlleur controlleur) {
	controlleur.getHistorique().undo(controlleur);
    }

    @Override

    public void redo(Controlleur controlleur) {
	controlleur.getHistorique().redo(controlleur);
    }

}