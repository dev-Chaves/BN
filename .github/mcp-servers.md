# MCP Servers Configuration

To enable MCP (Model Context Protocol) servers for this project, configure them in your GitHub Copilot settings.

## PostgreSQL MCP Server

This server allows Copilot to connect to and query the PostgreSQL database directly, which is useful for:
- Understanding current schema structure
- Validating migration files
- Testing database queries before implementing them

### Setup

1. **Install the PostgreSQL MCP server** (via your Copilot CLI or IDE plugin)
2. **Configure connection** in your Copilot settings:
   ```json
   {
     "mcpServers": {
       "postgres": {
         "host": "localhost",
         "port": 5432,
         "database": "benefix",
         "user": "postgres",
         "password": "password"
       }
     }
   }
   ```

3. **Verify connection** by asking Copilot to describe the current database schema

### Benefits

- Copilot can validate SQL migrations before you run them
- Inspect existing tables and relationships while writing ORM code
- Debug data issues by querying the database directly
- Ensure new entities match the actual database schema

### When to Use

Ask Copilot to:
- "What tables exist in the database?" 
- "Show me the schema for the benefits table"
- "Validate that my migration creates the right columns"
