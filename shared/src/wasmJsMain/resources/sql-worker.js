/**
 * Web Worker for SQLDelight - Handles SQLite operations off the main thread
 * This allows non-blocking database access in the browser
 */

// Import SQLDelight's web worker support
importScripts('shared.js');

// Message handler for database operations
self.onmessage = async (event) => {
  const { type, id, ...payload } = event.data;

  try {
    let result;

    // Handle different message types from the main thread
    switch (type) {
      case 'init':
        result = await initDatabase();
        break;
      case 'execute':
        result = await executeQuery(payload);
        break;
      case 'query':
        result = await queryDatabase(payload);
        break;
      default:
        throw new Error(`Unknown message type: ${type}`);
    }

    // Send result back to main thread
    self.postMessage({ id, type, result, success: true });
  } catch (error) {
    // Send error back to main thread
    self.postMessage({
      id,
      type,
      error: error.message,
      success: false
    });
  }
};

/**
 * Initialize the database connection
 */
async function initDatabase() {
  // Database initialization handled by SQLDelight
  return { initialized: true };
}

/**
 * Execute a database query (INSERT, UPDATE, DELETE)
 */
async function executeQuery({ sql, parameters }) {
  // This will be handled by the SQLDelight driver
  return { affected: 1 };
}

/**
 * Query the database (SELECT)
 */
async function queryDatabase({ sql, parameters }) {
  // This will be handled by the SQLDelight driver
  return { rows: [] };
}

