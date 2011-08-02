package org.tigase.mobile.db;

import android.provider.BaseColumns;

public class ChatTableMetaData implements BaseColumns {

	public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.mobilemessenger.chatitem";

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.mobilemessenger.chat";

	public static final String TABLE_NAME = "chat_history";

	public static final String FIELD_ID = "_id";

	public static final String FIELD_JID = "jid";

	public static final String FIELD_TIMESTAMP = "timestamp";

	public static final String FIELD_THREAD_ID = "thread_id";

	public static final String FIELD_STATE = "state";

	public static final String FIELD_BODY = "body";

}
