# logskbart-api

Vous êtes sur le README usager. Si vous souhaitez accéder au README développement, veuillez suivre ce lien : [README-developpement](README-developpement.md)

API permettant de :
1. récupérer les logs des chargements kbart dans un topic Kafka
2. d'isoler les logs de niveau `error` et de générer un fichier de log mis à disposition de l'application cercles-bacon ([lien cercles-bacon](https://cerclesbacon.abes.fr/))
3. d'envoyer les logs (tout niveau confondu) en BDD
4. d'exposer un web service permettant de récupérer les logs associés à un fichier kbart

## Récupération des logs, isolation des logs de niveau error, envoie des logs en BDD

La récupération, l'isolation et l'envoie des logs à partir d'un topic Kafka sont des processus automatiques.
Ils seront exécutés automatiquement et en parallèle du fonctionnement des API kbart2kafka ([lien github](https://github.com/abes-esr/kbart2kafka)) et best-ppn-api ([lien github](https://github.com/abes-esr/best-ppn-api)) 

## Web Service (ws) logs
Le Web Service (ws) logs permet de récupérer l'ensemble des logs associés au traitement d'un fichier kbart en fonction du nom de fichier et de la date de chargement.

### Exemple de requête :

`http://[url d'accès à l'API sur votre serveur]/v1/logs/[nom du fichier]/[date de chargement]`

### Exemple de résultat :

1. Dans un navigateur internet (recherche infructueuse) :
```xml
<LogDto>
<filename>CYBERLIBRIS_COUPERIN_SCIENCES-HUMAINES-ET-SOCIALES</filename>
<date>2000-01-04</date>
<ligneLogs/>
</LogDto>
```

2. Dans une application dédiée (recherche infructueuse) :
```json
{
    "filename": "CYBERLIBRIS_COUPERIN_SCIENCES-HUMAINES-ET-SOCIALES",
    "date": "2000-01-04",
    "ligneLogs": []
}
```
