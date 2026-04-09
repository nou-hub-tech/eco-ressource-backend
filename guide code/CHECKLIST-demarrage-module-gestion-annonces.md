# Checklist de demarrage - Module Gestion Annonces

## Fichiers de reference deja presents

- `module-gestion-annonces-final.md`
- `AGENTS.md`
- `.cursor/rules/module-gestion-annonces.mdc`
- `.cursor/rules/module-gestion-annonces-business.mdc`
- `.cursor/rules/module-gestion-annonces-comments-realtime.mdc`
- `.cursor/rules/module-gestion-annonces-ai-integrations.mdc`
- `.cursor/rules/module-gestion-annonces-api-validation.mdc`
- `.cursor/rules/module-gestion-annonces-code-style.mdc`

## Fichiers ou couches a creer en premier

### Backend Spring Boot

- `entity/`
- `repository/`
- `dto/`
- `service/`
- `controller/`
- `mapper/`
- `websocket/`
- `integration/`

### Elements minimums a implementer

- enums :
  - `ListingType`
  - `ListingStatus`
  - `GroupPurchaseStatus`

- entites :
  - `ResourceListing`
  - `PostAttachment`
  - `Comment`
  - `GroupPurchase`
  - `GroupParticipant`
  - `Favorite`

- repositories :
  - `ResourceListingRepository`
  - `CommentRepository`
  - `GroupPurchaseRepository`
  - `GroupParticipantRepository`
  - `FavoriteRepository`

- DTOs :
  - `CreateListingRequest`
  - `ListingResponse`
  - `ListingSearchRequest` ou params de recherche
  - `CreateCommentRequest`
  - `CommentResponse`
  - `JoinGroupRequest`
  - `GroupPurchaseResponse`
  - `FavoriteResponse`

- services :
  - `ListingService`
  - `CommentService`
  - `GroupPurchaseService`
  - `FavoriteService`
  - `MatchingService`
  - `NotificationService`

- controllers :
  - `ListingController`
  - `CommentController`
  - `GroupPurchaseController`
  - `FavoriteController`

## Integrations a prevoir

- `OpenRouteService` pour geolocalisation
- `Perspective API` pour moderation des commentaires
- `Leaflet + OpenStreetMap` pour la carte cote frontend plus tard
- `Python IA` pour :
  - detection materiau
  - tags automatiques
  - auto-description
  - suggestion de prix

## Fonctionnalites avancees a garder compatibles

- carte interactive
- notifications temps reel
- trending listings
- duplication d'annonce
- tags automatiques
- dashboard annonce
- moderation des commentaires

## Priorite de developpement

1. modeles et enums
2. persistence
3. APIs CRUD et recherche
4. commentaires
5. group buying
6. favoris
7. realtime
8. integrations externes
9. IA Python
10. fonctionnalites avancees
