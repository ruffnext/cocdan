A brand new online CoC (Call of Cthulhu) RPG backend, building with Rust + SeaORM.

This is just a project for learning......


# Getting started

1. Install `sea-orm-cli` via `cargo install sea-orm-cli`
2. Setting up environment variable: `echo "DATABASE_URL=\"sqlite://sqlite.db?mode=rwc\"\nRUST_LOG=coc_dan_backend=DEBUG" > .env`
3. Run migration via `just migrate`
4. Compile and run via `cargo run`

# Test REST API

1. Install `vscode`
2. Install plugin `rest client`
3. Open `*.http` files and try!

# Unit tests

1. run `cargo test`

> Unit tests use in-memory dummy sqlite database.

# Development

* Regenerate entities files using `just migrate`
* Regenerate Typescript binding files using `just ts`
