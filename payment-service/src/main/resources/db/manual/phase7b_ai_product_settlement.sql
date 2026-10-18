-- Neuro Bridge Phase 7B reviewed Payment schema change.
-- The current service uses Hibernate ddl-auto=update. Apply this reviewed,
-- idempotent PostgreSQL DDL through the deployment database-change process
-- before enabling the Phase 7B RPCs; do not depend on an unreviewed automatic
-- production schema mutation for financial settlement state.

CREATE TABLE IF NOT EXISTS ai_product_settlement (
    id uuid PRIMARY KEY,
    product_purchase_id uuid NOT NULL,
    product_id uuid NOT NULL,
    client_code varchar(100) NOT NULL,
    expert_code varchar(100) NOT NULL,
    gross_amount_vnd numeric(19,2) NOT NULL,
    platform_fee_vnd numeric(19,2) NOT NULL,
    expert_net_vnd numeric(19,2) NOT NULL,
    state varchar(50) NOT NULL,
    main_payment_transaction_id uuid NULL,
    fee_payment_transaction_id uuid NULL,
    snapshot_product_name text NOT NULL,
    snapshot_seller_expert_id uuid NOT NULL,
    snapshot_seller_expert_code text NOT NULL,
    snapshot_seller_display_name text NOT NULL,
    shortfall_amount_vnd numeric(19,2) NULL,
    outcome_code varchar(100) NULL,
    settled_at_utc timestamp with time zone NULL,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_product_settlement_purchase UNIQUE (product_purchase_id),
    CONSTRAINT ck_ai_product_settlement_amounts CHECK (
        gross_amount_vnd > 0
        AND gross_amount_vnd = trunc(gross_amount_vnd)
        AND platform_fee_vnd >= 0
        AND platform_fee_vnd = trunc(platform_fee_vnd)
        AND expert_net_vnd >= 0
        AND expert_net_vnd = trunc(expert_net_vnd)
        AND platform_fee_vnd = round(gross_amount_vnd * 0.10, 0)
        AND expert_net_vnd = gross_amount_vnd - platform_fee_vnd
    ),
    CONSTRAINT ck_ai_product_settlement_state CHECK (
        state IN ('IN_PROGRESS', 'SUCCESS', 'INSUFFICIENT_BALANCE', 'WALLET_NOT_ACTIVE', 'PAYMENT_FAILED')
    ),
    CONSTRAINT ck_ai_product_settlement_success_shape CHECK (
        state <> 'SUCCESS'
        OR (
            main_payment_transaction_id IS NOT NULL
            AND fee_payment_transaction_id IS NOT NULL
            AND settled_at_utc IS NOT NULL
        )
    )
);
