package ru.alex.shop;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShopManager {
    private final File file;
    private YamlConfiguration yml;
    private final Map<String, Shop> shops = new HashMap<>();

    public ShopManager(File dataFolder) {
        this.file = new File(dataFolder, "shops.yml");
    }

    public void load() {
        if (!file.exists()) {
            try { file.getParentFile().mkdirs(); file.createNewFile(); } catch (IOException ignored) {}
        }
        yml = YamlConfiguration.loadConfiguration(file);
        shops.clear();

        ConfigurationSection root = yml.getConfigurationSection("shops");
        if (root == null) return;

        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) continue;

            Location chestLoc = locFrom(s.getConfigurationSection("chest"));
            Location signLoc = locFrom(s.getConfigurationSection("sign"));
            UUID owner = UUID.fromString(s.getString("owner",""));
            int price = s.getInt("price");
            int amount = s.getInt("amount");
            ItemStack item = s.getItemStack("item");
            if (chestLoc == null || item == null) continue;

            Shop shop = new Shop(chestLoc, owner, price, amount, item);
            shop.setSignLoc(signLoc);
            shops.put(key, shop);
        }
    }

    public void save() {
        yml.set("shops", null);
        for (Map.Entry<String, Shop> e : shops.entrySet()) {
            String key = e.getKey();
            Shop shop = e.getValue();
            String path = "shops." + key;

            yml.set(path + ".owner", shop.getOwner().toString());
            yml.set(path + ".price", shop.getPrice());
            yml.set(path + ".amount", shop.getAmount());
            yml.set(path + ".item", shop.getItemSample());

            setLoc(path + ".chest", shop.getChestLoc());
            if (shop.getSignLoc() != null) setLoc(path + ".sign", shop.getSignLoc());
        }
        try { yml.save(file); } catch (IOException ignored) {}
    }

    private void setLoc(String path, Location loc) {
        yml.set(path + ".world", loc.getWorld().getName());
        yml.set(path + ".x", loc.getBlockX());
        yml.set(path + ".y", loc.getBlockY());
        yml.set(path + ".z", loc.getBlockZ());
    }

    private Location locFrom(ConfigurationSection s) {
        if (s == null) return null;
        World w = Bukkit.getWorld(s.getString("world"));
        if (w == null) return null;
        return new Location(w, s.getInt("x"), s.getInt("y"), s.getInt("z"));
    }

    public static String key(Location loc) {
        return loc.getWorld().getName()+":"+loc.getBlockX()+":"+loc.getBlockY()+":"+loc.getBlockZ();
    }

    public Shop getByChest(Location chestLoc) {
        return shops.get(key(chestLoc));
    }

    public Shop getBySign(Location signLoc) {
        for (Shop s : shops.values()) {
            if (s.getSignLoc() == null) continue;
            Location l = s.getSignLoc();
            if (l.getWorld().equals(signLoc.getWorld())
                    && l.getBlockX()==signLoc.getBlockX()
                    && l.getBlockY()==signLoc.getBlockY()
                    && l.getBlockZ()==signLoc.getBlockZ()) return s;
        }
        return null;
    }

    public void put(Shop shop) {
        shops.put(key(shop.getChestLoc()), shop);
    }

    public void removeByChest(Location chestLoc) {
        shops.remove(key(chestLoc));
    }
}
