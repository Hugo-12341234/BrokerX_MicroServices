# Glossaire métier BrokerX

| Terme              | Définition                                                                                         |
|--------------------|----------------------------------------------------------------------------------------------------|
| Utilisateur        | Personne inscrite sur la plateforme BrokerX, pouvant effectuer des opérations et gérer son compte. |
| Portefeuille       | Espace virtuel associé à un utilisateur, regroupant son solde en monnaie fiduciaire et ses actifs. |
| Solde              | Montant total disponible dans le portefeuille d’un utilisateur pour effectuer des opérations.      |
| Transaction        | Mouvement de fonds tel qu’un dépôt ou un retrait, ou opération d’achat/vente d’un actif.           |
| Ordre              | Demande formelle d’un utilisateur pour acheter ou vendre un stock. Un ordre contient le symbole, la quantité, le type (marché/limite), le prix (si limite), la durée et le sens (achat/vente). |
| Type d’ordre       | Catégorie d’ordre : Marché (exécuté au prix courant du marché, sans garantie de prix) ou Limite (exécuté uniquement si le prix cible est atteint). |
| Statut d’ordre     | État d’avancement d’un ordre : Pending (en attente), Active (en cours), Rejected (refusé), Completed (exécuté), Expired (périmé). |
| Durée d’ordre      | Période de validité d’un ordre : DAY (valide jusqu’à la fin de la journée), IOC (Immediate or Cancel : exécuté immédiatement ou annulé), FOK (Fill or Kill : exécuté en totalité ou annulé). |
| Bande de prix      | Plage de variation autorisée du prix d’un stock sur une période donnée, pour limiter la volatilité et protéger les investisseurs. |
| Tick Size          | Incrément minimal de variation du prix d’un stock : le prix d’un ordre doit être un multiple du tick size défini pour ce stock. |
| Stock              | Actif financier (ex : action) disponible à l’achat ou à la vente sur BrokerX. Chaque stock possède un symbole, un nom, un prix, une bande de prix et un tick size. |
| ClientOrderId      | Identifiant unique fourni par le client pour tracer et retrouver un ordre dans le système.         |
| KYC                | Processus de vérification d’identité réglementaire (Know Your Customer).                           |
| MFA                | Authentification multi-facteurs pour renforcer la sécurité d’accès à la plateforme.                |
| OTP                | Mot de passe à usage unique, utilisé pour la vérification d’identité ou l’authentification.        |
| Statut utilisateur | État du compte utilisateur : Pending (en attente), Active (actif), Rejected (refusé), Suspended (suspendu). |
| Rejet d’ordre      | Motif pour lequel un ordre est refusé (ex : fonds insuffisants, violation de bande de prix, tick size non respecté, quantité invalide). |
| SimulatedPayment   | Processus simulé de règlement d’un dépôt ou d’un retrait, utilisé pour tester la plateforme.       |
| Audit              | Journalisation des opérations importantes (création de compte, ordres, transactions, etc.).        |
| Session            | Période d’activité authentifiée d’un utilisateur sur la plateforme.                                |
| Rôle utilisateur   | Catégorie d’accès d’un utilisateur (ex : Client, Administrateur).                                 |
| Idempotency Key    | Clé unique permettant d’éviter la duplication d’une opération lors de réessais.                    |
| Statut transaction | État d’une transaction : Pending (en attente), Settled (réglée), Failed (échouée), Completed (terminée). |
| VérificationToken  | Jeton utilisé pour confirmer l’identité ou l’inscription d’un utilisateur.                        |
| Side               | Sens d’un ordre : Achat (Buy) ou Vente (Sell).                                                    |
| Description        | Texte explicatif associé à une transaction ou un ordre.                                           |
| Timestamp          | Date et heure d’enregistrement d’une opération ou d’un ordre.                                     |
| PreTradeValidation | Contrôle métier effectué avant l’acceptation d’un ordre (pouvoir d’achat, règles de prix, tick size, bande de prix, quantité, etc.). |

---
Ce glossaire recense les principaux termes métier utilisés dans BrokerX, en lien direct avec le code et les processus métier de la plateforme.
