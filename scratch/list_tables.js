const { Client } = require('pg');

const config = {
  host: 'aws-0-eu-west-1.pooler.supabase.com',
  port: 5432,
  user: 'postgres.vigeqwzqasblsnetbprm',
  password: 'Fred1234#@8202',
  database: 'postgres',
  ssl: { rejectUnauthorized: false }
};

async function run() {
  const client = new Client(config);
  try {
    await client.connect();
    console.log('Connected to eu-west-1 successfully!');

    // List all tables in public schema
    console.log('--- Public Tables ---');
    const resTables = await client.query(`
      SELECT table_name 
      FROM information_schema.tables 
      WHERE table_schema = 'public'
    `);
    console.log(resTables.rows.map(r => r.table_name));

    // Check if supabase_migrations.schema_migrations exists
    console.log('--- Supabase Migrations Table ---');
    try {
      const resMig = await client.query('SELECT * FROM supabase_migrations.schema_migrations');
      console.log('Migrations in supabase_migrations.schema_migrations:', resMig.rows);
    } catch (err) {
      console.log('No supabase_migrations.schema_migrations table found or access denied:', err.message);
    }

  } catch (err) {
    console.error(err);
  } finally {
    await client.end();
  }
}

run();
