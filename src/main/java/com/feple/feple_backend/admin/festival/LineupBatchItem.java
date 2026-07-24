package com.feple.feple_backend.admin.festival;

import com.feple.feple_backend.artistfestival.entity.LineupUpdate;

record LineupBatchItem(Long afId, LineupUpdate update) {}
