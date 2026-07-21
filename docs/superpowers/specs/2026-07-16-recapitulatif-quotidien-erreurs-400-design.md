# Récapitulatif quotidien des erreurs 400

## Objectif

Envoyer un seul email quotidien récapitulant les rapports `_400.bad` qui n'ont pas encore fait l'objet d'un récapitulatif réussi. Les erreurs `_other.bad` restent notifiées immédiatement et ne figurent pas dans ce récapitulatif.

## Comportement fonctionnel

- Un traitement contenant uniquement des erreurs 400 crée son `_400.bad` sans envoyer d'email immédiat.
- Un traitement contenant d'autres erreurs crée son `_other.bad` et déclenche immédiatement un email pointant vers ce fichier.
- Un traitement mixte déclenche immédiatement l'email `_other.bad`; son `_400.bad` est inclus dans le récapitulatif quotidien.
- Le récapitulatif est exécuté à 23 h dans le fuseau `Europe/Paris`.
- Aucun email quotidien n'est envoyé lorsqu'aucun `_400.bad` n'est en attente.

## Architecture

### Sélection durable des rapports

Le système de fichiers est la source de vérité. Le scheduler scanne le sous-répertoire `bad/` configuré par `abes.path-to-reports` et sélectionne les fichiers réguliers correspondant à `*_400.bad`.

Un fichier d'état placé dans le répertoire des rapports conserve l'instant du dernier récapitulatif réussi. À chaque exécution, le scheduler capture un instant de coupure et sélectionne les fichiers modifiés après le précédent instant et au plus tard à cet instant. Cela permet :

- de survivre à un redémarrage du service ;
- de reprendre les rapports après un envoi échoué ou une exécution manquée ;
- de reporter à l'exécution suivante un fichier créé pendant l'envoi.

Lorsqu'aucun état n'existe encore, la borne de début correspond au début de la journée courante en `Europe/Paris`, afin de ne pas notifier tout l'historique au premier déploiement.

### Scheduler

`DailyRecapScheduler` orchestre le scan, l'envoi et la mise à jour de l'état. Il utilise :

- `abes.daily-recap.cron=0 0 23 * * *` ;
- `abes.daily-recap.zone=Europe/Paris`.

La planification Spring est activée sur l'application. L'état n'est avancé qu'après un envoi réussi. Lorsqu'il n'y a aucun rapport, il est avancé jusqu'à l'instant de coupure sans appeler le service d'email.

### Emails

`EmailService` expose deux intentions explicites :

- un email immédiat contenant un lien vers `bad/<fichier>_other.bad` ;
- un email quotidien contenant un lien par rapport `bad/<fichier>_400.bad`.

La méthode interne d'envoi indique si l'appel au service de mail a réussi, afin que le scheduler sache s'il peut enregistrer son nouvel état.

## Gestion des erreurs

- Une erreur de lecture du répertoire ou du fichier d'état empêche d'avancer l'état et est journalisée.
- Un échec du service de mail conserve la borne précédente pour permettre une reprise ultérieure.
- Les noms envoyés dans l'email proviennent uniquement des fichiers découverts dans le répertoire `bad/`.
- Les rapports sont triés par nom pour produire un email déterministe.

## Tests

Les tests sont écrits avant la production et couvrent :

- la sélection des `_400.bad` dans la fenêtre temporelle ;
- l'absence d'email lorsqu'aucun rapport n'est en attente ;
- un email unique contenant tous les liens ;
- la mise à jour de l'état après succès ;
- la conservation de l'état après échec ;
- la reprise avec un état existant après redémarrage ;
- l'email immédiat pour `_other.bad` ;
- l'absence d'email immédiat pour un traitement contenant uniquement des erreurs 400.

## Hors périmètre

- Aucun récapitulatif hebdomadaire.
- Aucun stockage en base de données.
- Aucune suppression automatique des fichiers `.bad`.
- Aucun changement du format des rapports dans cette étape.
