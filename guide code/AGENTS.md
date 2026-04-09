# AGENTS

## Module Scope

This workspace contains the finalized specification for the `Gestion Annonces / Posts` module in `module-gestion-annonces-final.md`.

When working on this module, treat that file as the source of truth.

The module must stay:

- simple;
- business-driven;
- implementation-ready;
- scalable without unnecessary complexity.

## Required Reading

Before coding anything related to this module, read:

1. `module-gestion-annonces-final.md`
2. `.cursor/rules/module-gestion-annonces.mdc`
3. the companion rules in `.cursor/rules/`

If code conflicts with the finalized spec, prefer the finalized spec unless the user explicitly changes requirements.

## Core Domain

Build around exactly these 7 entities:

- `Product`
- `ResourceListing`
- `PostAttachment`
- `Comment`
- `GroupPurchase`
- `GroupParticipant`
- `Favorite`

Do not introduce extra domain entities unless the user explicitly extends the scope.

## Functional Scope

This module supports:

- listing creation;
- listing exploration and search;
- attachments;
- threaded comments;
- group buying;
- favorites;
- realtime notifications;
- AI-assisted enrichment.

## Business Rules

### Listings

- `ResourceListing` is the core entity.
- Listing type is required at creation: `SURPLUS`, `DEMANDE`, `GROUP_BUYING`.
- Listing status must follow the finalized spec: `ACTIVE`, `CLOSED`, `EXPIRED`, `CANCELLED`.
- A listing must be linked to an existing `Product`.

### Comments

- Chat is not allowed. Use threaded comments only.
- Replies must use `Comment.parent_id`.
- Only authenticated users can comment.
- Comment moderation must be supported.
- Listing owner and admin can remove inappropriate comments.

### Group Buying

- `GroupPurchase` exists only for `GROUP_BUYING` listings.
- Group status must follow the spec: `OPEN`, `FULL`, `SUCCESS`, `FAILED`, `CLOSED`.
- Seller cannot join their own group.
- One participation per company per group.
- Joined quantity must be positive and cannot exceed remaining quantity.
- Group cannot accept participants after close, expiry, or completion.
- Quantity updates must be concurrency-safe.

### Favorites

- One favorite per `user_id` and `listing_id`.

## Implementation Boundaries

### Keep In Spring Boot

- listings;
- comments;
- group buying;
- favorites;
- status transitions;
- security and permissions;
- request validation;
- filtering and classic search;
- realtime notifications with WebSocket/STOMP;
- persistence and transactions.

### Keep In Python

- material detection from image;
- automatic tags;
- automatic short description;
- price suggestion;
- optional later improvements for advanced recommendations.

Do not move core business rules into Python.

## AI Scope

AI is for enrichment, not for replacing the module logic.

Use AI for:

- material detection;
- tag generation;
- auto-description;
- price suggestion.

Image source priority:

1. first `PostAttachment` image;
2. fallback to `Product.image`;
3. if no image exists, skip visual analysis.

## Matching Rules

For V1, matching should be rules/scoring based, not mandatory ML.

Use criteria such as:

- product category;
- geographic distance;
- price attractiveness;
- quantity compatibility;
- group progress;
- simple interaction history.

Advanced ML-style matching is optional later.

## External APIs

Use these external integrations when implementing the finalized spec.

### Recommended

- `OpenRouteService`: geocoding and location search
- `Leaflet + OpenStreetMap`: interactive map
- `Perspective API`: abusive/inappropriate comment moderation

### Optional AI Helpers

- `Hugging Face Inference API`: image classification or label assistance
- `Gemini API` or `OpenRouter`: text generation and description help

### Integration Guidance

- add timeouts;
- add graceful fallback behavior;
- log failures clearly;
- do not block the main business flow unnecessarily if an optional AI helper fails.

## Advanced Features To Support

Do not forget these advanced module features from the finalized spec:

- interactive map for nearby listings;
- realtime notifications for comments, participation, and group status;
- trending listings based on interactions;
- listing duplication;
- automatic tags;
- listing dashboard with views, interactions, and popularity;
- intelligent suggestions during listing creation;
- abusive comment moderation.

Implement them progressively if the user asks for phased delivery, but keep them consistent with the spec.

## API Surface

Keep the REST API aligned with the finalized spec.

### Listings

- `POST /api/listings`
- `GET /api/listings`
- `GET /api/listings/{id}`
- `GET /api/listings/search`

### Comments

- `POST /api/listings/{id}/comments`
- `GET /api/listings/{id}/comments`

### Groups

- `POST /api/groups/{id}/join`
- `GET /api/groups/{id}`
- `DELETE /api/groups/{id}/leave`
- `GET /api/groups/{id}/participants`

### Favorites

- `POST /api/listings/{id}/favorite`
- `DELETE /api/listings/{id}/favorite`

## Validation Requirements

Always validate again on the backend, even if the frontend validates.

Must validate:

- required fields;
- referenced entity existence;
- permissions and ownership;
- listing/group state;
- parent-child comment consistency;
- anti-spam and abusive comment checks;
- file extension, size, count, and MIME type when possible;
- quantity limits and uniqueness constraints.

## Code Organization

Preferred module structure:

```text
listing/
  controller/
  service/
  repository/
  entity/
  dto/
  mapper/
  websocket/
```

Use explicit names:

- `*Controller`
- `*Service`
- `*Repository`
- `*Mapper`
- request/response DTOs with clear names

Use enums for listing type, listing status, and group status.

## Delivery Priorities

Recommended order if implementing from scratch:

1. entities, repositories, enums, and DTOs;
2. listing creation and search;
3. comments;
4. group buying;
5. favorites;
6. realtime notifications;
7. Python AI enrichment;
8. external API integrations;
9. advanced features and polishing.

## Agent Behavior

When coding this module:

- do not invent extra scope;
- do not reintroduce chat;
- do not bypass group buying constraints;
- do not push AI into the core business layer;
- do not ignore external API usage defined in the spec;
- keep changes aligned with `module-gestion-annonces-final.md`.
