use axum::{
    extract::{Path, State},
    Json,
};

use crate::{
    daemon::{entities::Stage, DbEntity},
    left_span,
    typedef::err::{ErrCode, Left},
    AppState,
};

pub async fn get_stage(
    State(state): State<AppState>,
    Path(stage_id): Path<String>,
) -> Result<Json<Stage>, Left> {
    let stage =
        if let Some(stage) = Stage::db_load_by_id(stage_id.clone(), &state.db.manager).await? {
            stage
        } else {
            return Err(left_span!(
                ErrCode::NoContent,
                format!("stage not found: {}", stage_id)
            ));
        };

    Ok(Json(stage))
}
