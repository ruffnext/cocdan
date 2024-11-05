use axum::{
    extract::{Path, State},
    Json,
};

use crate::{
    daemon::{
        entities::{Session, Stage},
        DbEntity,
    },
    left_span, mls,
    typedef::err::{ErrCode, Left},
    AppState,
};

pub async fn get_stage(
    State(state): State<AppState>,
    Path(stage_id): Path<String>,
    _session: Session,
) -> Result<Json<Stage>, Left> {
    let stage_id = stage_id
        .parse::<i64>()
        .map_err(mls!(ErrCode::InvalidParameter("bad stage id".into())))?;

    let stage = if let Some(stage) = Stage::db_load_by_id(stage_id, &state.db.manager).await? {
        stage
    } else {
        return Err(left_span!(
            ErrCode::NoContent,
            format!("stage not found: {}", stage_id)
        ));
    };

    Ok(Json(stage))
}
