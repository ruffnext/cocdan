mod db;

pub use db::entities;
pub use db::DbEntity;
pub use db::DbService;
pub use db::SurrealRecord;

#[cfg(test)]
pub use db::new_mock_db;
