# Conversion de CandidatsDoublons en XLSX

## Objectif

Remplacer les futurs ajouts dans `CandidatsDoublons.txt` par un classeur cumulatif `CandidatsDoublons.xlsx`, directement exploitable dans Excel ou LibreOffice. Le fichier TXT historique est conservé sans migration.

## Périmètre

Cette évolution concerne uniquement `logskbart-api`. Elle est développée sur `feature/SOA-503-candidats-doublons-xlsx`, branche issue de la correction des rapports d'erreurs, afin de ne pas modifier la PR #65 pendant sa revue.

## Architecture

### Service dédié

Un service dédié prend en charge le traitement de `CandidatsDoublons` :

1. lire le rapport `_other.bad` associé au bouquet ;
2. ignorer l'en-tête et les messages non structurés ;
3. extraire les données du message ;
4. créer ou ouvrir `CandidatsDoublons.xlsx` ;
5. ajouter les nouvelles lignes à la fin de la feuille ;
6. enregistrer le classeur de manière sécurisée.

`LogsListener` conserve l'orchestration Kafka et appelle ce service après la création des rapports. Le service retourne si au moins une ligne a été ajoutée. L'email `CandidatsDoublons` n'est envoyé que dans ce cas ; l'email immédiat `_other.bad` reste envoyé pour toute erreur hors 400.

### Dépendance

Le projet utilise `org.apache.poi:poi-ooxml:5.2.5`, conformément au plan SOA-503.

## Format du classeur

- Fichier : `${abes.path-to-reports}/CandidatsDoublons.xlsx`.
- Feuille unique : `CandidatsDoublons`.
- Ligne 1 figée et filtre automatique actif.
- En-têtes en gras, texte blanc sur fond bleu foncé.
- Largeurs de colonnes bornées pour éviter les cellules illisibles ou excessivement larges.
- Toutes les valeurs, notamment PPN et ISSN, sont enregistrées comme texte.

| Colonne | En-tête | Contenu |
|---|---|---|
| A | PPN | PPN extrait du message |
| B | Commande WinIBW | `che ppn <PPN>` |
| C | Titre | `publication title` |
| D | Type de ressource | `publication_type` |
| E | ISSN imprimé | `print_identifier` |
| F | ISSN en ligne | `online_identifier` |
| G | Nom du bouquet | Nom du fichier sans `.tsv`, `_FORCE` ou `_BYPASS` |
| H | Nature de l'erreur | Texte précédant le premier groupe de PPN |

## Append et concurrence

La méthode d'ajout est synchronisée dans la JVM. Si le classeur existe, Apache POI l'ouvre avec `WorkbookFactory` et ajoute les lignes après la dernière ligne utilisée. S'il n'existe pas, le service crée la feuille et ses en-têtes.

Le classeur est écrit dans un fichier temporaire placé dans le même répertoire, puis déplacé avec remplacement vers le chemin final. Un déplacement atomique est tenté lorsque le système de fichiers le supporte. Une erreur de lecture ou d'écriture laisse le dernier XLSX valide inchangé.

## Gestion des données

- Une ligne sans tabulation ou sans marqueur `publication title : ` est ignorée.
- Un champ absent produit une cellule texte vide.
- Les lignes valides sont ajoutées dans l'ordre du rapport `_other.bad`.
- Aucun dédoublonnage n'est effectué : deux traitements produisant le même candidat créent deux lignes, comme le mode append historique.
- `CandidatsDoublons.txt` n'est ni lu, ni modifié, ni supprimé.

## Email

`EmailService.sendCandidatsDoublonsEmail` pointe vers `CandidatsDoublons.xlsx` et adapte son libellé. Le fichier reste à la racine du répertoire de rapports pour conserver l'URL actuelle, seul le suffixe change.

## Vérification

Les tests TDD ouvrent le classeur avec Apache POI et vérifient :

- le nom de la feuille, les huit en-têtes et leur ordre ;
- les huit cellules d'une ligne et leur type texte ;
- le gel de la première ligne et le filtre ;
- l'ajout à un classeur existant sans perte des lignes précédentes ;
- la suppression des suffixes `_FORCE` et `_BYPASS` du bouquet ;
- l'absence de création lorsqu'aucune ligne n'est exploitable ;
- la conservation du fichier TXT historique ;
- le lien `.xlsx` dans l'email.

Un classeur d'exemple généré par le code est également ouvert et rendu pour un contrôle visuel des en-têtes, largeurs et contenus.

## Hors périmètre

- Conversion du fichier TXT historique.
- Déduplication des candidats.
- Rapports XLSX de `kafka2sudoc`.
- Suppression ou archivage automatique du classeur.
