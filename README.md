# PhantomBan 📜

[![GitHub](https://img.shields.io/github/license/Jochyoua/PhantomBan?style=plastic)](https://github.com/Jochyoua/PhantomBan/blob/main/LICENSE) [![GitHub last commit](https://img.shields.io/github/last-commit/Jochyoua/PhantomBan?style=plastic)](https://github.com/Jochyoua/PhantomBan/commits/) [![Github Release](https://img.shields.io/github/v/release/Jochyoua/PhantomBan?style=plastic)](https://github.com/Jochyoua/PhantomBan/releases/latest)

PhantomBan introduces a unique "phantom ban" system to your Minecraft server, allowing players to join while being restricted in specific ways. This offers a new dimension to server moderation and encourages positive behavior.

## 🌟 Features

- **Phantom Ban**: Allow players to join the server while restricting their actions based on configurable events.
- **Loyalty Rewards**: Reduce ban duration by spending time on the server.
- **Dynamic Event Handling**: Configure specific events that phantom-banned players are restricted from.
- **Permission Management**: Dynamically manage permissions for bypassing specific event restrictions.
- **Blacklist Management**: Easily manage blacklisted players with commands.
- **Invisibility Effect**: Optionally apply invisibility to phantom-banned players.

## 🚀 Getting Started

1. **Install the Plugin**: Download the latest release and place the JAR file in your server's `plugins` directory.
2. **Configuration**: Customize the configuration file located in `plugins/PhantomBan/config.yml`.

## 📋 Commands

- `/phantomban help` - Display help message.
- `/phantomban add <player>` - Add a player to the blacklist.
- `/phantomban remove <player>` - Remove a player from the blacklist.
- `/phantomban reload` - Reload the plugin configuration.

## 🔑 Permissions

- `phantomban.notify` - Notify when a player joins while phantom banned (default: op).
- `phantomban.modify` - Add or remove players from the blacklist (default: op).
- `phantomban.bypass.*` - Bypass specific event restrictions while phantom banned. (Default: false)
### Bypass Permissions Example
- **Async Player Chat Event** (`org#bukkit#event#player#AsyncPlayerChatEvent`):
    - **Permission**: `phantomban.bypass.AsyncPlayerChatEvent`
    - **Description**: Allows a phantom-banned player to bypass chat restrictions.

## ⚙️ Configuration Example

Here's an example of the `config.yml` file:

```yaml
data:
  blacklist: []

settings:
  blacklist-enabled: true
  loyalty-rewards:
    enabled: true
    seconds-until-unban: 1800 # 30 MINUTES
    commands-to-run:
      - "unban %1$s"
      - "minecraft:tellraw %1$s [\"\\u00A7aYou have completed your sentence and may re-connect to rejoin society! Thank you for your service.\"]"
  effects:
    invisibility: true
    events:
      org#bukkit#event#player#PlayerJoinEvent:
        enabled: true
        message: "&cYou have joined the server while &bphantom-banned&c. Your ban will be reduced by playing."
      org#bukkit#event#player#AsyncPlayerChatEvent:
        enabled: true
        message: "&cYou cannot chat while &bphantom-banned&c. &6%1$s"
      org#bukkit#event#player#PlayerCommandPreprocessEvent:
        enabled: true
        message: "&cYou cannot use commands while &bphantom-banned&c. &6%1$s"
      org#bukkit#event#player#PlayerDropItemEvent:
        enabled: true
        message: "&cYou cannot drop items while &bphantom-banned&c. &6%1$s"
      org#bukkit#event#entity#EntityDamageByEntityEvent:
        enabled: true
        message: "&cYou cannot damage entities while &bphantom-banned&c. &6%1$s"
      org#bukkit#event#player#PlayerInteractEvent:
        enabled: true
        message: "&cYou cannot interact with items while &bphantom-banned&c. &6%1$s"

messages:
  help:
    - "&6/phantomban &ehelp &7- &aDisplay this message"
    - "&6/phantomban &eadd <player> &7- &aAdd a player to the blacklist"
    - "&6/phantomban &eremove <player> &7- &aRemove a player from the blacklist"
    - "&6/phantomban &ereload &7- &aReload the config and event listeners"
  notification: "&ePlayer &c%1$s &e has joined while &bphantom-banned&e."
  reload-success: "&eSuccessfully reloaded config. Please check to see if event listeners successfully fire."
  reload-failure: "&cFailed to successfully reload config. Plugin has been &c&ldisabled &c- Check console and restart."
  remove-success: "&eSuccessfully removed data from blacklist."
  remove-failure: "&cFailed to remove data from blacklist. &eMaybe it has already been removed?"
  add-success: "&eSuccessfully added data to blacklist."
  add-failure: "&cFailed to add data to blacklist. &eMaybe it already exists?"
  time-until-unban: "You have %1$s seconds left until unbanned."
```
## 💬 Get in Touch

For assistance, questions, or suggestions:

- Open an [issue on GitHub](https://github.com/Jochyoua/PhantomBan/issues/new/choose).
- Reach out to the author: [Jochyoua](https://www.spigotmc.org/conversations/add?to=Jochyoua).

## 🐛 Found a Bug? Want a New Feature?

If you encounter any bugs or have ideas for new features, we'd love to hear from you!

- **Bug Reports**: Please open an [issue on GitHub](https://github.com/Jochyoua/PhantomBan/issues/new/choose) to report any bugs or glitches you come across.
- **Feature Suggestions**: Share your ideas for new features or enhancements by opening an issue. We value your input!

## 🎉 Contribute

You're invited to contribute! If you have enhancements or new features in mind, create a pull request and join the fun.

## 📄 License

This project is licensed under the [GPL-3.0 license](LICENSE).

---

Thank you for using PhantomBan! We hope this plugin enhances your server's moderation experience.
