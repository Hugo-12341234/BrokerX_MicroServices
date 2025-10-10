-- Migration pour mettre à jour la colonne price des stocks existants
UPDATE stock_rule SET price = 150.00 WHERE symbol = 'AAPL';
UPDATE stock_rule SET price = 260.00 WHERE symbol = 'MSFT';
UPDATE stock_rule SET price = 320.00 WHERE symbol = 'TSLA';
UPDATE stock_rule SET price = 1500.00 WHERE symbol = 'GOOG';

