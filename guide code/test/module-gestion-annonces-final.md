# Module Gestion Annonces / Posts

## 1. Objectif du module

Ce module permet aux entreprises de :

- publier des offres de surplus ;
- publier des demandes ;
- interagir autour d'une annonce via un fil de commentaires ;
- collaborer via un mecanisme de group buying ;
- decouvrir des annonces pertinentes grace a un matching intelligent ;
- suivre les evolutions importantes en temps reel.

Le module doit rester :

- simple a comprendre ;
- logique metier ;
- facile a implementer ;
- evolutif ;
- sans complexite inutile.

## 2. Perimetre fonctionnel final

Le module couvre les fonctionnalites suivantes :

- creation d'une annonce ;
- consultation et recherche d'annonces ;
- gestion des pieces jointes ;
- fil de commentaires par annonce ;
- gestion du group buying ;
- favoris ;
- notifications temps reel ;
- recommandations et assistance par IA.

## 3. Entites finales

Le module repose sur 7 entites principales.

### 3.1 Product

Produit existant obligatoire avant toute publication.

```text
Product {
  id
  name
  category
  description
  company_id
}
```

### 3.2 ResourceListing

Entite centrale du module.

```text
ResourceListing {
  id
  title
  description
  type (SURPLUS, DEMANDE, GROUP_BUYING)
  status (ACTIVE, CLOSED, EXPIRED, CANCELLED)

  quantity
  unit
  price

  location
  latitude
  longitude

  product_id
  company_id

  created_at
}
```

### 3.3 PostAttachment

```text
PostAttachment {
  id
  file_url
  listing_id
}
```

### 3.4 Comment

Le chat est remplace par un fil de commentaires simple et threade.

```text
Comment {
  id
  content
  user_id
  listing_id
  parent_id
  created_at
}
```

### 3.5 GroupPurchase

```text
GroupPurchase {
  id
  listing_id
  target_quantity
  current_quantity
  deadline
  status (OPEN, FULL, CLOSED, FAILED, SUCCESS)
}
```

### 3.6 GroupParticipant

```text
GroupParticipant {
  id
  group_id
  company_id
  quantity
}
```

### 3.7 Favorite

```text
Favorite {
  id
  user_id
  listing_id
}
```

## 4. Regles de conception importantes

### 4.1 Regles generales

- une annonce doit toujours etre liee a un `Product` existant ;
- une annonce appartient a une seule entreprise ;
- le type de l'annonce est obligatoire des la creation ;
- le statut est gere par le systeme selon l'etat metier ;
- le fil de commentaires remplace le chat direct afin de simplifier l'architecture ;
- le temps reel est reserve aux evenements importants, pas a toute l'application.

### 4.2 Types d'annonce

- `SURPLUS` : une entreprise vend un surplus ;
- `DEMANDE` : une entreprise exprime un besoin ;
- `GROUP_BUYING` : une entreprise publie une annonce collaborative qui permet a plusieurs acheteurs de rejoindre un groupe.

### 4.3 Pourquoi choisir le type des la creation

Le type doit etre choisi au depart car il determine :

- les champs a afficher dans le formulaire ;
- les validations metier ;
- l'existence ou non d'un `GroupPurchase` ;
- les actions disponibles cote frontend ;
- les notifications a envoyer ;
- la logique de participation.

## 5. Contraintes metier globales

### 5.1 Contraintes sur Product

- le produit doit exister ;
- le produit doit etre visible ou autorise pour l'entreprise ;
- la categorie doit etre coherente avec l'annonce.

### 5.2 Contraintes sur ResourceListing

- `title` obligatoire ;
- `description` obligatoire ;
- `type` obligatoire ;
- `quantity` obligatoire et strictement positive ;
- `unit` obligatoire ;
- `product_id` obligatoire ;
- `company_id` obligatoire ;
- `location` obligatoire si la recherche geographique est active ;
- `latitude` et `longitude` doivent etre valides si renseignes ;
- `price` doit etre positif ou nul selon le cas d'usage ;
- une annonce fermee ou expiree ne peut plus etre rejointe.

### 5.3 Contraintes sur Comment

- `content` obligatoire ;
- longueur minimum et maximum a definir ;
- `parent_id` optionnel ;
- si `parent_id` est renseigne, le commentaire parent doit appartenir au meme `listing_id` ;
- un commentaire supprime ne doit pas casser l'arbre logique si les reponses existent ;
- l'anti-spam doit limiter les publications trop frequentes ;
- le contenu peut etre analyse avant publication pour detecter les propos abusifs, toxiques ou inappropries ;
- un commentaire peut etre refuse, masque ou mis en moderation selon le score retourne par l'outil de moderation ;
- le proprietaire de l'annonce et l'administrateur peuvent supprimer un commentaire inapproprie.

### 5.4 Contraintes sur GroupPurchase

- un `GroupPurchase` existe uniquement si `ResourceListing.type = GROUP_BUYING` ;
- `target_quantity` obligatoire et strictement positive ;
- `current_quantity` initialisee a `0` ;
- `deadline` obligatoire ;
- `deadline` doit etre dans le futur au moment de la creation ;
- le statut initial est `OPEN` ;
- le statut devient `FULL` ou `SUCCESS` lorsque la quantite cible est atteinte ;
- le statut devient `FAILED` ou `CLOSED` si la date limite est depassee sans atteindre le seuil ;
- le groupe ne doit plus accepter de participants si le statut n'est plus `OPEN`.

### 5.5 Contraintes sur GroupParticipant

- une entreprise ne peut participer qu'une seule fois a un meme groupe ;
- le vendeur createur de l'annonce ne peut pas rejoindre son propre groupe ;
- la quantite du participant doit etre strictement positive ;
- la somme `current_quantity + nouvelle_quantite` ne doit pas depasser `target_quantity` ;
- la participation est interdite si le groupe est expire, ferme ou complet ;
- le groupe doit etre lie a une annonce active.

### 5.6 Contraintes sur Favorite

- un utilisateur ne peut ajouter qu'un seul favori par annonce ;
- il doit etre authentifie ;
- l'annonce doit exister.

## 6. Controles de saisie a prevoir

### 6.1 Controles communs frontend

- champs obligatoires clairement identifies ;
- messages d'erreur simples et precis ;
- prevention des valeurs negatives ;
- prevention des champs vides ou uniquement espaces ;
- verification du format des nombres ;
- verification du format des coordonnees ;
- limitation du nombre et de la taille des fichiers ;
- desactivation du bouton de soumission si le formulaire est invalide ;
- affichage d'un recapitulatif avant publication.

### 6.2 Controles backend obligatoires

Tous les controles frontend doivent etre verifies a nouveau cote backend.

Il faut valider :

- presence des champs obligatoires ;
- existence des references (`product_id`, `listing_id`, `group_id`) ;
- droits d'acces de l'utilisateur ;
- coherences metier ;
- etat courant de l'annonce ou du groupe ;
- prevention des doublons fonctionnels ;
- protection contre les requetes malveillantes.

### 6.3 Controles sur les fichiers

- extensions autorisees : `jpg`, `jpeg`, `png`, `webp`, `pdf` selon besoin ;
- taille maximale par fichier ;
- nombre maximum de fichiers par annonce ;
- rejet des fichiers vides ;
- nommage securise lors du stockage ;
- verification MIME type si possible.

### 6.4 Controles sur les commentaires

- contenu non vide ;
- longueur maximale ;
- blocage des messages repetes sur courte periode ;
- filtrage de contenu abusif si necessaire ;
- moderation automatique possible via `Perspective API` ;
- blocage ou mise en attente si le score de toxicite depasse un seuil defini ;
- interdiction de commenter sur une annonce inexistante.

## 7. Scenario principal 1 : Creation d'une annonce

### 7.1 Objectif

Permettre a une entreprise de publier une annonce exploitable par les autres entreprises.

### 7.2 Acteur principal

- entreprise vendeuse ou demandeuse.

### 7.3 Preconditions

- utilisateur authentifie ;
- entreprise connue du systeme ;
- produit existant dans le catalogue ;
- droits de publication disponibles.

### 7.4 Deroulement principal

1. L'utilisateur ouvre le formulaire de creation.
2. Il selectionne un `Product` existant.
3. Il choisit le type d'annonce : `SURPLUS`, `DEMANDE` ou `GROUP_BUYING`.
4. Il saisit le titre et la description.
5. Il renseigne la quantite, l'unite, le prix et la localisation.
6. Il ajoute des pieces jointes si besoin.
7. Si le type est `GROUP_BUYING`, il renseigne `target_quantity` et `deadline`.
8. Le systeme verifie la validite des donnees.
9. Le systeme cree le `ResourceListing`.
10. Si besoin, le systeme cree le `GroupPurchase` associe.
11. L'annonce est publiee avec le statut `ACTIVE`.
12. Une notification temps reel peut etre envoyee aux utilisateurs interesses.

### 7.5 Variantes

- si le type est `SURPLUS`, aucune creation de `GroupPurchase` n'est necessaire ;
- si le type est `DEMANDE`, l'annonce exprime un besoin et reste consultable comme demande ;
- si le type est `GROUP_BUYING`, le groupe est initialise automatiquement avec `current_quantity = 0`.

### 7.6 Erreurs possibles

- produit inexistant ;
- type non choisi ;
- quantite invalide ;
- prix invalide ;
- deadline invalide ;
- fichiers refuses ;
- utilisateur non autorise.

### 7.7 Resultat final

- l'annonce est enregistree et visible dans la marketplace ;
- le groupe est pret a recevoir des participants si l'annonce est de type `GROUP_BUYING`.

## 8. Scenario principal 2 : Consultation et recherche des annonces

### 8.1 Objectif

Permettre aux entreprises d'explorer la marketplace et de trouver les annonces pertinentes.

### 8.2 Acteurs

- acheteur ;
- entreprise interessee ;
- participant potentiel.

### 8.3 Deroulement principal

1. L'utilisateur consulte la liste des annonces.
2. Il applique des filtres : categorie, prix, localisation, type, popularite.
3. Le systeme retourne les annonces actives correspondant aux criteres.
4. L'utilisateur ouvre le detail d'une annonce.
5. Le systeme affiche :
   - les informations principales ;
   - les pieces jointes ;
   - les commentaires ;
   - l'etat du groupe si c'est un `GROUP_BUYING`.

### 8.4 Contraintes

- seules les annonces visibles et autorisees doivent etre affichees ;
- les annonces fermees ou expirees doivent l'indiquer clairement ;
- les annonces `GROUP_BUYING` doivent afficher la progression du groupe ;
- le bouton d'action doit s'adapter au type d'annonce.

## 9. Scenario principal 3 : Commentaires sur une annonce

### 9.1 Objectif

Permettre une discussion simple autour d'une annonce sans mettre en place un chat complet.

### 9.2 Acteurs

- vendeur ;
- acheteur ;
- participant au groupe.

### 9.3 Deroulement principal

1. L'utilisateur ouvre le detail de l'annonce.
2. Il saisit un commentaire.
3. Le systeme valide le contenu.
4. Le systeme peut envoyer le texte a un service de moderation automatique avant publication.
5. Si le commentaire est autorise, il est enregistre.
6. Le commentaire est diffuse en temps reel aux utilisateurs connectes a l'annonce et une notification s'affiche si un utilisateur publie ou repond a un commentaire.
7. Le proprietaire de l'annonce et l'administrateur peuvent supprimer un commentaire inapproprie.
6. Les autres utilisateurs peuvent repondre via `parent_id`.

### 9.4 Contraintes

- seul un utilisateur authentifie peut commenter ;
- sur un groupe ferme, on peut autoriser uniquement les participants et le vendeur a commenter ;
- un commentaire doit appartenir a une seule annonce ;
- une reponse doit viser un commentaire du meme fil ;
- les notifications doivent etre envoyees en cas de nouveau commentaire ;
- un commentaire juge toxique, insultant ou inapproprie peut etre bloque avant publication ;
- un seuil de moderation doit etre defini pour decider entre publication, avertissement ou refus.

### 9.5 Pourquoi le fil de commentaires est preferable au chat

- architecture plus simple ;
- historique plus clair ;
- implementation plus rapide ;
- moderation plus facile ;
- meilleur alignement avec une annonce publique.

## 10. Scenario principal 4 : Group Buying de A a Z

### 10.1 Objectif

Permettre a plusieurs entreprises de se regrouper pour acheter ensemble sur une meme annonce.

### 10.2 Idee metier

Le vendeur publie une annonce de type `GROUP_BUYING`.

Les entreprises interessees rejoignent le groupe avec une quantite souhaitee.

Le groupe reussit si la somme des participations atteint la quantite cible avant la deadline.

### 10.3 Acteurs

- vendeur createur du groupe ;
- entreprises participantes ;
- systeme de notification ;
- moteur de matching intelligent.

### 10.4 Etape A : Creation du group buying par le vendeur

1. Le vendeur cree une annonce.
2. Il choisit le type `GROUP_BUYING`.
3. Il saisit les informations de base du listing.
4. Il renseigne la quantite cible `target_quantity`.
5. Il fixe la `deadline`.
6. Le systeme cree le `ResourceListing`.
7. Le systeme cree le `GroupPurchase` associe.
8. Le groupe est ouvert avec le statut `OPEN`.

### 10.5 Etape B : Decouverte du groupe par les acheteurs

1. Les acheteurs consultent la marketplace.
2. Le systeme peut recommander le groupe selon :
   - la categorie ;
   - la distance ;
   - le prix ;
   - l'historique d'interet ;
   - la proximite avec le seuil.
3. L'utilisateur ouvre le detail du groupe.
4. Il voit :
   - la quantite cible ;
   - la quantite actuelle ;
   - la quantite restante ;
   - la date limite ;
   - les commentaires ;
   - les pieces jointes.

### 10.6 Etape C : Participation a un groupe

1. L'entreprise clique sur `Rejoindre le groupe`.
2. Elle saisit la quantite souhaitee.
3. Le systeme verifie :
   - que le groupe existe ;
   - que le groupe est `OPEN` ;
   - que l'annonce est `ACTIVE` ;
   - que la deadline n'est pas depassee ;
   - que l'entreprise n'est pas le vendeur ;
   - qu'elle n'a pas deja participe ;
   - que la quantite est valide ;
   - que la quantite restante est suffisante.
4. Si tout est valide, le systeme cree `GroupParticipant`.
5. Le systeme met a jour `current_quantity`.
6. Le systeme diffuse l'evolution en temps reel.
7. Si `current_quantity >= target_quantity`, le groupe change d'etat.

### 10.7 Etape D : Suivi en temps reel

Le frontend affiche :

- la barre de progression ;
- la quantite atteinte ;
- la quantite restante ;
- les nouveaux commentaires ;
- les nouvelles participations ;
- le statut du groupe.

### 10.8 Etape E : Reussite du groupe

Le groupe reussit si :

- `current_quantity >= target_quantity` ;
- la deadline n'est pas depassee.

Actions systeme possibles :

- passage du statut a `FULL` puis `SUCCESS` ;
- blocage de nouvelles participations ;
- notification a tous les participants ;
- transmission au module commande ou transaction.

### 10.9 Etape F : Echec du groupe

Le groupe echoue si :

- la deadline est depassee ;
- et `current_quantity < target_quantity`.

Actions systeme possibles :

- passage du statut a `FAILED` ou `CLOSED` ;
- blocage des nouvelles participations ;
- notification aux participants ;
- suggestion d'autres groupes similaires.

### 10.10 Etape G : Sortie ou annulation d'un participant

Si la regle metier autorise la sortie avant deadline :

1. le participant demande a quitter ;
2. le systeme supprime ou desactive sa participation ;
3. `current_quantity` est recalculee ;
4. le statut du groupe est re-evalue ;
5. une notification est diffusee.

Si la sortie n'est pas autorisee apres un certain point, la regle doit etre explicite.

### 10.11 Contraintes metier du group buying

- le vendeur ne rejoint jamais son propre groupe ;
- un participant ne rejoint qu'une seule fois le meme groupe ;
- la somme des quantites ne depasse jamais la cible ;
- un groupe n'accepte plus de participants apres la deadline ;
- un groupe complet est verrouille ;
- le groupe est lie a une seule annonce ;
- tous les participants rejoignent le meme produit et le meme prix de reference ;
- la coherence doit etre garantie en cas d'acces concurrents.

### 10.12 Pourquoi le vendeur ne doit pas rejoindre son propre groupe

- ce n'est pas coherent metier ;
- il est l'organisateur et non l'acheteur ;
- cela evite la triche sur la progression ;
- cela empeche de simuler un groupe complet artificiellement ;
- cela garde une separation claire entre offreur et acheteurs.

## 11. Scenario principal 5 : Favoris et engagement

### 11.1 Objectif

Permettre a un utilisateur de sauvegarder les annonces qui l'interessent.

### 11.2 Deroulement

1. L'utilisateur ouvre une annonce.
2. Il clique sur `Ajouter aux favoris`.
3. Le systeme verifie qu'il est authentifie.
4. Le systeme verifie que le favori n'existe pas deja.
5. Le favori est enregistre.

### 11.3 Contraintes

- un seul favori par utilisateur et par annonce ;
- l'annonce doit exister ;
- la suppression d'une annonce doit gerer ses favoris.

## 12. Scenario principal 6 : Notifications et temps reel

### 12.1 Evenements temps reel prioritaires

- nouvelle annonce pertinente ;
- nouveau commentaire ;
- nouvelle participation a un groupe ;
- groupe presque complet ;
- groupe complet ;
- groupe expire.

### 12.2 Choix technique

Implementation recommandee :

- Spring Boot WebSocket ;
- STOMP ;
- topics dedies par annonce ou par groupe.

### 12.3 Exemple d'usage

```java
@MessageMapping("/comments")
@SendTo("/topic/listings")
public CommentMessage publish(CommentMessage message) {
    return message;
}
```

Et pour le group buying :

```java
messagingTemplate.convertAndSend("/topic/group/" + groupId, payload);
```

### 12.4 Contraintes

- envoyer uniquement les evenements utiles ;
- ne pas surcharger le serveur avec du temps reel inutile ;
- securiser les canaux si certaines donnees sont reservees aux participants.

## 13. Scenario principal 7 : Matching intelligent et IA

### 13.1 Objectif

Ameliorer la decouverte et l'efficacite du module sans le complexifier.

L'IA dans ce module ne sert pas a remplacer la logique metier principale.

Elle sert surtout a enrichir automatiquement une annonce et a aider l'utilisateur a publier plus vite avec de meilleures informations.

### 13.2 Fonctionnalites IA proposees

- reconnaissance d'image ;
- detection du materiau ;
- generation automatique de description ;
- suggestion de prix ;
- suggestion de publication ;
- matching intelligent des annonces et des groupes ;
- tags automatiques.

### 13.3 Objectif concret de l'IA dans l'annonce

Meme si le produit possede deja une image dans `Product`, l'IA peut apporter une vraie valeur sur l'annonce elle-meme.

L'IA peut servir a :

- analyser l'image de l'annonce via `PostAttachment` ;
- utiliser l'image du `Product` en fallback si aucune image n'est jointe a l'annonce ;
- detecter automatiquement un type de materiau comme `plastique`, `metal`, `textile`, `verre` ;
- suggerer un prix intelligent base sur l'historique des annonces et les donnees disponibles ;
- generer automatiquement une description courte et exploitable ;
- proposer des tags et categories utiles au matching ;
- accelerer le remplissage de l'annonce pour l'utilisateur.

En pratique :

- l'utilisateur remplit moins d'informations manuellement ;
- le systeme enrichit l'annonce automatiquement ;
- les recherches et recommandations deviennent plus pertinentes.

### 13.4 Quelle image utiliser pour l'IA

La priorite recommandee est la suivante :

1. utiliser l'image uploadee dans `PostAttachment` ;
2. si aucune image n'est jointe a l'annonce, utiliser l'image du `Product` ;
3. si aucune image n'est disponible, l'IA image n'est pas executee et seules les suggestions textuelles restent possibles.

Pseudo-code logique :

```text
image_to_analyze = PostAttachment[0].file_url if PostAttachment else Product.image
material_type = AI.detect_material(image_to_analyze)
description = AI.generate_description(image_to_analyze)
suggested_price = AI.suggest_price(product_id)
tags = AI.generate_tags(image_to_analyze)
```

### 13.5 Quand declencher l'IA

L'IA peut etre declenchee dans les cas suivants :

- a la creation de l'annonce ;
- des qu'un `PostAttachment` est ajoute ;
- a la mise a jour de l'annonce ;
- si l'utilisateur remplace ou modifie l'image ;
- sur demande manuelle via un bouton du type `Generer automatiquement`.

### 13.6 Resume logique d'utilisation de l'IA

| Cas | Source image | Action IA |
| --- | --- | --- |
| utilisateur ajoute une image | `PostAttachment` | analyse image, tags, materiau, auto-description, suggestion prix |
| aucune image dans l'annonce | `Product.image` | fallback pour analyse, tags, materiau et description |
| aucune image disponible | aucune | pas d'analyse visuelle, seulement suggestions textuelles ou prix |

### 13.7 Matching intelligent

Le systeme peut calculer un score simple base sur :

- categorie du produit ;
- proximite geographique ;
- prix attractif ;
- historique des interactions ;
- type d'annonce ;
- etat d'avancement du groupe ;
- tags generes automatiquement ;
- materiau detecte si pertinent.

### 13.8 Exemple de logique de score

```text
Score = distance (30%) + categorie (40%) + historique (30%)
```

Ce score peut etre enrichi plus tard par :

- le prix ;
- les tags detectes ;
- la popularite ;
- l'etat du group buying ;
- la frequence d'interaction de l'utilisateur.

### 13.9 Suggestion de prix intelligente

La suggestion de prix ne doit pas dependre uniquement d'une API externe.

Dans une premiere version, elle peut etre basee sur :

- la moyenne des annonces similaires ;
- l'historique des prix du meme produit ;
- la categorie ;
- la localisation ;
- l'unite ;
- l'etat de l'offre et de la demande.

Le systeme peut alors retourner :

- un prix suggere ;
- une fourchette minimale et maximale ;
- un indicateur simple du type `prix bas`, `prix correct`, `prix eleve`.

### 13.10 Contraintes

- garder des algorithmes simples au debut ;
- eviter une IA trop lourde dans la premiere version ;
- garder le coeur metier principal dans Spring Boot ;
- implementer les traitements IA en Python ;
- toujours garder une possibilite de fonctionnement sans IA.

### 13.11 Choix technique recommande pour l'IA

Pour ce projet, les parties IA peuvent etre realisees en `Python`, ce qui est tres logique pour :

- l'analyse d'image ;
- la classification de materiaux ;
- la generation de tags ;
- la generation de description ;
- certaines suggestions intelligentes.

Le module principal reste en `Spring Boot`, tandis que la partie IA est developpee en `Python`.

Cela permet :

- de garder une architecture metier claire ;
- d'utiliser plus facilement des bibliotheques IA ;
- d'evoluer plus tard vers une architecture plus separee si necessaire ;
- de commencer simplement sans compliquer tout le projet.

Bibliotheques Python possibles selon le besoin :

- `OpenCV` ;
- `transformers` ;
- `scikit-learn` ;
- `TensorFlow` ou `PyTorch` si besoin avance.

### 13.12 Workflow complet : Frontend -> Spring Boot -> Python IA -> resultat

Ce workflow montre comment l'annonce peut etre creee puis enrichie automatiquement par l'IA.

#### Etape 1 : saisie utilisateur cote frontend

L'utilisateur :

- selectionne un `Product` ;
- choisit le type d'annonce ;
- saisit les champs principaux ;
- ajoute une ou plusieurs images dans `PostAttachment` si necessaire ;
- clique sur `Publier` ou `Generer automatiquement`.

Le frontend envoie alors la requete au backend Spring Boot.

#### Etape 2 : reception et validation dans Spring Boot

Spring Boot :

- verifie l'authentification ;
- verifie les champs obligatoires ;
- verifie le produit ;
- verifie le type d'annonce ;
- verifie la quantite, le prix, la localisation et les fichiers ;
- determine l'image a analyser.

Logique de choix de l'image :

1. utiliser la premiere image de `PostAttachment` si elle existe ;
2. sinon utiliser l'image du `Product` ;
3. sinon continuer sans analyse visuelle.

#### Etape 3 : appel de la partie IA en Python

Si une analyse IA est necessaire, Spring Boot envoie a Python les informations utiles :

- `product_id` ;
- `listing_title` ;
- `listing_description` ;
- `image_url` ou chemin image ;
- `category` ;
- eventuellement l'historique de prix disponible.

Exemple de donnees envoyees :

```json
{
  "productId": 12,
  "title": "Lot de bouteilles PET recyclees",
  "description": "Surplus disponible cette semaine",
  "imageUrl": "https://.../image1.jpg",
  "category": "plastique"
}
```

#### Etape 4 : traitements effectues en Python

Le module Python peut executer plusieurs traitements :

- detection de materiau ;
- generation de tags ;
- generation de description courte ;
- suggestion de prix ;
- proposition d'une categorie ou sous-categorie ;
- aide au matching.

Exemple de resultat retourne :

```json
{
  "material": "plastique",
  "tags": ["pet", "bouteille", "recyclable"],
  "generatedDescription": "Lot de bouteilles PET recyclees disponible en surplus, adapte aux entreprises de recyclage et de transformation.",
  "suggestedPrice": 2.8,
  "priceLevel": "prix correct"
}
```

#### Etape 5 : traitement du retour IA dans Spring Boot

Spring Boot recupere les resultats puis :

- complete la description si l'utilisateur a active l'auto-generation ;
- enregistre les tags ou suggestions ;
- stocke le materiau detecte si ce champ est prevu ;
- conserve le prix suggere comme recommandation ;
- poursuit la creation normale du `ResourceListing`.

Deux comportements sont possibles :

- soit les resultats IA sont appliques automatiquement ;
- soit ils sont proposes a l'utilisateur pour validation avant enregistrement final.

#### Etape 6 : enregistrement final de l'annonce

Spring Boot enregistre :

- le `ResourceListing` ;
- les `PostAttachment` ;
- le `GroupPurchase` si le type est `GROUP_BUYING` ;
- les metadonnees issues de l'IA si elles sont conservees.

#### Etape 7 : retour frontend

Le frontend affiche :

- l'annonce creee ;
- les suggestions generees ;
- les tags ;
- le materiau detecte ;
- le prix suggere ;
- les notifications en cas de publication reussie.

#### Resume simple du flux

```text
Frontend
  -> saisie annonce + upload image
Spring Boot
  -> validation metier + choix image
Python IA
  -> analyse image + description + tags + prix suggere
Spring Boot
  -> enregistrement final de l'annonce
Frontend
  -> affichage resultat enrichi
```

#### Avantages de ce flux

- separation claire entre logique metier et logique IA ;
- creation d'annonce plus rapide ;
- meilleure qualite de donnees ;
- meilleure recherche et meilleur matching ;
- architecture simple a expliquer en soutenance.

### 13.13 Ce qu'il faut faire exactement dans la partie IA

Pour garder un projet realiste, la partie IA doit rester ciblee sur l'enrichissement automatique des annonces.

Les 4 fonctions principales a realiser sont les suivantes.

#### 1. Detection du materiau depuis l'image

Objectif :

- analyser l'image de l'annonce ;
- identifier un materiau principal.

Exemples de resultats possibles :

- `plastique` ;
- `metal` ;
- `textile` ;
- `verre`.

Source image :

- `PostAttachment` en priorite ;
- `Product.image` en fallback.

Utilite :

- enrichir l'annonce ;
- faciliter les tags ;
- ameliorer le matching.

#### 2. Generation automatique de tags

Objectif :

- generer des mots-cles utiles a partir de l'image et du texte.

Exemples de tags :

- `PET` ;
- `recyclable` ;
- `bouteille` ;
- `lot industriel`.

Utilite :

- ameliorer la recherche ;
- faciliter la classification ;
- aider les recommandations.

#### 3. Generation automatique d'une courte description

Objectif :

- aider l'utilisateur a publier plus vite ;
- proposer une description propre et exploitable.

Exemple :

- entree : image + titre `Lot de bouteilles PET` ;
- sortie : `Lot de bouteilles PET recyclables disponible en surplus, adapte aux entreprises de recyclage.`

Utilite :

- gain de temps ;
- meilleur contenu d'annonce ;
- meilleure qualite globale des publications.

#### 4. Suggestion de prix

Objectif :

- suggerer un prix coherent a l'utilisateur.

Le calcul peut se baser sur :

- l'historique des annonces ;
- la categorie ;
- la localisation ;
- l'unite ;
- les prix d'annonces similaires.

Utilite :

- aider l'utilisateur a fixer un prix ;
- eviter les prix incoherents ;
- ameliorer la qualite du marche.

### 13.14 Ce qu'il ne faut pas mettre dans l'IA au debut

Pour eviter une complexite inutile, il ne faut pas mettre dans la partie IA au debut :

- toute la logique de `group buying` ;
- les commentaires ;
- les favoris ;
- les notifications ;
- la gestion complete des annonces ;
- les validations metier principales ;
- les statuts ;
- les permissions.

Toutes ces fonctions doivent rester dans `Spring Boot`.

### 13.15 Cas du matching group buying

Le matching du group buying peut etre intelligent sans etre obligatoirement de l'IA.

Dans une premiere version, il est recommande de faire un matching par score metier base sur :

- la categorie ;
- la distance ;
- le prix ;
- la quantite compatible ;
- l'avancement du groupe ;
- l'historique simple des interactions.

Cela permet d'avoir un matching intelligent, clair et facile a implementer.

Si tu as du temps plus tard, ce matching peut etre ameliore par une logique IA en Python.

### 13.16 Repartition recommandee entre Spring Boot et Python

#### Dans Spring Boot

- creation d'annonce ;
- mise a jour d'annonce ;
- group buying ;
- commentaires ;
- favoris ;
- notifications ;
- WebSocket ;
- filtres classiques ;
- controle metier ;
- securite.

#### Dans Python

- detection du materiau ;
- generation de tags ;
- auto-description ;
- suggestion de prix.

#### Option plus tard

- matching IA personnalise ;
- recommandations plus avancees ;
- prediction de participation.

### 13.17 Ordre recommande de realisation

1. realiser tout le module sans IA ;
2. ajouter la detection du materiau ;
3. ajouter les tags automatiques ;
4. ajouter l'auto-description ;
5. ajouter la suggestion de prix ;
6. ameliorer le matching si le temps le permet.

### 13.18 Formulation simple pour rapport ou soutenance

Tu peux presenter la partie IA ainsi :

`La partie IA du module sert a enrichir automatiquement les annonces grace a l'analyse d'image et aux suggestions intelligentes. Elle couvre principalement la detection du materiau, la generation de tags, l'auto-description et la suggestion de prix, tandis que la logique metier principale reste geree par Spring Boot.`

## 14. Regles de statut recommandees

### 14.1 Statut d'annonce

- `ACTIVE` : annonce visible et exploitable ;
- `CLOSED` : annonce cloturee manuellement ou automatiquement ;
- `EXPIRED` : date ou periode de validite depassee ;
- `CANCELLED` : annonce annulee.

### 14.2 Statut de groupe

- `OPEN` : groupe ouvert aux participations ;
- `FULL` : quantite cible atteinte ;
- `SUCCESS` : groupe valide et pret pour l'etape suivante ;
- `FAILED` : deadline depassee sans succes ;
- `CLOSED` : groupe ferme.

## 15. Cas limites a prendre en consideration

- deux entreprises essaient de rejoindre au meme moment ;
- la derniere quantite disponible est prise pendant la soumission ;
- le groupe atteint pile la quantite cible ;
- un participant tente de rejoindre apres expiration ;
- un vendeur tente de rejoindre son propre groupe ;
- un utilisateur tente de commenter une annonce fermee ;
- une piece jointe est invalide ;
- le produit lie a une annonce n'est plus disponible ;
- un groupe reste ouvert alors que l'annonce est fermee ;
- suppression d'une annonce avec commentaires, favoris et groupe associe.

## 16. Recommandations techniques Spring Boot

### 16.1 Structure proposee

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

### 16.2 Technologies

- Spring Boot ;
- Spring Web ;
- Spring Data JPA ;
- PostgreSQL ;
- WebSocket STOMP ;
- validation Bean Validation ;
- Python pour les traitements IA ;
- integration d'APIs externes si besoin pour certaines fonctions IA.

### 16.3 Services metier a prevoir

- `ListingService` ;
- `CommentService` ;
- `GroupPurchaseService` ;
- `FavoriteService` ;
- `MatchingService` ;
- `NotificationService`.

## 17. APIs finales recommandees

### 17.1 Listings

- `POST /api/listings`
- `GET /api/listings`
- `GET /api/listings/{id}`
- `GET /api/listings/search?category=&price=&location=`

### 17.2 Commentaires

- `POST /api/listings/{id}/comments`
- `GET /api/listings/{id}/comments`

### 17.3 Group Buying

- `POST /api/groups/{id}/join`
- `GET /api/groups/{id}`
- `DELETE /api/groups/{id}/leave`
- `GET /api/groups/{id}/participants`

### 17.4 Favoris

- `POST /api/listings/{id}/favorite`
- `DELETE /api/listings/{id}/favorite`

### 17.5 APIs externes gratuites ou avec free tier a consommer

Cette section concerne les APIs externes que tu peux integrer dans ton module pour ajouter des fonctionnalites intelligentes sans construire toute la logique toi-meme.

L'idee est de choisir des APIs simples, gratuites ou avec free tier.

Dans ton cas :

- le module principal est gere par Spring Boot ;
- les parties IA peuvent consommer ces APIs depuis Python ;
- puis renvoyer les resultats au module principal.

### 17.6 Choix final recommande : quoi utiliser concretement

Pour eviter de disperser le projet, voici le choix final le plus logique pour une version faisable, propre et intelligente.

#### A. Ce qu'il faut utiliser comme APIs externes

##### 1. Geolocalisation

API recommandee :

- `OpenRouteService` en premier choix ;
- `Nominatim` en alternative simple.

Pourquoi :

- convertir une ville ou une adresse en coordonnees ;
- alimenter la recherche par localisation ;
- calculer la proximite pour le matching ;
- afficher des annonces sur carte.

Donc :

- si tu veux un seul vrai choix principal, prends `OpenRouteService`.

##### 2. Carte

Solution recommandee :

- `Leaflet + OpenStreetMap`.

Pourquoi :

- simple ;
- gratuit ;
- parfait pour afficher les annonces proches.

##### 3. IA texte

Si tu veux utiliser une API externe pour generer du texte :

- `Gemini API` est un bon choix ;
- `OpenRouter` peut servir d'alternative.

Usage :

- generation de description ;
- suggestion de publication ;
- generation de tags textuels.

##### 4. IA image

Si tu veux utiliser une API externe pour t'aider sur l'image :

- `Hugging Face Inference API`.

Usage :

- classification image ;
- generation de labels ;
- aide a la detection de materiau.

##### 5. Moderation des commentaires

API recommandee :

- `Perspective API`.

Usage :

- analyser le texte d'un commentaire avant sa publication ;
- detecter un contenu toxique, insultant, agressif ou inapproprie ;
- aider la moderation automatique.

Pourquoi :

- tres adaptee aux commentaires ;
- simple a integrer ;
- utile pour proteger les espaces de discussion.

#### B. Ce qu'il faut faire en Python IA

Le plus logique est de faire en `Python` les traitements intelligents qui demandent un peu d'analyse.

##### 1. Detection de materiau

A faire en Python.

Exemple :

- image annonce ;
- resultat : `plastique`, `metal`, `textile`, `verre`.

##### 2. Generation automatique de description

A faire en Python.

Tu peux soit :

- appeler une API externe depuis Python ;
- soit traiter le texte localement si tu as une logique simple.

##### 3. Generation de tags

A faire en Python.

Exemple :

- `recyclable` ;
- `PET` ;
- `bouteille` ;
- `metal`;
- `lot industriel`.

##### 4. Suggestion de prix

A faire principalement en Python en combinant :

- historique de ta base ;
- prix des annonces similaires ;
- categorie ;
- localisation ;
- unite ;
- eventuellement une petite logique statistique.

Important :

- la suggestion de prix ne depend pas obligatoirement d'une API externe ;
- elle peut etre calculee a partir de tes propres donnees.

##### 5. Matching intelligent

Le matching peut etre partage entre :

- `Spring Boot` pour les filtres metier simples ;
- `Python` pour le score intelligent si tu veux aller plus loin.

Dans une V1, tu peux faire :

- filtrage simple dans Spring Boot ;
- calcul du score final dans Python si necessaire ;
- ou tout garder dans Spring Boot si tu veux aller plus vite.

#### C. Ce qu'il vaut mieux garder directement dans Spring Boot

Pour ne pas compliquer inutilement le projet, garde dans `Spring Boot` :

- creation d'annonce ;
- gestion des commentaires ;
- group buying ;
- favoris ;
- notifications WebSocket ;
- filtrage simple ;
- regles metier ;
- securite ;
- gestion des statuts ;
- recherche classique.

#### D. Decision finale simple a suivre

Si tu veux une version claire et realiste, prends cette combinaison :

- `Spring Boot` :
  logique metier complete du module.
- `Python` :
  material detection, tags, description, suggestion de prix.
- `OpenRouteService` :
  geolocalisation.
- `Leaflet + OpenStreetMap` :
  carte.
- `Hugging Face` :
  aide sur image si necessaire.
- `Gemini API` :
  aide pour description ou texte si necessaire.
- `Perspective API` :
  moderation automatique des commentaires.

#### E. Version minimale recommandee pour ton PFE ou prototype

Si tu veux eviter trop de complexite, la combinaison minimale ideale est :

- `Spring Boot` pour tout le module principal ;
- `Python` pour :
  - detection materiau ;
  - tags ;
  - description ;
  - suggestion de prix ;
- `OpenRouteService` pour la geolocalisation ;
- `Leaflet + OpenStreetMap` pour la carte ;
- `Perspective API` pour la moderation des commentaires.

Tu peux laisser en option :

- `Hugging Face` ;
- `Gemini API`.

#### F. Resume ultra clair

| Besoin | Outil recommande |
| --- | --- |
| gestion annonces, commentaires, group buying, favoris | Spring Boot |
| detection materiau | Python |
| tags automatiques | Python |
| auto-description | Python, avec ou sans API externe |
| suggestion de prix | Python + base de donnees locale |
| geolocalisation | OpenRouteService |
| carte interactive | Leaflet + OpenStreetMap |
| aide image optionnelle | Hugging Face |
| aide texte optionnelle | Gemini API |
| moderation des commentaires | Perspective API |

#### A. Geolocalisation et recherche par position

##### 1. OpenRouteService Geocoding

Usage dans le module :

- convertir une ville ou une adresse en `latitude` et `longitude` lors de la creation d'annonce ;
- faire du reverse geocoding pour afficher une localisation lisible ;
- aider le matching par proximite geographique.

Ce que tu peux faire :

- `forward geocoding` : texte vers coordonnees ;
- `reverse geocoding` : coordonnees vers adresse ;
- autocompletion de lieu.

Pourquoi c'est bien :

- simple a integrer ;
- documentation claire ;
- free tier pratique pour un projet.

Exemple d'usage :

- saisie de `Sfax, Tunisie` ;
- l'API retourne `latitude` et `longitude` ;
- tu stockes ces valeurs dans `ResourceListing`.

Lien :

- [https://openrouteservice.org/](https://openrouteservice.org/)

##### 2. Nominatim OpenStreetMap

Usage dans le module :

- geocodage simple pour convertir une adresse en coordonnees ;
- affichage lisible d'une ville ou region a partir des coordonnees.

Attention :

- tres utile pour un prototype ou PFE ;
- pour une vraie charge de production, il faut respecter les limites d'usage et la politique du service.

Lien :

- [https://nominatim.org/](https://nominatim.org/)

##### 3. Leaflet + OpenStreetMap

Ce n'est pas une API metier mais c'est la solution la plus simple pour la carte interactive.

Usage dans le module :

- afficher les annonces proches sur une carte ;
- montrer la position d'un listing ;
- filtrer visuellement par zone.

Lien :

- [https://leafletjs.com/](https://leafletjs.com/)
- [https://www.openstreetmap.org/](https://www.openstreetmap.org/)

#### B. IA simple pour image et tags automatiques

##### 1. Hugging Face Inference API

Usage dans le module :

- detection approximative du type de matiere a partir d'une image ;
- classification d'image ;
- generation de tags automatiques ;
- aide a l'auto-description.

Exemples de cas :

- image d'un lot de plastique ;
- l'API retourne des labels proches ;
- tu convertis ensuite ces labels en tags metier comme `plastique`, `metal`, `textile`.

Pourquoi c'est interessant :

- facile a tester ;
- free tier disponible ;
- plusieurs modeles accessibles sans construire un modele toi-meme.

Lien :

- [https://huggingface.co/docs/api-inference/index](https://huggingface.co/docs/api-inference/index)

Important :

- pour ton besoin, il vaut mieux utiliser l'API comme aide a la classification et garder une logique metier simple dans ton backend ;
- ne pas promettre une reconnaissance industrielle parfaite.

#### C. Generation de texte et assistance IA

##### 1. Gemini API free tier

Usage dans le module :

- generer une description automatique a partir d'un titre ou d'une image ;
- suggerer un texte de publication ;
- proposer des tags ;
- aider a reformuler une annonce.

Exemples de cas :

- l'utilisateur envoie un titre + une image ;
- l'API retourne une description propre et claire ;
- tu affiches cette suggestion avant validation.

Lien :

- [https://ai.google.dev/](https://ai.google.dev/)

##### 2. OpenRouter avec modeles gratuits

Usage dans le module :

- alternative simple pour generation de texte ;
- suggestion de description ;
- suggestion de resume ;
- suggestion de tags automatiques.

Avantage :

- plusieurs modeles gratuits ou tres peu couteux ;
- pratique pour prototype.

Lien :

- [https://openrouter.ai/](https://openrouter.ai/)

#### D. APIs a utiliser de preference en logique interne

Certaines fonctionnalites ne necessitent pas forcement une API externe.

Il vaut mieux les faire dans ton backend Spring Boot au debut :

- `matching intelligent` : score interne base sur categorie, distance, prix ;
- `suggestion de prix` : moyenne des annonces similaires dans ta propre base ;
- `trending listings` : calcul via vues, favoris, commentaires, participations ;
- `popularite` : score interne.

Cela rend ton application :

- plus simple ;
- moins dependante de services externes ;
- plus facile a presenter en soutenance ;
- plus facile a tester.

#### E. Mapping entre fonctionnalites du module et APIs externes

| Fonctionnalite | API recommandee | Type |
| --- | --- | --- |
| Convertir une adresse en coordonnees | OpenRouteService / Nominatim | gratuit ou free tier |
| Carte interactive | Leaflet + OpenStreetMap | gratuit |
| Detection image / tags automatiques | Hugging Face Inference API | free tier |
| Auto description d'annonce | Gemini API / OpenRouter | free tier |
| Suggestion de publication | Gemini API / OpenRouter | free tier |
| Matching intelligent | logique interne Spring Boot | interne |
| Suggestion de prix | logique interne basee sur la DB | interne |

#### F. Conseils de consommation dans Spring Boot

Implementation simple recommandee :

- utiliser `WebClient` ;
- centraliser les appels externes dans un package `integration/` ou `client/` ;
- isoler chaque API dans un service dedie ;
- utiliser des timeouts ;
- journaliser les erreurs ;
- prevoir un fallback si l'API externe ne repond pas.

Si les fonctions IA sont faites en `Python`, alors :

- les APIs d'image, de texte ou de classification peuvent etre appelees par le module Python ;
- Spring Boot recupere ensuite uniquement le resultat final ;
- cela permet de separer clairement la logique metier et la logique IA sans compliquer le coeur du module.

Exemple de structure :

```text
listing/
  integration/
    geocoding/
      OpenRouteServiceClient.java
    ai/
      HuggingFaceClient.java
      GeminiClient.java
```

Exemple simple avec `WebClient` :

```java
@Service
public class GeocodingClient {

    private final WebClient webClient;

    public GeocodingClient(WebClient.Builder builder) {
        this.webClient = builder.baseUrl("https://api.openrouteservice.org").build();
    }

    public String searchLocation(String text, String apiKey) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/geocode/search")
                        .queryParam("api_key", apiKey)
                        .queryParam("text", text)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
```

#### G. Ce qu'il faut vraiment utiliser dans une V1

Pour une premiere version simple et credible, tu peux prendre seulement :

- `OpenRouteService` ou `Nominatim` pour la geolocalisation ;
- `Leaflet + OpenStreetMap` pour la carte ;
- `Hugging Face` pour les tags image si tu veux une touche IA ;
- `Gemini` ou `OpenRouter` pour l'auto-description.

Et garder en interne :

- le matching intelligent ;
- la suggestion de prix ;
- le score de popularite ;
- les tendances.

## 18. Verifications backend critiques

Ces verifications doivent etre considerees comme obligatoires.

### 18.1 Lors de la creation d'annonce

- verifier l'utilisateur courant ;
- verifier le produit ;
- verifier le type ;
- verifier la quantite ;
- verifier le prix ;
- verifier la localisation ;
- verifier la coherence entre annonce et groupe ;
- verifier les pieces jointes.

### 18.2 Lors d'une participation a un groupe

- verrouiller ou securiser la mise a jour de quantite ;
- recalculer la quantite restante ;
- verifier le statut reel du groupe juste avant enregistrement ;
- verifier l'unicite du participant ;
- verifier que l'entreprise n'est pas le vendeur.

### 18.3 Lors d'un commentaire

- verifier l'annonce ;
- verifier le droit de commenter ;
- verifier le parent ;
- verifier le contenu.

## 19. Ce qu'il faut dire en soutenance

- architecture simplifiee et scalable ;
- remplacement du chat par un fil de commentaires intelligent ;
- collaboration entre entreprises grace au group buying ;
- temps reel cible via WebSocket ;
- matching intelligent base sur proximite, categorie et prix ;
- solution innovante mais realiste a implementer.

## 20. Conclusion

Cette version finale du module est :

- simple car elle repose sur 7 entites bien ciblees ;
- intelligente grace au matching et a l'assistance IA ;
- interactive grace aux commentaires et au temps reel ;
- innovante grace au group buying ;
- realiste a developper avec Spring Boot.

Le module est suffisamment riche pour etre valorise en projet ou en soutenance, tout en restant implementable sans complexite excessive.
