using System;
using AiAvailableService.Domain.Enums;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace AiAvailableService.Infrastructure.Persistence.Migrations
{
    /// <inheritdoc />
    public partial class InitialAiAvailable : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AlterDatabase()
                .Annotation("Npgsql:Enum:public.preview_type", "IMAGE,VIDEO")
                .Annotation("Npgsql:Enum:public.product_asset_type", "SOURCE_CODE,VIDEO,DATASET,DOCUMENT")
                .Annotation("Npgsql:Enum:public.product_status", "DRAFT,PUBLISHED,UNPUBLISHED")
                .Annotation("Npgsql:Enum:public.purchase_status", "PENDING_PAYMENT,SUCCESS");

            migrationBuilder.CreateTable(
                name: "ai_product",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    seller_expert_id = table.Column<Guid>(type: "uuid", nullable: false),
                    seller_expert_code = table.Column<string>(type: "text", nullable: false),
                    name = table.Column<string>(type: "text", nullable: false),
                    description = table.Column<string>(type: "text", nullable: true),
                    category_code = table.Column<string>(type: "text", nullable: false),
                    technologies = table.Column<string[]>(type: "text[]", nullable: false, defaultValueSql: "ARRAY[]::text[]"),
                    ai_capabilities = table.Column<string[]>(type: "text[]", nullable: false, defaultValueSql: "ARRAY[]::text[]"),
                    price_vnd = table.Column<decimal>(type: "numeric(19,2)", precision: 19, scale: 2, nullable: false),
                    status = table.Column<ProductStatus>(type: "product_status", nullable: false, defaultValueSql: "'DRAFT'::product_status"),
                    first_published_at = table.Column<DateTimeOffset>(type: "timestamptz", nullable: true),
                    last_published_at = table.Column<DateTimeOffset>(type: "timestamptz", nullable: true),
                    preview_type = table.Column<PreviewType>(type: "preview_type", nullable: true),
                    preview_storage_bucket = table.Column<string>(type: "text", nullable: true),
                    preview_storage_object_key = table.Column<string>(type: "text", nullable: true),
                    preview_original_filename = table.Column<string>(type: "text", nullable: true),
                    preview_content_type = table.Column<string>(type: "text", nullable: true),
                    preview_byte_size = table.Column<long>(type: "bigint", nullable: true),
                    preview_video_duration_seconds = table.Column<decimal>(type: "numeric(8,3)", precision: 8, scale: 3, nullable: true),
                    preview_uploaded_at = table.Column<DateTimeOffset>(type: "timestamptz", nullable: true),
                    created_at = table.Column<DateTimeOffset>(type: "timestamptz", nullable: false, defaultValueSql: "now()"),
                    updated_at = table.Column<DateTimeOffset>(type: "timestamptz", nullable: false, defaultValueSql: "now()")
                },
                constraints: table =>
                {
                    table.PrimaryKey("pk_ai_product", x => x.id);
                    table.CheckConstraint("ck_ai_product_category_code_nonblank", "btrim(category_code) <> ''");
                    table.CheckConstraint("ck_ai_product_description_nonblank_if_present", "description IS NULL OR btrim(description) <> ''");
                    table.CheckConstraint("ck_ai_product_name_nonblank", "btrim(name) <> ''");
                    table.CheckConstraint("ck_ai_product_preview_shape", "(\n    (preview_type IS NULL\n        AND preview_storage_bucket IS NULL\n        AND preview_storage_object_key IS NULL\n        AND preview_original_filename IS NULL\n        AND preview_content_type IS NULL\n        AND preview_byte_size IS NULL\n        AND preview_video_duration_seconds IS NULL\n        AND preview_uploaded_at IS NULL)\n    OR\n    (preview_type IS NOT NULL\n        AND preview_storage_bucket IS NOT NULL\n        AND preview_storage_object_key IS NOT NULL\n        AND preview_original_filename IS NOT NULL\n        AND preview_content_type IS NOT NULL\n        AND preview_byte_size IS NOT NULL\n        AND preview_uploaded_at IS NOT NULL\n        AND preview_byte_size >= 0\n        AND (\n            (preview_type = 'IMAGE' AND preview_video_duration_seconds IS NULL)\n            OR\n            (preview_type = 'VIDEO'\n                AND preview_video_duration_seconds > 0\n                AND preview_video_duration_seconds <= 240.000)\n        ))\n)");
                    table.CheckConstraint("ck_ai_product_price_vnd_positive_whole", "price_vnd > 0 AND price_vnd = trunc(price_vnd)");
                    table.CheckConstraint("ck_ai_product_publication_timestamp_shape", "(\n    (status = 'DRAFT' AND first_published_at IS NULL AND last_published_at IS NULL)\n    OR\n    (status IN ('PUBLISHED', 'UNPUBLISHED')\n        AND first_published_at IS NOT NULL\n        AND last_published_at IS NOT NULL\n        AND last_published_at >= first_published_at)\n)");
                });

            migrationBuilder.CreateTable(
                name: "product_asset",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    product_id = table.Column<Guid>(type: "uuid", nullable: false),
                    asset_type = table.Column<ProductAssetType>(type: "product_asset_type", nullable: false),
                    storage_bucket = table.Column<string>(type: "text", nullable: false),
                    storage_object_key = table.Column<string>(type: "text", nullable: false),
                    original_filename = table.Column<string>(type: "text", nullable: false),
                    content_type = table.Column<string>(type: "text", nullable: false),
                    byte_size = table.Column<long>(type: "bigint", nullable: false),
                    uploaded_at = table.Column<DateTimeOffset>(type: "timestamptz", nullable: false, defaultValueSql: "now()")
                },
                constraints: table =>
                {
                    table.PrimaryKey("pk_product_asset", x => x.id);
                    table.CheckConstraint("ck_product_asset_byte_size_nonnegative", "byte_size >= 0");
                    table.ForeignKey(
                        name: "fk_product_asset_product_id",
                        column: x => x.product_id,
                        principalTable: "ai_product",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateTable(
                name: "product_purchase",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    product_id = table.Column<Guid>(type: "uuid", nullable: false),
                    buyer_client_id = table.Column<Guid>(type: "uuid", nullable: false),
                    buyer_client_code = table.Column<string>(type: "text", nullable: false),
                    status = table.Column<PurchaseStatus>(type: "purchase_status", nullable: false, defaultValueSql: "'PENDING_PAYMENT'::purchase_status"),
                    payment_transaction_id = table.Column<Guid>(type: "uuid", nullable: true),
                    product_name_at_purchase = table.Column<string>(type: "text", nullable: true),
                    seller_expert_id_at_purchase = table.Column<Guid>(type: "uuid", nullable: true),
                    seller_expert_code_at_purchase = table.Column<string>(type: "text", nullable: true),
                    seller_display_name_at_purchase = table.Column<string>(type: "text", nullable: true),
                    price_vnd_at_purchase = table.Column<decimal>(type: "numeric(19,2)", precision: 19, scale: 2, nullable: true),
                    platform_fee_vnd_at_purchase = table.Column<decimal>(type: "numeric(19,2)", precision: 19, scale: 2, nullable: true),
                    expert_net_vnd_at_purchase = table.Column<decimal>(type: "numeric(19,2)", precision: 19, scale: 2, nullable: true),
                    purchased_at = table.Column<DateTimeOffset>(type: "timestamptz", nullable: true),
                    access_expires_at = table.Column<DateTimeOffset>(type: "timestamptz", nullable: true),
                    created_at = table.Column<DateTimeOffset>(type: "timestamptz", nullable: false, defaultValueSql: "now()"),
                    updated_at = table.Column<DateTimeOffset>(type: "timestamptz", nullable: false, defaultValueSql: "now()")
                },
                constraints: table =>
                {
                    table.PrimaryKey("pk_product_purchase", x => x.id);
                    table.CheckConstraint("ck_product_purchase_status_snapshot_shape", "(\n    (status = 'PENDING_PAYMENT'\n        AND payment_transaction_id IS NULL\n        AND product_name_at_purchase IS NULL\n        AND seller_expert_id_at_purchase IS NULL\n        AND seller_expert_code_at_purchase IS NULL\n        AND seller_display_name_at_purchase IS NULL\n        AND price_vnd_at_purchase IS NULL\n        AND platform_fee_vnd_at_purchase IS NULL\n        AND expert_net_vnd_at_purchase IS NULL\n        AND purchased_at IS NULL\n        AND access_expires_at IS NULL)\n    OR\n    (status = 'SUCCESS'\n        AND payment_transaction_id IS NOT NULL\n        AND product_name_at_purchase IS NOT NULL\n        AND seller_expert_id_at_purchase IS NOT NULL\n        AND seller_expert_code_at_purchase IS NOT NULL\n        AND seller_display_name_at_purchase IS NOT NULL\n        AND price_vnd_at_purchase IS NOT NULL\n        AND platform_fee_vnd_at_purchase IS NOT NULL\n        AND expert_net_vnd_at_purchase IS NOT NULL\n        AND purchased_at IS NOT NULL\n        AND access_expires_at IS NOT NULL)\n)");
                    table.CheckConstraint("ck_product_purchase_success_monetary_entitlement", "status <> 'SUCCESS'\nOR\n(\n    price_vnd_at_purchase = trunc(price_vnd_at_purchase)\n    AND platform_fee_vnd_at_purchase = trunc(platform_fee_vnd_at_purchase)\n    AND expert_net_vnd_at_purchase = trunc(expert_net_vnd_at_purchase)\n    AND platform_fee_vnd_at_purchase = round(price_vnd_at_purchase * 0.10, 0)\n    AND expert_net_vnd_at_purchase = price_vnd_at_purchase - platform_fee_vnd_at_purchase\n    AND access_expires_at = purchased_at + interval '30 days'\n)");
                    table.ForeignKey(
                        name: "fk_product_purchase_product_id",
                        column: x => x.product_id,
                        principalTable: "ai_product",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Restrict);
                });

            migrationBuilder.CreateTable(
                name: "product_rating",
                columns: table => new
                {
                    id = table.Column<Guid>(type: "uuid", nullable: false),
                    purchase_id = table.Column<Guid>(type: "uuid", nullable: false),
                    score = table.Column<short>(type: "smallint", nullable: false),
                    review_text = table.Column<string>(type: "text", nullable: true),
                    created_at = table.Column<DateTimeOffset>(type: "timestamptz", nullable: false, defaultValueSql: "now()"),
                    updated_at = table.Column<DateTimeOffset>(type: "timestamptz", nullable: false, defaultValueSql: "now()")
                },
                constraints: table =>
                {
                    table.PrimaryKey("pk_product_rating", x => x.id);
                    table.CheckConstraint("ck_product_rating_review_text_nonblank_if_present", "review_text IS NULL OR btrim(review_text) <> ''");
                    table.CheckConstraint("ck_product_rating_score_range", "score BETWEEN 1 AND 5");
                    table.ForeignKey(
                        name: "fk_product_rating_purchase_id",
                        column: x => x.purchase_id,
                        principalTable: "product_purchase",
                        principalColumn: "id",
                        onDelete: ReferentialAction.Restrict);
                });

            migrationBuilder.CreateIndex(
                name: "ix_ai_product_published_category_price",
                table: "ai_product",
                columns: new[] { "category_code", "price_vnd", "id" },
                filter: "\"status\" = 'PUBLISHED'");

            migrationBuilder.CreateIndex(
                name: "ix_ai_product_published_price",
                table: "ai_product",
                columns: new[] { "price_vnd", "id" },
                filter: "\"status\" = 'PUBLISHED'");

            migrationBuilder.CreateIndex(
                name: "ix_ai_product_published_recent",
                table: "ai_product",
                columns: new[] { "last_published_at", "id" },
                descending: new[] { true, false },
                filter: "\"status\" = 'PUBLISHED'");

            migrationBuilder.CreateIndex(
                name: "ix_ai_product_seller_updated",
                table: "ai_product",
                columns: new[] { "seller_expert_id", "updated_at", "id" },
                descending: new[] { false, true, false });

            migrationBuilder.CreateIndex(
                name: "uq_product_asset_product_type",
                table: "product_asset",
                columns: new[] { "product_id", "asset_type" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "uq_product_asset_storage_object",
                table: "product_asset",
                columns: new[] { "storage_bucket", "storage_object_key" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "ix_product_purchase_buyer_history",
                table: "product_purchase",
                columns: new[] { "buyer_client_id", "purchased_at", "id" },
                descending: new[] { false, true, false },
                filter: "\"status\" = 'SUCCESS'");

            migrationBuilder.CreateIndex(
                name: "uq_product_purchase_payment_transaction",
                table: "product_purchase",
                column: "payment_transaction_id",
                unique: true);

            migrationBuilder.CreateIndex(
                name: "uq_product_purchase_product_buyer",
                table: "product_purchase",
                columns: new[] { "product_id", "buyer_client_id" },
                unique: true);

            migrationBuilder.CreateIndex(
                name: "uq_product_rating_purchase",
                table: "product_rating",
                column: "purchase_id",
                unique: true);

            migrationBuilder.Sql(
                """
                CREATE FUNCTION public.fn_reject_product_asset_mutation_after_first_publish()
                RETURNS trigger
                LANGUAGE plpgsql
                AS $$
                DECLARE
                    old_product_id uuid;
                    new_product_id uuid;
                    candidate_product_id uuid;
                    parent_first_published_at timestamptz;
                BEGIN
                    IF TG_OP = 'INSERT' THEN
                        new_product_id := NEW.product_id;
                    ELSIF TG_OP = 'UPDATE' THEN
                        old_product_id := OLD.product_id;
                        new_product_id := NEW.product_id;
                    ELSE
                        old_product_id := OLD.product_id;
                    END IF;

                    FOR candidate_product_id IN
                        SELECT DISTINCT candidate.product_id
                        FROM unnest(ARRAY[old_product_id, new_product_id]) AS candidate(product_id)
                        WHERE candidate.product_id IS NOT NULL
                        ORDER BY candidate.product_id
                    LOOP
                        SELECT first_published_at
                        INTO parent_first_published_at
                        FROM public.ai_product
                        WHERE id = candidate_product_id
                        FOR UPDATE;

                        IF parent_first_published_at IS NOT NULL THEN
                            RAISE EXCEPTION USING
                                ERRCODE = '55000',
                                MESSAGE = 'Product assets are immutable after first publication';
                        END IF;
                    END LOOP;

                    RETURN CASE WHEN TG_OP = 'DELETE' THEN OLD ELSE NEW END;
                END;
                $$;
                """);

            migrationBuilder.Sql(
                """
                CREATE TRIGGER trg_product_asset_lock_after_first_publish
                BEFORE INSERT OR UPDATE OR DELETE ON public.product_asset
                FOR EACH ROW
                EXECUTE FUNCTION public.fn_reject_product_asset_mutation_after_first_publish();
                """);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.Sql(
                "DROP TRIGGER IF EXISTS trg_product_asset_lock_after_first_publish ON public.product_asset;");
            migrationBuilder.Sql(
                "DROP FUNCTION IF EXISTS public.fn_reject_product_asset_mutation_after_first_publish();");

            migrationBuilder.DropTable(
                name: "product_rating");

            migrationBuilder.DropTable(
                name: "product_purchase");

            migrationBuilder.DropTable(
                name: "product_asset");

            migrationBuilder.DropTable(
                name: "ai_product");

            migrationBuilder.Sql("DROP TYPE IF EXISTS public.preview_type;");
            migrationBuilder.Sql("DROP TYPE IF EXISTS public.product_asset_type;");
            migrationBuilder.Sql("DROP TYPE IF EXISTS public.product_status;");
            migrationBuilder.Sql("DROP TYPE IF EXISTS public.purchase_status;");
        }
    }
}
