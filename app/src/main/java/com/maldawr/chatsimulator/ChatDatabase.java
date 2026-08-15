package com.maldawr.chatsimulator;

import android.content.Context;

import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Database;
import androidx.room.Entity;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.PrimaryKey;
import androidx.room.Query;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.List;

@Database(entities = {ChatDatabase.BotRow.class, ChatDatabase.MessageRow.class, ChatDatabase.CallRow.class}, version = 2, exportSchema = false)
public abstract class ChatDatabase extends RoomDatabase {
    private static volatile ChatDatabase INSTANCE;
    public abstract DaoApi dao();

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE bots ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE bots ADD COLUMN groupChat INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE bots ADD COLUMN groupSubtitle TEXT NOT NULL DEFAULT ''");
            database.execSQL("ALTER TABLE bots ADD COLUMN lastSender TEXT NOT NULL DEFAULT ''");
        }
    };

    public static ChatDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (ChatDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), ChatDatabase.class, "chat_simulator_v5.db")
                            .allowMainThreadQueries()
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    @Entity(tableName = "bots")
    public static class BotRow {
        @PrimaryKey public long id;
        public String name;
        public String phone;
        public String status;
        public int unread;
        public String lastMessage;
        public long lastTime;
        public boolean autoReply;
        public String avatarUri;
        public int activeFrom;
        public int activeTo;
        public String replyMode;
        public boolean initiative;
        public int maxBurst;
        @ColumnInfo(defaultValue = "0") public boolean favorite;
        @ColumnInfo(defaultValue = "0") public boolean groupChat;
        @ColumnInfo(defaultValue = "''") public String groupSubtitle = "";
        @ColumnInfo(defaultValue = "''") public String lastSender = "";
    }

    @Entity(tableName = "messages")
    public static class MessageRow {
        @PrimaryKey public long id;
        public long botId;
        public String text;
        public boolean incoming;
        public long time;
    }

    @Entity(tableName = "calls")
    public static class CallRow {
        @PrimaryKey public long id;
        public long botId;
        public String type;
        public long time;
        public int durationSec;
    }

    @Dao
    public interface DaoApi {
        @Query("SELECT COUNT(*) FROM bots") int botCount();
        @Query("SELECT * FROM bots ORDER BY lastTime DESC") List<BotRow> botsOrdered();
        @Query("SELECT * FROM bots WHERE unread > 0 ORDER BY lastTime DESC") List<BotRow> unreadBotsOrdered();
        @Query("SELECT * FROM bots WHERE favorite = 1 ORDER BY lastTime DESC") List<BotRow> favoriteBotsOrdered();
        @Query("SELECT * FROM bots WHERE groupChat = 1 ORDER BY lastTime DESC") List<BotRow> groupBotsOrdered();
        @Query("SELECT COUNT(*) FROM bots WHERE favorite = 1") int favoriteCount();
        @Query("SELECT COUNT(*) FROM bots WHERE groupChat = 1") int groupCount();
        @Query("SELECT * FROM bots WHERE id = :id LIMIT 1") BotRow bot(long id);
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putBot(BotRow row);
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putBots(List<BotRow> rows);
        @Query("UPDATE bots SET unread = 0 WHERE id = :botId") void markRead(long botId);
        @Query("UPDATE bots SET favorite = :favorite WHERE id = :botId") void setFavorite(long botId, boolean favorite);
        @Query("SELECT COALESCE(SUM(unread),0) FROM bots") int totalUnread();
        @Query("DELETE FROM bots") void clearBots();

        @Query("SELECT * FROM messages WHERE botId = :botId ORDER BY time ASC, id ASC") List<MessageRow> messages(long botId);
        @Query("SELECT COUNT(*) FROM messages WHERE botId = :botId") int messageCount(long botId);
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putMessage(MessageRow row);
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putMessages(List<MessageRow> rows);
        @Query("DELETE FROM messages") void clearMessages();

        @Query("SELECT * FROM calls ORDER BY time DESC") List<CallRow> calls();
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putCall(CallRow row);
        @Insert(onConflict = OnConflictStrategy.REPLACE) void putCalls(List<CallRow> rows);
        @Query("DELETE FROM calls") void clearCalls();
    }
}
