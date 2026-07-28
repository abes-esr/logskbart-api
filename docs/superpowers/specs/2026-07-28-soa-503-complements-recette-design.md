# SOA-503 — Dédoublonnage des candidats et affichage des rapports dans CerclesBacon

## Contexte

La recette fonctionnelle a validé la production des nouveaux rapports :

- `<fichier>_400.bad` pour les erreurs de validation classées 400 ;
- `<fichier>_other.bad` pour les autres erreurs ;
- `CandidatsDoublons.xlsx` pour les candidats de doublons structurés.

Deux compléments sont nécessaires :

1. empêcher l'ajout répété d'une même ligne dans le classeur cumulatif `CandidatsDoublons.xlsx` ;
2. rendre les rapports `_400.bad` et `_other.bad` visibles dans l'interface CerclesBacon historique déployée en test.

Les modifications concernent deux dépôts indépendants :

- `logskbart-api`, basé sur `develop` ;
- `Bacon`, basé sur `master`.

## Décisions fonctionnelles

### Classeur des candidats

Le classeur reste cumulatif. Les données précédentes ne sont pas supprimées.

Une ligne est considérée comme déjà présente lorsque les huit valeurs suivantes, après suppression des espaces en début et fin, sont identiques :

1. PPN ;
2. commande WinIBW ;
3. titre ;
4. type de ressource ;
5. ISSN imprimé ;
6. ISSN en ligne ;
7. nom du bouquet ;
8. nature de l'erreur.

La comparaison respecte la casse des valeurs métier. Elle n'effectue pas de rapprochement flou et ne fusionne pas deux candidats partiellement similaires.

Lorsqu'un traitement contient :

- uniquement des lignes déjà présentes : le classeur n'est pas réécrit et aucun courriel « Candidats doublons » n'est envoyé ;
- des lignes nouvelles et existantes : seules les nouvelles sont ajoutées et un courriel est envoyé ;
- uniquement des lignes nouvelles : toutes sont ajoutées et un courriel est envoyé.

L'ordre des nouvelles lignes reste celui du rapport `_other.bad`.

### Affichage CerclesBacon

Pour chaque TSV, l'interface peut afficher zéro, un ou deux rapports :

- lien « Erreurs 400 » si `<base>_400.bad` existe ;
- lien « Autres erreurs » si `<base>_other.bad` existe.

Si au moins un rapport existe, le statut de la ligne devient « Terminé avec erreurs ».

Les anciens rapports `<base>.bad` à la racine de `ToLoad` restent pris en charge pendant la transition.

## Conception — `logskbart-api`

### Service concerné

`CandidatsDoublonsService.append(String filename)` conserve sa responsabilité :

1. lire les candidats structurés du rapport `_other.bad` ;
2. ouvrir ou créer le classeur ;
3. ajouter les candidats ;
4. retourner `true` uniquement si le classeur contient au moins une nouvelle ligne issue du traitement.

### Index des lignes existantes

À l'ouverture du classeur :

- la ligne d'en-tête est ignorée ;
- chaque ligne de données est convertie en une clé immuable composée des huit cellules ;
- les cellules absentes sont assimilées à une chaîne vide ;
- chaque valeur est normalisée avec `trim()` ;
- les clés sont stockées dans un `Set`.

Pour chaque candidat lu dans `_other.bad` :

- sa clé est calculée avec la même règle ;
- l'ajout au `Set` détermine s'il est nouveau ;
- seuls les candidats nouveaux produisent une ligne Excel.

Cette approche élimine aussi les doublons présents plusieurs fois dans un même rapport.

### Écriture du classeur

L'écriture temporaire et le remplacement atomique existants sont conservés.

Si aucune ligne nouvelle n'est trouvée :

- aucun fichier temporaire n'est créé ;
- le classeur existant n'est pas réécrit ;
- la méthode retourne `false`.

Si des lignes sont ajoutées :

- le filtre automatique est recalculé ;
- le classeur est écrit dans le fichier temporaire ;
- le remplacement atomique est effectué ;
- la méthode retourne `true`.

### Notification

`LogsListener` conserve le contrat actuel :

- `append(...) == true` déclenche `sendCandidatsDoublonsEmail(...)` ;
- `append(...) == false` ne déclenche aucun courriel de candidats.

Aucune modification n'est apportée au courriel des erreurs hors 400.

## Conception — `Bacon`

### Cause du défaut

L'application déployée :

- recherche les fichiers `.bad` uniquement dans `/applis/bacon/toLoad/` ;
- comprend uniquement les noms `<base>.bad` ;
- ne parcourt pas `/applis/bacon/toLoad/bad/` ;
- ne sait pas associer les suffixes `_400` et `_other` au TSV d'origine.

Le traitement aboutit donc côté services et les courriels sont envoyés, mais la colonne « Erreurs » reste vide.

### Découverte des rapports

`ListFileJSONCommand` continue à lire les TSV et les anciens `.bad` comme aujourd'hui. Une seconde source fixe est ajoutée :

```text
/applis/bacon/toLoad/bad/*.bad
```

Seuls les noms respectant l'un des deux formats suivants sont interprétés :

```text
<base>_400.bad
<base>_other.bad
```

Le suffixe de catégorie est retiré avant d'appliquer le parseur existant du nom KBART. La partie `<base>` conserve notamment l'éventuel suffixe `_FORCE` ou `_BYPASS`.

Le rapport est ensuite associé à la même clé que le TSV :

```text
provider_zone_package_date[_FORCE|_BYPASS]
```

### Réponse JSON

Deux propriétés facultatives sont ajoutées à chaque ligne :

- `BAD_400` : nom du rapport 400 ;
- `BAD_OTHER` : nom du rapport hors 400.

La propriété historique `BAD` est conservée pour les anciens rapports.

L'absence d'un fichier ne produit pas une propriété vide.

### Rendu dans `welcome.jsp`

La colonne « Erreurs » est construite ainsi :

- rapport historique : lien « Voir » ;
- rapport 400 : lien « Erreurs 400 » ;
- rapport hors 400 : lien « Autres erreurs » ;
- plusieurs rapports : liens séparés visuellement.

Lorsque `BAD`, `BAD_400` ou `BAD_OTHER` existe, la colonne d'état affiche :

```text
Terminé avec erreurs
```

Le comportement « En cours... » demeure fondé sur l'existence d'un fichier `.log` vide lorsqu'aucun rapport n'est encore disponible.

### Téléchargement sécurisé

`ViewFileCommand` accepte toujours uniquement un nom de fichier validé, jamais un chemin fourni par l'utilisateur.

Le répertoire est choisi côté serveur :

- nom se terminant par `_400.bad` ou `_other.bad` : `/applis/bacon/toLoad/bad/` ;
- autre fichier `.bad` ou `.log` : `/applis/bacon/toLoad/`.

Le chemin final est normalisé et doit rester dans le répertoire autorisé correspondant. Toute tentative de sortie du répertoire est rejetée.

Les liens ne dépendent donc ni d'un nom d'hôte codé en dur ni d'un accès direct non authentifié à `kbart2kafka`.

## Compatibilité

- Les TSV sans rapport conservent leur affichage actuel.
- Les anciens fichiers `<base>.bad` restent affichables.
- Les fichiers `_FORCE` et `_BYPASS` sont associés à leur propre ligne.
- Un TSV peut présenter simultanément les liens 400 et hors 400.
- Le récapitulatif quotidien des erreurs 400 et les courriels immédiats hors 400 ne sont pas modifiés.
- Java 21 est conservé pour les services concernés.

## Tests automatisés

### `logskbart-api`

Les tests doivent couvrir :

1. création d'un nouveau classeur avec un candidat ;
2. ajout d'un candidat nouveau à un classeur existant ;
3. second traitement du même candidat sans nouvelle ligne ;
4. mélange d'un candidat existant et d'un candidat nouveau ;
5. même candidat répété dans un unique `_other.bad` ;
6. cellules vides dans une ligne existante ;
7. absence de courriel lorsque toutes les lignes sont déjà présentes ;
8. courriel unique lorsqu'au moins une ligne est nouvelle.

### `Bacon`

Le code d'interprétation des noms de rapports sera isolé dans une unité testable. Les tests doivent couvrir :

1. association de `<base>_400.bad` au TSV ;
2. association de `<base>_other.bad` au TSV ;
3. conservation de `_FORCE` ;
4. conservation de `_BYPASS` ;
5. présence simultanée des deux catégories ;
6. compatibilité avec `<base>.bad` ;
7. rejet d'un nom ne respectant pas le format ;
8. résolution d'un nouveau rapport dans le sous-répertoire `bad/` ;
9. impossibilité de sortir du répertoire autorisé.

Une vérification du rendu doit confirmer les libellés « Terminé avec erreurs », « Erreurs 400 » et « Autres erreurs ».

## Recette fonctionnelle

La recette réutilise les fichiers SOA-503 existants :

- un TSV produisant `_400.bad` ;
- un TSV produisant `_other.bad` et des candidats doublons.

Les résultats attendus sont :

1. la ligne du TSV affiche « Terminé avec erreurs » ;
2. le lien correspondant apparaît sans rechargement manuel au prochain rafraîchissement automatique ;
3. chaque lien télécharge le bon rapport ;
4. deux catégories produisent deux liens ;
5. rejouer le fichier de candidats ne crée aucune ligne supplémentaire dans le XLSX ;
6. aucun nouveau courriel « Candidats doublons » n'est envoyé lors de ce rejeu ;
7. un fichier contenant un candidat déjà connu et un nouveau candidat ajoute seulement le nouveau et déclenche un courriel.

## Déploiement

Les changements seront développés sur deux branches :

- `logskbart-api` : `feature/SOA-503-dedoublonnage-candidats` ;
- `Bacon` : `feature/SOA-503-affichage-rapports-cerclesbacon`.

Chaque branche fera l'objet de tests et d'un commit indépendant. Les merges suivront les branches de référence propres aux dépôts :

- vers `develop` pour `logskbart-api` ;
- vers `master` pour `Bacon`, après confirmation du processus de livraison de l'application historique.

Le déploiement de `Bacon` doit précéder ou accompagner la recette d'interface. Le changement `logskbart-api` peut être validé séparément avec le classeur XLSX.
