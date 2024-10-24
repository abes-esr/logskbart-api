#!/bin/bash

export SPRING_ELASTICSEARCH_URIS=${SPRING_ELASTICSEARCH_URIS:='http://localhost:9200'}

curl -X PUT "$SPRING_ELASTICSEARCH_URIS/logkbart" -H 'Content-Type: application/json' -d'
     {
     	"settings": {
     		"number_of_shards": 1,
     		"number_of_replicas": 0
     	},
     	"mappings": {
     		"properties": {
     			"ID": {
     				"type": "text"
     			},
     			"PACKAGE_NAME": {
     				"type": "text",
     				"fields": {
     				  "keyword": {
     				    "type": "keyword",
     				    "ignore_above": 256
     				  }
     				}
     			},
     			"TIMESTAMP": {
     				"type": "date"
     			},
     			"THREAD": {
     				"type": "text"
     			},
     			"LEVEL": {
     				"type": "text"
     			},
     			"LOGGER_NAME": {
     				"type": "text"
     			},
     			"MESSAGE": {
     				"type": "text"
     			},
     			"END_OF_BATCH": {
     				"type": "boolean"
     			},
     			"LOGGER_FQCN": {
     				"type": "text"
     			},
     			"THREAD_ID": {
     				"type": "integer"
     			},
     			"THREAD_PRIORITY": {
     				"type": "integer"
     			},
     			"NB_LINE": {
     				"type": "integer"
     			}
     		}
     	}
     }'

java -Xms4G -Xmx4G -XX:+UseG1GC -XX:ConcGCThreads=5 -XX:+ExitOnOutOfMemoryError -XX:MaxGCPauseMillis=100 -jar /app/logskbart-api.jar
