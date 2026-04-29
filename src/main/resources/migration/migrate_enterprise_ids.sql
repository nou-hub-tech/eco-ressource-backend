-- ═══════════════════════════════════════════════════════════════════
--  🔄 SCRIPT DE MIGRATION — Enterprise ID pour transactions et escrows
--  EcoRessource B2B — À exécuter UNE SEULE FOIS après déploiement
-- ═══════════════════════════════════════════════════════════════════
--
--  Problème : Les transactions/escrows créés avant l'ajout du champ
--             enterprise_id ont cette valeur à NULL → invisibles.
--
--  Solution : Associer via le projet ou l'invoice liée.
--
-- ───────────────────────────────────────────────────────────────────
--  VÉRIFICATION AVANT MIGRATION
-- ───────────────────────────────────────────────────────────────────

-- Voir les transactions sans enterprise_id
SELECT COUNT(*) AS tx_sans_entreprise FROM TRANSACTION WHERE enterprise_id IS NULL;

-- Voir les escrows sans enterprise_id
SELECT COUNT(*) AS esc_sans_entreprise FROM ESCROW WHERE enterprise_id IS NULL;

-- Voir la liste des entreprises existantes
SELECT id, company_name FROM ENTERPRISE;

-- ───────────────────────────────────────────────────────────────────
--  STRATÉGIE 1 : Associer via le nom de projet (transaction → invoice)
--  (Marche si le champ "project" est identique entre transaction et invoice)
-- ───────────────────────────────────────────────────────────────────

UPDATE TRANSACTION t
SET t.enterprise_id = (
    SELECT e.id
    FROM ENTERPRISE e
    WHERE LOWER(e.company_name) = (
        SELECT LOWER(i.seller_name)
        FROM INVOICE i
        WHERE LOWER(i.project) = LOWER(t.project)
        LIMIT 1
    )
    LIMIT 1
)
WHERE t.enterprise_id IS NULL
  AND EXISTS (
    SELECT 1 FROM INVOICE i WHERE LOWER(i.project) = LOWER(t.project)
  );

-- ───────────────────────────────────────────────────────────────────
--  STRATÉGIE 2 : Associer les escrows via linked_invoice_id
-- ───────────────────────────────────────────────────────────────────

UPDATE ESCROW esc
SET esc.enterprise_id = (
    SELECT e.id
    FROM ENTERPRISE e
    INNER JOIN INVOICE i ON LOWER(e.company_name) = LOWER(i.seller_name)
    WHERE i.id = esc.linked_invoice_id
    LIMIT 1
)
WHERE esc.enterprise_id IS NULL
  AND esc.linked_invoice_id IS NOT NULL;

-- ───────────────────────────────────────────────────────────────────
--  STRATÉGIE 3 (MANUEL) : Assigner TOUT à une seule entreprise
--  ⚠️  À utiliser SEULEMENT si vous n'avez qu'une entreprise principale
--      Remplacez XXX par l'ID de votre entreprise (voir SELECT ci-dessus)
-- ───────────────────────────────────────────────────────────────────

-- UPDATE TRANSACTION SET enterprise_id = XXX WHERE enterprise_id IS NULL;
-- UPDATE ESCROW      SET enterprise_id = XXX WHERE enterprise_id IS NULL;

-- ───────────────────────────────────────────────────────────────────
--  VÉRIFICATION APRÈS MIGRATION
-- ───────────────────────────────────────────────────────────────────

SELECT COUNT(*) AS tx_migrees    FROM TRANSACTION WHERE enterprise_id IS NOT NULL;
SELECT COUNT(*) AS tx_restantes  FROM TRANSACTION WHERE enterprise_id IS NULL;
SELECT COUNT(*) AS esc_migrees   FROM ESCROW      WHERE enterprise_id IS NOT NULL;
SELECT COUNT(*) AS esc_restantes FROM ESCROW      WHERE enterprise_id IS NULL;

-- ───────────────────────────────────────────────────────────────────
--  BONUS : Mettre à jour le type des factures existantes (invoice_type)
--  en se basant sur le nom de l'entreprise
-- ───────────────────────────────────────────────────────────────────

-- Marquer VENTE si sellerName = companyName d'une entreprise
UPDATE INVOICE i
SET i.invoice_type = 'VENTE'
WHERE i.invoice_type IS NULL
  AND EXISTS (
    SELECT 1 FROM ENTERPRISE e WHERE LOWER(e.company_name) = LOWER(i.seller_name)
  );

-- Marquer ACHAT si clientName = companyName d'une entreprise
UPDATE INVOICE i
SET i.invoice_type = 'ACHAT'
WHERE i.invoice_type IS NULL
  AND EXISTS (
    SELECT 1 FROM ENTERPRISE e WHERE LOWER(e.company_name) = LOWER(i.client_name)
  );

SELECT invoice_type, COUNT(*) AS nb FROM INVOICE GROUP BY invoice_type;
