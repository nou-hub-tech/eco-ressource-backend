# Prompt de demarrage - Module Gestion Annonces

Utilise `module-gestion-annonces-final.md` comme source de verite, ainsi que `AGENTS.md` et toutes les regles dans `.cursor/rules/`.

Je veux que tu commences le developpement du module `Gestion Annonces / Posts` en respectant strictement la specification finale.

Contraintes obligatoires :

- ne pas inventer de nouvelles entites hors de celles definies ;
- garder le module simple, logique et implementation-ready ;
- remplacer le chat par un fil de commentaires threades ;
- respecter toutes les contraintes du `group buying` ;
- garder la logique metier principale dans `Spring Boot` ;
- reserver `Python` aux parties IA ;
- utiliser `Perspective API` pour la moderation des commentaires quand cette partie est implemente ;
- ne pas ignorer les APIs externes et les fonctionnalites avancees definies dans le document final.

Entites obligatoires :

- `Product`
- `ResourceListing`
- `PostAttachment`
- `Comment`
- `GroupPurchase`
- `GroupParticipant`
- `Favorite`

Ce que je veux que tu fasses en premier :

1. lire `module-gestion-annonces-final.md`, `AGENTS.md` et les regles `.cursor/rules/` ;
2. proposer une structure backend Spring Boot du module ;
3. generer les `enums`, `entities`, `repositories`, `DTOs` et signatures `services/controllers` ;
4. implementer en priorite :
   - creation d'annonce ;
   - consultation/recherche d'annonces ;
   - commentaires ;
   - group buying ;
   - favoris ;
5. preparer les points d'integration pour :
   - `OpenRouteService`
   - `Perspective API`
   - `Leaflet + OpenStreetMap`
   - `Python IA` pour detection materiau, tags, auto-description et suggestion de prix ;
6. garder les fonctionnalites avancees compatibles avec la specification :
   - carte interactive ;
   - notifications temps reel ;
   - trending listings ;
   - duplication d'annonce ;
   - tags automatiques ;
   - dashboard annonce.

Ordre de travail recommande :

1. `enums`
2. `entities`
3. `repositories`
4. `DTOs`
5. `services`
6. `controllers`
7. `websocket`
8. `integrations`

Important :

- avant toute implementation, resume le plan de fichiers a creer ;
- si le projet existant contient deja une architecture backend, adapte-toi a cette architecture au lieu d'en imposer une nouvelle ;
- si certaines decisions dependent du projet existant, propose l'option la plus simple et coherente avec la specification ;
- fais d'abord une V1 propre et fonctionnelle, puis laisse les points d'extension pour les fonctionnalites avancees.
