package studio.thevipershow.safechat.chat.listeners;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;
import org.tomlj.TomlArray;
import studio.thevipershow.safechat.SafeChat;
import studio.thevipershow.safechat.chat.ChatUtil;
import studio.thevipershow.safechat.chat.check.ChatCheck;
import studio.thevipershow.safechat.chat.check.CheckType;
import studio.thevipershow.safechat.chat.check.ChecksContainer;
import studio.thevipershow.safechat.persistence.SafeChatHibernate;
import studio.thevipershow.safechat.persistence.mappers.PlayerDataManager;

public final class ChatListener implements Listener {

    public ChatListener(SafeChatHibernate safeChatHibernate, ChecksContainer checksContainer) {
        this.safeChatHibernate = safeChatHibernate;
        this.playerDataManager = Objects.requireNonNull(safeChatHibernate.getPlayerDataManager(),
                "SafeChat's Hibernate PlayerDataManager wasn't configured yet!");
        this.checksContainer = checksContainer;
    }

    private final SafeChatHibernate safeChatHibernate;
    private final PlayerDataManager playerDataManager;
    private final ChecksContainer checksContainer;

    private static void sendWarning(ChatCheck check, AsyncPlayerChatEvent event) {
        if (!check.hasWarningEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        TomlArray messages = check.getWarningMessages();

        for (int k = 0; k < messages.size(); k++) {
            String message = check.replacePlaceholders(Objects.requireNonNull(messages.getString(k)), event);
            player.sendMessage(ChatUtil.color(message));
        }
    }

    private void updateData(@NotNull CheckType checkType, @NotNull Player player) {
        playerDataManager.addOrUpdatePlayerData(player, checkType);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        for (ChatCheck chatCheck : checksContainer.getActiveChecks()) {
            if (chatCheck.check(event)) {
                event.setCancelled(true);
                sendWarning(chatCheck, event);
                updateData(chatCheck.getCheckType(), event.getPlayer());
                break;
            }
        }
    }

    public SafeChatHibernate getSafeChatHibernate() {
        return safeChatHibernate;
    }

    public ChecksContainer getChecksContainer() {
        return checksContainer;
    }
}
