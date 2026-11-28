package com.aistra.hail.ui.actions

/**
 * Pure dismiss-decision policy for the Actions bottom sheet.
 *
 * Returns `true` when dismissing the sheet must clear the editor/draft state.
 * The edit flow clears on dismiss; the create flow keeps the draft (selected apps
 * plus app-picker state) so an accidental outside-tap/back does not lose
 * in-progress work.
 *
 * `pickerOpen` is accepted for clarity and future branching (for example if the
 * picker ever needs to be closed independently of the editor), but it does not
 * change the result under the current create/edit rule: create never clears and
 * edit always clears, regardless of whether the app picker is open.
 */
@Suppress("UnusedParameter")
internal fun shouldClearOnDismiss(
    isCreate: Boolean,
    pickerOpen: Boolean,
): Boolean = !isCreate

/**
 * Whether a create-flow dismiss with the app picker open must commit the in-progress
 * draft selection into the committed editor fields (so an accidental outside-tap does
 * not lose the selection). Only the create flow with an open picker commits; edit
 * flow never commits on dismiss (it clears).
 */
internal fun shouldCommitDraftOnDismiss(
    isCreate: Boolean,
    pickerOpen: Boolean,
): Boolean = isCreate && pickerOpen

/**
 * Seeds the draft selection for an app picker from the existing in-progress draft
 * when it is non-empty, otherwise from the committed field value. Prefers the draft
 * so an accidental dismiss does not clobber in-progress work.
 */
internal fun seedDraftForPicker(
    existingDraft: Set<String>,
    committed: List<String>,
): Set<String> = if (existingDraft.isNotEmpty()) existingDraft else committed.toSet()
