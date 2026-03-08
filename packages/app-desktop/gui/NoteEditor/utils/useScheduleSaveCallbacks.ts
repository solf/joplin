import Logger from '@joplin/utils/Logger';
import { RefObject, useCallback } from 'react';
import { FormNote, NoteBodyEditorRef } from './types';
import { formNoteToNote } from '.';
import ExternalEditWatcher from '@joplin/lib/services/ExternalEditWatcher';
import Note from '@joplin/lib/models/Note';
import type { Dispatch } from 'redux';
import eventManager, { EventName } from '@joplin/lib/eventManager';
import type { OnSetFormNote } from './useFormNote';

const logger = Logger.create('useScheduleSaveCallbacks');

interface Props {
	setFormNote: RefObject<OnSetFormNote>;
	editorId: string;
	dispatch: Dispatch;
	editorRef: RefObject<NoteBodyEditorRef>;
}

const useScheduleSaveCallbacks = (props: Props) => {
	const scheduleSaveNote = useCallback((formNote: FormNote) => {
		if (!formNote.saveActionQueue) throw new Error('saveActionQueue is not set!!'); // Sanity check

		// reg.logger().debug('Scheduling...', formNote);

		const makeAction = (formNote: FormNote) => {
			return async function() {
				const note = await formNoteToNote(formNote);

				// SOLF: Skip no-op saves to prevent spurious user_updated_time changes.
				// TinyMCE can fire change events without actual content modifications
				// (key events during navigation, HTML normalization, image layout, etc.)
				// which would otherwise overwrite user_updated_time via autoTimestamp.
				const existingNote = await Note.load(note.id);
				if (existingNote
					&& existingNote.title === note.title
					&& existingNote.body === note.body
					&& existingNote.parent_id === note.parent_id
					&& existingNote.deleted_time === note.deleted_time
					&& existingNote.is_conflict === note.is_conflict) {
					logger.debug('Skipping no-op save for note', note.id);
					props.setFormNote.current((prev: FormNote) => {
						return { ...prev, hasChanged: false };
					});
					props.dispatch({
						type: 'EDITOR_NOTE_STATUS_REMOVE',
						id: formNote.id,
					});
					return;
				}

				logger.debug('Saving note...', note);
				const savedNote = await Note.save(note, { changeId: `editorChange-${props.editorId}` });

				props.setFormNote.current((prev: FormNote) => {
					return { ...prev, user_updated_time: savedNote.user_updated_time, hasChanged: false };
				});

				void ExternalEditWatcher.instance().updateNoteFile(savedNote);

				props.dispatch({
					type: 'EDITOR_NOTE_STATUS_REMOVE',
					id: formNote.id,
				});

				eventManager.emit(EventName.NoteContentChange, { note: savedNote });
			};
		};

		formNote.saveActionQueue.push(makeAction(formNote));
		return formNote.saveActionQueue.waitForAllDone();
	}, [props.dispatch, props.editorId, props.setFormNote]);

	const saveNoteIfWillChange = useCallback(async (formNote: FormNote) => {
		if (!formNote.id || !formNote.bodyWillChangeId || !props.editorRef.current) return;

		const body = await props.editorRef.current.content();

		void scheduleSaveNote({
			...formNote,
			body: body,
			bodyWillChangeId: 0,
			bodyChangeId: 0,
		});
	}, [scheduleSaveNote, props.editorRef]);

	return { saveNoteIfWillChange, scheduleSaveNote };
};

export default useScheduleSaveCallbacks;
