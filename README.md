# Eco Ressource Backend

Eco Ressource Backend est l'API Spring Boot de la plateforme Eco Ressource, une marketplace B2B orientee economie circulaire. Le projet centralise la gestion des utilisateurs, entreprises, annonces, stocks, reservations, livraisons, dons, evenements solidaires et modules financiers.

L'objectif de la plateforme est de faciliter la reutilisation, l'echange, la donation et la valorisation de ressources entre acteurs professionnels et associatifs, tout en proposant des services complementaires comme la moderation, l'analyse assistee par IA, le suivi logistique, les paiements et la gestion de documents.

## Fonctionnalites principales

- Authentification et securisation des endpoints avec Spring Security et JWT.
- Gestion des utilisateurs, roles, entreprises, transporteurs et associations solidaires.
- Publication, recherche, moderation et gestion des annonces et ressources.
- Gestion des produits, stocks, mouvements d'inventaire et fichiers associes.
- Reservations, commandes de livraison, expeditions et suivi logistique.
- Dons, participations aux evenements et documents d'evenement.
- Modules finance : factures, transactions, financement, escrow, analytics et integrations de paiement.
- Services IA pour la generation de descriptions, l'analyse de defauts et l'aide a la recherche de produits.
- Notifications et integrations externes : email, Discord, Stripe, Konnect, services de geocodage et APIs IA.

## Technologies utilisees

- Java 17
- Spring Boot 3.2.5
- Spring Web, Spring Security, Spring Data JPA, Validation et WebSocket
- MySQL
- Maven
- JWT
- Springdoc OpenAPI / Swagger
- Stripe SDK
- Thymeleaf pour les templates email

## Structure du projet

- `src/main/java/com/marketplace/backend/controller` : endpoints REST de l'application.
- `src/main/java/com/marketplace/backend/entity` : entites metier et modeles persistants.
- `src/main/java/com/marketplace/backend/repository` : repositories Spring Data JPA.
- `src/main/java/com/marketplace/backend/service` : logique metier.
- `src/main/resources` : configuration, templates et scripts de migration.
- `database` : scripts SQL de base de donnees.
- `diagrams` : diagrammes de conception du projet.

## Lancement local

Prerequis :

- Java 17
- Maven
- MySQL

Etapes principales :

1. Creer une base MySQL nommee `eco_ressource_db`.
2. Configurer les variables sensibles dans un fichier `.env` local ou dans l'environnement systeme.
3. Adapter `src/main/resources/application.properties` si necessaire.
4. Lancer l'application :

```bash
mvn spring-boot:run
```

Par defaut, l'API demarre sur le port `9090`.

La documentation Swagger est disponible a l'adresse :

```text
http://localhost:9090/swagger-ui.html
```

## Diagrammes

### Diagramme de classes

![Diagramme DC final](diagrams/dc%20final.png)

### Diagramme de cas d'utilisation

![Diagramme UC final](diagrams/uc%20final.jpg)
