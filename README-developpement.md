# logskbart-api

Le langage utilisé est Java, avec le framework Spring.

API permettant de :
1. récupérer les logs des chargements kbart dans un topic Kafka
2. d'isoler les logs de niveau `error` et de générer un fichier de log mis à disposition de l'application cercles-bacon ([lien cercles-bacon](https://cerclesbacon.abes.fr/)) 
3. d'envoyer les logs (tout niveau confondu) en BDD
4. d'exposer un web service permettant de récupérer les logs associés à un fichier kbart

## Schéma de l'architecture du projet Convergence
![schéma de l'architecture du projet Convergence](https://raw.githubusercontent.com/abes-esr/kbart2kafka/fb60a20d84e7cf06722044559bdb18165e6e13e0/documentation/ArchitectureConvergence.svg "schéma de l'architecture du projet Convergence")

## Récupération des logs
*(class `LogsListener.java`)*

Cette classe comporte un `@KafkaListener`, `listenInfoKbart2KafkaAndErrorKbart2Kafka` qui lit les messages kafka à partir du topic Kafka `bacon.logs.toload`.
Chaque message kafka correspond à une entrée de log créée par les API kbart2kafka ([lien github](https://github.com/abes-esr/kbart2kafka)) et best-ppn-api ([lien github](https://github.com/abes-esr/best-ppn-api)).
>[!NOTE] 
> 
> La `key` de chaque message kafka comprend deux informations :
> - le nom du fichier kbart
> - Le numéro de la ligne du fichier kbart concernée par ce message de log

La présence d'un fichier de log temporaire portant le nom du fichier kbart en cours est contrôlée. Le cas échéant, les nouveaux messages de log sont ajoutés à la suite. Sinon, un nouveau fichier de log est créé et le premier message de log y est inscrit. 

## Isolation des logs de niveau error 
*(class `LogsListener.java`)*

Le `LogLevel` du message kafka est contrôlé. S'il est strictement de niveau `ERROR`, alors il sera inscrit dans le fichier de log temporaire. 
Avant cette inscription, la présence dudit fichier est contrôlée. S'il n'est pas présent, il est créé avant inscription du message kafka.

## Envoie des logs en BDD
*(class `LogsListener.java`)*

Quel que soit le `LogLevel` des messages kafka, ils seront in fine envoyer en base de données. 
La base de données choisie pour l'API est PostgreSQL ([site officiel](https://www.postgresql.org/)). l'API utilise le module JpaRepository pour l'accès à la BDD ([documentation officielle](https://docs.spring.io/spring-data/jpa/reference/jpa.html))

### Configuration de l'accès à la base de données

La configuration de l'accès à la BDD PostgreSQL s'effectue dans différents fichiers. 

Dans le fichier `application.properties` :
`spring.jpa.open-in-view=false`

Dans les fichiers `application-localhost.properties`, `application-dev.properties`, `application-test.properties` et `application-prod.properties`
```yaml
# Base Postgres
spring.datasource.logsdb.driver-class-name=org.postgresql.Driver
spring.datasource.logsdb.jdbcurl=
spring.datasource.logsdb.username=
spring.datasource.logsdb.password=

spring.jpa.logsdb.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.logsdb.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.logsdb.generate-ddl=true
spring.jpa.logsdb.hibernate.ddl-auto=update
spring.jpa.logsdb.show-sql=false
spring.sql.logsdb.init.mode=never
```

>[!NOTE]
> 
> Certains champs devront être complétés :
> `spring.datasource.logsdb.jdbcurl` avec l'url d'accès à votre base de données
> `spring.datasource.logsdb.username` avec un username permettant l'écriture dans votre BDD
> `spring.datasource.logsdb.password` avec le password associé au username
