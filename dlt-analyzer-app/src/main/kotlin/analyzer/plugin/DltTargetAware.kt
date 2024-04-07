package analyzer.plugin

import db.DltTarget

interface DltTargetAware {
    fun setDltTarget(dltTarget: DltTarget?)
}
