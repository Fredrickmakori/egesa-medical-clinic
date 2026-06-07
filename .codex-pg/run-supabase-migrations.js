const fs = require("fs");
const path = require("path");
const { Client } = require("pg");

const root = path.resolve(__dirname, "..");
const files = [
  "infra/supabase/schema.sql",
  "infra/supabase/migrations/001_add_rbac_and_sync.sql",
  "infra/supabase/migrations/002_staff_and_rbac.sql",
  "infra/supabase/migrations/202605010001_billing_and_payments.sql",
  "infra/supabase/migrations/202605180001_domain_code_tables_and_constraints.sql",
  "infra/supabase/migrations/202605180001_core_clinical_tables.sql",
  "infra/supabase/migrations/202605180001_reporting_pipeline.sql",
  "infra/supabase/migrations/202606060001_eagle_tech_control_plane.sql",
  "infra/supabase/migrations/202606060002_eagle_tech_hospital_tenant_template.sql",
];

async function main() {
  const password = process.env.SUPABASE_DB_PASSWORD;
  if (!password) throw new Error("SUPABASE_DB_PASSWORD is required");

  const client = new Client({
    host: "db.vigeqwzqasblsnetbprm.supabase.co",
    port: 5432,
    database: "postgres",
    user: "postgres",
    password,
    ssl: { rejectUnauthorized: false },
    statement_timeout: 120000,
  });

  await client.connect();
  const version = await client.query("select current_database() as db, current_user as user, version()");
  console.log(`Connected to ${version.rows[0].db} as ${version.rows[0].user}`);

  const results = [];
  for (const rel of files) {
    const abs = path.join(root, rel);
    const sql = fs.readFileSync(abs, "utf8");
    process.stdout.write(`Applying ${rel} ... `);
    try {
      await client.query("begin");
      await client.query(sql);
      await client.query("commit");
      console.log("ok");
      results.push({ file: rel, status: "ok" });
    } catch (error) {
      await client.query("rollback").catch(() => {});
      console.log(`failed: ${error.message}`);
      results.push({ file: rel, status: "failed", error: error.message });
    }
  }

  const tables = await client.query(`
    select table_name
      from information_schema.tables
     where table_schema = 'public'
       and table_type = 'BASE TABLE'
     order by table_name
  `);

  console.log("\nMigration results:");
  for (const result of results) {
    console.log(`- ${result.status.toUpperCase()} ${result.file}${result.error ? ` :: ${result.error}` : ""}`);
  }
  console.log("\nPublic tables:");
  console.log(tables.rows.map((row) => row.table_name).join(", "));

  await client.end();
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
