# 🔄 Refresh des Ordres Seed - Guide pour le Correcteur

## Problème
Les ordres seed dans le matching-service expirent après 24h. Si vous redémarrez le projet après plus d'une journée, vous n'aurez plus d'ordres disponibles pour tester le matching.

## Solution Simple ✅

Si vos ordres seed ont expiré, copiez simplement la migration V7 en nouvelle version :

### Étapes :
1. Allez dans `matching-service/src/main/resources/db/migration/`
2. Copiez le fichier `V7__refresh_seed_orders.sql`
3. Renommez-le en `V8__refresh_seed_orders_again.sql` (ou V9, V10, etc.)
4. Redémarrez le matching-service : `docker-compose restart matching-service`

### Exemple :
```bash
# Dans le dossier matching-service/src/main/resources/db/migration/
cp V7__refresh_seed_orders.sql V8__refresh_seed_orders_again.sql
```

Flyway exécutera automatiquement la nouvelle migration et refreshera les timestamps des ordres seed.

## Vérification 🧪

Après le redémarrage, vous devriez voir dans les logs :
```
Successfully applied 1 migration to schema "public"
Migration applied: V8__refresh_seed_orders_again.sql
```

## Ordres Seed Disponibles 📋

Après refresh, vous aurez ces ordres disponibles pour testing :
- **AAPL** : 10 actions à 100$ (VENTE)
- **MSFT** : 10 actions à 200$ (VENTE)  
- **TSLA** : 15 actions à 150$ (VENTE)
- **GOOG** : 17 actions à 1200$ (VENTE)

Vous pouvez créer des ordres d'ACHAT pour matcher avec ces ordres seed.
